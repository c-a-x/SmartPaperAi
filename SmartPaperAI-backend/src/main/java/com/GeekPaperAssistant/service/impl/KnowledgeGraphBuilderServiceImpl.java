package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.model.graph.ConceptNode;
import com.GeekPaperAssistant.model.graph.DocumentNode;
import com.GeekPaperAssistant.model.vo.KnowledgeGraphBuildResultVO;
import com.GeekPaperAssistant.repository.graph.ConceptRepository;
import com.GeekPaperAssistant.repository.graph.DocumentNodeRepository;
import com.GeekPaperAssistant.service.KnowledgeGraphBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 知识图谱构建服务实现
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphBuilderServiceImpl implements KnowledgeGraphBuilderService {

    private final ChatClient ragChatClient;
    private final ConceptRepository conceptRepository;
    private final DocumentNodeRepository documentNodeRepository;
    private final Neo4jClient neo4jClient;

    @Override
    @Transactional
    public KnowledgeGraphBuildResultVO buildGraphForDocument(
            Long documentId,
            String title,
            String content,
            Long userId,
            Long kbId) {

        log.info("开始为文档构建知识图谱: documentId={}, title={}", documentId, title);

        try {
            // 1. 提取文档中的概念
            List<ConceptInfo> concepts = extractConceptsFromDocument(title, content);
            log.info("从文档中提取了 {} 个概念", concepts.size());

            if (concepts.isEmpty()) {
                return KnowledgeGraphBuildResultVO.builder()
                        .documentId(documentId)
                        .conceptCount(0)
                        .newConceptCount(0)
                        .relationshipCount(0)
                        .success(true)
                        .build();
            }

            // 2. 创建或更新概念节点
            int newConceptCount = 0;
            List<String> conceptNames = new ArrayList<>();

            for (ConceptInfo conceptInfo : concepts) {
                Optional<ConceptNode> existing = conceptRepository.findByName(conceptInfo.name);

                if (existing.isEmpty()) {
                    // 创建新概念
                    ConceptNode concept = new ConceptNode(
                            conceptInfo.name,
                            conceptInfo.type,
                            conceptInfo.field
                    );
                    concept.setDescription(conceptInfo.description);
                    concept.setImportance(conceptInfo.importance);
                    concept.setFrequency(1);

                    conceptRepository.save(concept);
                    newConceptCount++;
                    log.debug("创建新概念: {}", conceptInfo.name);
                } else {
                    // 更新现有概念的频次
                    ConceptNode concept = existing.get();
                    concept.setFrequency(concept.getFrequency() + 1);
                    conceptRepository.save(concept);
                    log.debug("更新概念频次: {}", conceptInfo.name);
                }

                conceptNames.add(conceptInfo.name);
            }

            // 3. 创建或更新文档节点
            Optional<DocumentNode> existingDoc = documentNodeRepository.findByDocumentId(documentId);
            if (existingDoc.isEmpty()) {
                DocumentNode docNode = new DocumentNode(documentId, title, "other", userId);
                docNode.setKbId(kbId);
                docNode.setCreateTime(LocalDateTime.now());
                documentNodeRepository.save(docNode);
                log.debug("创建文档节点: documentId={}", documentId);
            }

            // 4. 创建文档-概念关系（CONTAINS）
            for (ConceptInfo conceptInfo : concepts) {
                createDocumentConceptRelation(
                        documentId,
                        conceptInfo.name,
                        conceptInfo.importance,
                        conceptInfo.frequency,
                        conceptInfo.confidence
                );
            }

            // 5. 创建概念间关系（RELATED_TO）
            int relationshipCount = createConceptRelationships(conceptNames);

            // 6. 创建概念层次关系（IS_A）
            createConceptHierarchy(conceptNames);

            // 7. 提取并创建文档作者关系（AUTHORED_BY）
            createAuthorRelationships(documentId, title, content);

            // 8. 计算并创建文档相似度关系（SIMILAR_TO）
            createDocumentSimilarityRelationships(documentId, userId);

            log.info("知识图谱构建完成: documentId={}, 概念数={}, 新概念={}, 关系数={}",
                    documentId, concepts.size(), newConceptCount, relationshipCount);

            return KnowledgeGraphBuildResultVO.builder()
                    .documentId(documentId)
                    .conceptCount(concepts.size())
                    .newConceptCount(newConceptCount)
                    .relationshipCount(relationshipCount)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("知识图谱构建失败: documentId={}", documentId, e);
            return KnowledgeGraphBuildResultVO.builder()
                    .documentId(documentId)
                    .conceptCount(0)
                    .newConceptCount(0)
                    .relationshipCount(0)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public void deleteGraphForDocument(Long documentId) {
        log.info("删除文档的知识图谱数据: documentId={}", documentId);

        try {
            // 删除文档节点（级联删除关系）
            documentNodeRepository.findByDocumentId(documentId)
                    .ifPresent(documentNodeRepository::delete);

            log.info("文档知识图谱数据删除完成: documentId={}", documentId);

        } catch (Exception e) {
            log.error("删除文档知识图谱数据失败: documentId={}", documentId, e);
        }
    }

    /**
     * 从文档中提取概念
     */
    private List<ConceptInfo> extractConceptsFromDocument(String title, String content) {
        try {
            // 限制内容长度以避免 token 超限
            String limitedContent = content.length() > 4000
                    ? content.substring(0, 4000) + "..."
                    : content;

            String prompt = """
                    请从以下文档中提取关键概念实体。每个概念需要包含：
                    - name: 概念名称
                    - type: 概念类型（PERSON/ORGANIZATION/TECHNOLOGY/THEORY/METHOD/FIELD）
                    - field: 所属学科领域
                    - description: 概念描述（简短）
                    - importance: 重要性（0.0-1.0）
                    - frequency: 在文档中出现的次数
                    - confidence: 提取置信度（0.0-1.0）

                    文档标题: {title}
                    文档内容: {content}

                    请直接返回JSON数组格式，不要添加额外的解释：
                    [
                      {
                        "name": "概念名称",
                        "type": "TECHNOLOGY",
                        "field": "计算机科学",
                        "description": "简短描述",
                        "importance": 0.9,
                        "frequency": 5,
                        "confidence": 0.95
                      }
                    ]
                    """
                    .replace("{title}", title)
                    .replace("{content}", limitedContent);

            List<ConceptInfo> concepts = ragChatClient.prompt()
                    .system("你是一个专业的学术概念提取专家。请严格按照JSON数组格式返回结果，不要添加任何额外的包装对象或解释文字。")
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ConceptInfo>>() {});

            return concepts != null ? concepts : new ArrayList<>();

        } catch (Exception e) {
            log.warn("概念提取失败，使用降级方案: {}", e.getMessage());
            // 降级方案：从标题中提取基本概念
            return List.of(new ConceptInfo(
                    title,
                    "FIELD",
                    "未分类",
                    "从文档标题提取",
                    0.7,
                    1,
                    0.5
            ));
        }
    }

    /**
     * 创建文档-概念关系
     */
    private void createDocumentConceptRelation(
            Long documentId,
            String conceptName,
            Double importance,
            Integer frequency,
            Double confidence) {

        try {
            String cypher = """
                    MATCH (doc:Document {documentId: $documentId})
                    MATCH (concept:Concept {name: $conceptName})
                    MERGE (doc)-[r:CONTAINS]->(concept)
                    SET r.importance = $importance,
                        r.frequency = $frequency,
                        r.confidence = $confidence
                    RETURN r
                    """;

            neo4jClient.query(cypher)
                    .bind(documentId).to("documentId")
                    .bind(conceptName).to("conceptName")
                    .bind(importance).to("importance")
                    .bind(frequency).to("frequency")
                    .bind(confidence).to("confidence")
                    .run();

        } catch (Exception e) {
            log.warn("创建文档-概念关系失败: documentId={}, concept={}", documentId, conceptName, e);
        }
    }

    /**
     * 创建概念间关系
     *
     * <p>使用 LLM 分析概念列表，识别它们之间的语义关系</p>
     */
    private int createConceptRelationships(List<String> conceptNames) {
        if (conceptNames.size() < 2) {
            return 0;
        }

        try {
            String prompt = """
                    请分析以下概念列表，识别它们之间的语义关系。
                    对于每对相关的概念，返回关系类型（SIMILAR/OPPOSITE/PART_OF/USES/DEPENDS_ON）和关系强度（0.0-1.0）。

                    概念列表: {concepts}

                    请直接返回JSON数组格式：
                    [
                      {
                        "concept1": "概念A",
                        "concept2": "概念B",
                        "relationType": "SIMILAR",
                        "strength": 0.8
                      }
                    ]

                    只返回确实相关的概念对，不要生成无关的关系。
                    """
                    .replace("{concepts}", String.join(", ", conceptNames));

            List<ConceptRelationInfo> relations = ragChatClient.prompt()
                    .system("你是一个专业的概念关系分析专家。请严格按照JSON数组格式返回结果。")
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<ConceptRelationInfo>>() {});

            if (relations == null || relations.isEmpty()) {
                return 0;
            }

            // 创建关系
            int count = 0;
            for (ConceptRelationInfo relation : relations) {
                try {
                    String cypher = """
                            MATCH (c1:Concept {name: $concept1})
                            MATCH (c2:Concept {name: $concept2})
                            MERGE (c1)-[r:RELATED_TO]-(c2)
                            SET r.relationType = $relationType,
                                r.strength = $strength,
                                r.confidence = 0.8
                            RETURN r
                            """;

                    neo4jClient.query(cypher)
                            .bind(relation.concept1).to("concept1")
                            .bind(relation.concept2).to("concept2")
                            .bind(relation.relationType).to("relationType")
                            .bind(relation.strength).to("strength")
                            .run();

                    count++;

                } catch (Exception e) {
                    log.warn("创建概念关系失败: {} - {}", relation.concept1, relation.concept2, e);
                }
            }

            return count;

        } catch (Exception e) {
            log.warn("分析概念关系失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 概念信息（用于LLM提取）
     */
    private record ConceptInfo(
            String name,
            String type,
            String field,
            String description,
            Double importance,
            Integer frequency,
            Double confidence
    ) {}

    /**
     * 概念关系信息（用于LLM分析）
     */
    private record ConceptRelationInfo(
            String concept1,
            String concept2,
            String relationType,
            Double strength
    ) {}

    /**
     * 作者信息（用于LLM提取）
     */
    private record AuthorInfo(
            String name,
            String affiliation,
            String email
    ) {}

    /**
     * 创建概念层次关系（IS_A）
     * 分析概念之间的上下位关系
     */
    private void createConceptHierarchy(List<String> conceptNames) {
        if (conceptNames.size() < 2) {
            return;
        }

        try {
            String prompt = """
                    请分析以下概念列表，识别它们之间的上下位关系（层次关系）。
                    例如："深度学习" IS_A "机器学习"，"机器学习" IS_A "人工智能"

                    概念列表: {concepts}

                    请返回JSON数组格式：
                    [
                      {
                        "child": "深度学习",
                        "parent": "机器学习",
                        "confidence": 0.95
                      }
                    ]

                    只返回明确的上下位关系，不确定的不要返回。
                    """
                    .replace("{concepts}", String.join(", ", conceptNames));

            List<HierarchyRelation> relations = ragChatClient.prompt()
                    .system("你是一个专业的概念分类专家，精通知识本体和分类学。请严格按照JSON数组格式返回结果。")
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<HierarchyRelation>>() {});

            if (relations == null || relations.isEmpty()) {
                log.debug("未发现概念层次关系");
                return;
            }

            // 创建 IS_A 关系
            int count = 0;
            for (HierarchyRelation relation : relations) {
                try {
                    String cypher = """
                            MATCH (child:Concept {name: $childName})
                            MATCH (parent:Concept {name: $parentName})
                            MERGE (child)-[r:IS_A]->(parent)
                            SET r.confidence = $confidence
                            RETURN r
                            """;

                    neo4jClient.query(cypher)
                            .bind(relation.child).to("childName")
                            .bind(relation.parent).to("parentName")
                            .bind(relation.confidence).to("confidence")
                            .run();

                    count++;
                    log.debug("创建层次关系: {} IS_A {}", relation.child, relation.parent);

                } catch (Exception e) {
                    log.warn("创建概念层次关系失败: {} -> {}", relation.child, relation.parent, e);
                }
            }

            log.info("创建了 {} 个概念层次关系", count);

        } catch (Exception e) {
            log.warn("分析概念层次关系失败: {}", e.getMessage());
        }
    }

    /**
     * 提取并创建文档作者关系（AUTHORED_BY）
     */
    private void createAuthorRelationships(Long documentId, String title, String content) {
        try {
            // 限制内容长度
            String limitedContent = content.length() > 2000
                    ? content.substring(0, 2000) + "..."
                    : content;

            String prompt = """
                    请从以下学术文档中提取作者信息。

                    文档标题: {title}
                    文档内容: {content}

                    请返回JSON数组格式：
                    [
                      {
                        "name": "作者姓名",
                        "affiliation": "所属机构",
                        "email": "邮箱（如果有）"
                      }
                    ]

                    如果无法确定作者信息，返回空数组 []
                    """
                    .replace("{title}", title)
                    .replace("{content}", limitedContent);

            List<AuthorInfo> authors = ragChatClient.prompt()
                    .system("你是一个专业的学术文献分析专家。请严格按照JSON数组格式返回结果，无法确定时返回空数组。")
                    .user(prompt)
                    .call()
                    .entity(new ParameterizedTypeReference<List<AuthorInfo>>() {});

            if (authors == null || authors.isEmpty()) {
                log.debug("未提取到作者信息: documentId={}", documentId);
                return;
            }

            // 创建作者节点和关系
            for (AuthorInfo author : authors) {
                try {
                    String cypher = """
                            // 创建或获取作者节点
                            MERGE (author:Author {name: $authorName})
                            ON CREATE SET
                                author.affiliation = $affiliation,
                                author.email = $email
                            ON MATCH SET
                                author.affiliation = COALESCE($affiliation, author.affiliation),
                                author.email = COALESCE($email, author.email)

                            // 创建文档-作者关系
                            WITH author
                            MATCH (doc:Document {documentId: $documentId})
                            MERGE (doc)-[r:AUTHORED_BY]->(author)
                            RETURN r
                            """;

                    neo4jClient.query(cypher)
                            .bind(author.name).to("authorName")
                            .bind(author.affiliation).to("affiliation")
                            .bind(author.email).to("email")
                            .bind(documentId).to("documentId")
                            .run();

                    log.debug("创建作者关系: documentId={}, author={}", documentId, author.name);

                } catch (Exception e) {
                    log.warn("创建作者关系失败: documentId={}, author={}", documentId, author.name, e);
                }
            }

            log.info("为文档 {} 提取了 {} 个作者", documentId, authors.size());

        } catch (Exception e) {
            log.warn("提取作者信息失败: documentId={}, error={}", documentId, e.getMessage());
        }
    }

    /**
     * 计算并创建文档相似度关系（SIMILAR_TO）
     * 基于共享概念的数量和重要性计算相似度
     */
    private void createDocumentSimilarityRelationships(Long documentId, Long userId) {
        try {
            // 查找与当前文档共享概念最多的其他文档（同一用户）
            String cypher = """
                    MATCH (currentDoc:Document {documentId: $documentId})-[:CONTAINS]->(c:Concept)
                    WITH currentDoc, collect(c) AS currentConcepts

                    MATCH (otherDoc:Document {userId: $userId})-[:CONTAINS]->(sharedConcept:Concept)
                    WHERE otherDoc.documentId <> $documentId
                      AND sharedConcept IN currentConcepts

                    WITH currentDoc, otherDoc,
                         count(DISTINCT sharedConcept) AS sharedCount,
                         avg(sharedConcept.importance) AS avgImportance

                    WHERE sharedCount >= 1

                    WITH currentDoc, otherDoc, sharedCount, avgImportance,
                         (toFloat(sharedCount) * 0.7 + avgImportance * 0.3) AS similarity

                    ORDER BY similarity DESC
                    LIMIT 5

                    MERGE (currentDoc)-[r:SIMILAR_TO]-(otherDoc)
                    SET r.similarity = similarity,
                        r.similarityType = 'SEMANTIC',
                        r.sharedConcepts = sharedCount

                    RETURN otherDoc.documentId AS similarDocId, similarity, sharedCount
                    """;

            var results = neo4jClient.query(cypher)
                    .bind(documentId).to("documentId")
                    .bind(userId).to("userId")
                    .fetch()
                    .all();

            if (!results.isEmpty()) {
                log.info("为文档 {} 创建了 {} 个相似度关系", documentId, results.size());
            }

        } catch (Exception e) {
            log.warn("创建文档相似度关系失败: documentId={}, error={}", documentId, e.getMessage());
        }
    }

    /**
     * 层次关系信息（用于LLM分析）
     */
    private record HierarchyRelation(
            String child,
            String parent,
            Double confidence
    ) {}
}
