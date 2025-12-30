package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.service.GraphEnhancedRAGService;
import com.GeekPaperAssistant.model.graph.ConceptNode;
import com.GeekPaperAssistant.model.graph.DocumentNode;
import com.GeekPaperAssistant.model.vo.KnowledgeGraphVO;
import com.GeekPaperAssistant.repository.graph.ConceptRepository;
import com.GeekPaperAssistant.repository.graph.DocumentNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import cn.dev33.satoken.stp.StpUtil;

import java.util.*;

/**
 * 图增强的RAG服务实现类
 *
 * @author 席崇援
 * @since 2025-10-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphEnhancedRAGServiceImpl implements GraphEnhancedRAGService {

    private final ConceptRepository conceptRepository;
    private final DocumentNodeRepository documentNodeRepository;
    private final ChatClient ragChatClient;
    private final Neo4jClient neo4jClient;

    @Override
    public Set<Long> findDocumentsByConceptNetwork(List<String> concepts, Long userId, int maxDepth) {
        if (concepts.isEmpty()) {
            return new HashSet<>();
        }

        try {
            // 使用 Neo4jClient 执行动态 Cypher 查询（因为 Neo4j 不支持在路径长度中使用参数）
            String cypher = String.format("""
                MATCH (concept:Concept)-[:RELATED_TO*1..%d]-(related:Concept)
                WHERE concept.name IN $concepts
                MATCH (doc:Document {userId: $userId})-[:CONTAINS]->(related)
                WITH DISTINCT doc, count(related) AS relatedCount
                RETURN doc.documentId AS documentId
                ORDER BY relatedCount DESC
                LIMIT 50
                """, maxDepth);

            Collection<Long> documentIds = neo4jClient.query(cypher)
                .bind(concepts).to("concepts")
                .bind(userId).to("userId")
                .fetchAs(Long.class)
                .mappedBy((typeSystem, record) -> record.get("documentId").asLong())
                .all();

            return new HashSet<>(documentIds);

        } catch (Exception e) {
            log.warn("图查询失败，返回空集合: {}", e.getMessage());
            return new HashSet<>();
        }
    }
    
    @Override
    public KnowledgeGraphVO buildDocumentKnowledgeGraph(Long documentId, String query) {
        try {
            log.info("构建文档知识图谱: documentId={}, query={}", documentId, query);
            
            // 1. 从查询中提取概念
            List<String> extractedConcepts = extractConceptsFromQuery(query);
            log.debug("提取的概念: {}", extractedConcepts);
            
            // 2. 查找与此文档相关的文档（通过概念网络）
            Set<Long> relatedDocIds = findDocumentsByConceptNetwork(
                extractedConcepts, getCurrentUserId(), 1);
            
            // 3. 确保包含当前文档
            relatedDocIds.add(documentId);
            
            // 4. 构建知识图谱可视化
            return buildKnowledgeGraphVisualization(extractedConcepts, relatedDocIds, getCurrentUserId());
            
        } catch (Exception e) {
            log.error("构建文档知识图谱失败: documentId={}", documentId, e);
            // 返回空图谱
            return KnowledgeGraphVO.builder()
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .stats(KnowledgeGraphVO.GraphStats.builder()
                    .totalNodes(0)
                    .totalRelationships(0)
                    .conceptNodes(0)
                    .documentNodes(0)
                    .authorNodes(0)
                    .build())
                .build();
        }
    }
    
    /**
     * 从查询中提取概念实体
     */
    private List<String> extractConceptsFromQuery(String query) {
        try {
            String prompt = """
                从以下用户查询中提取关键概念实体，包括：
                1. 专业术语和技术概念
                2. 人名、机构名
                3. 学科领域
                4. 方法和理论名称

                用户查询: {query}

                请直接返回JSON数组格式的概念列表，例如：["概念1", "概念2", "概念3"]
                不要包含其他字段，不要添加额外的解释，只返回纯JSON数组。
                """.replace("{query}", query);

            List<String> concepts = ragChatClient.prompt()
                    .system("你是一个专业的概念提取专家。请严格按照要求返回JSON数组格式，不要添加任何额外的包装对象或字段。")
                    .user(prompt)
                    .call()
                    .entity(new org.springframework.core.ParameterizedTypeReference<List<String>>() {});

            return concepts != null && !concepts.isEmpty() ? concepts : new ArrayList<>();

        } catch (Exception e) {
            log.warn("概念提取失败，使用关键词分割作为降级方案: {}", e.getMessage());
            // 降级方案：简单的关键词提取
            String[] keywords = query.split("\\s+");
            List<String> result = new ArrayList<>();
            for (String keyword : keywords) {
                if (keyword.length() >= 2) { // 过滤掉单字词
                    result.add(keyword);
                }
            }
            return result.isEmpty() ? List.of(query) : result;
        }
    }
    
    /**
     * 查找相关概念（使用动态 Cypher）
     */
    private List<ConceptNode> findRelatedConcepts(String conceptName, int depth, int limit) {
        try {
            String cypher = String.format("""
                MATCH (c:Concept {name: $conceptName})-[:RELATED_TO*1..%d]-(related:Concept)
                RETURN DISTINCT related
                ORDER BY related.importance DESC
                LIMIT $limit
                """, depth);

            Collection<ConceptNode> concepts = neo4jClient.query(cypher)
                .bind(conceptName).to("conceptName")
                .bind(limit).to("limit")
                .fetchAs(ConceptNode.class)
                .mappedBy((typeSystem, record) -> {
                    var node = record.get("related").asNode();
                    ConceptNode concept = new ConceptNode();
                    concept.setId(node.get("id").asString());
                    concept.setName(node.get("name").asString());
                    concept.setDescription(node.get("description").asString(null));
                    concept.setType(node.get("type").asString(null));
                    concept.setField(node.get("field").asString(null));
                    concept.setImportance(node.get("importance").asDouble(0.5));
                    concept.setFrequency(node.get("frequency").asInt(1));
                    return concept;
                })
                .all();

            return new ArrayList<>(concepts);

        } catch (Exception e) {
            log.warn("查找相关概念失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.warn("获取用户ID失败，使用默认值", e);
            return 1L;
        }
    }
    
    /**
     * 构建知识图谱可视化数据
     * 
     * @param concepts 相关概念
     * @param documentIds 相关文档ID
     * @param userId 用户ID
     * @return 知识图谱可视化数据
     */
    private KnowledgeGraphVO buildKnowledgeGraphVisualization(
            List<String> concepts, Set<Long> documentIds, Long userId) {
        
        try {
            log.debug("开始构建知识图谱可视化数据: concepts={}, documents={}", concepts.size(), documentIds.size());
            
            List<KnowledgeGraphVO.GraphNode> nodes = new ArrayList<>();
            List<KnowledgeGraphVO.GraphRelationship> relationships = new ArrayList<>();
            
            // 用于统计的计数器
            int conceptCount = 0;
            int documentCount = 0;
            
            // 1. 添加概念节点和它们之间的关系
            Set<String> addedConceptIds = new HashSet<>();
            for (String conceptName : concepts.stream().limit(10).toList()) {
                Optional<ConceptNode> conceptOpt = conceptRepository.findByName(conceptName);
                if (conceptOpt.isPresent()) {
                    ConceptNode concept = conceptOpt.get();
                    String nodeId = "concept-" + concept.getId();
                    
                    if (!addedConceptIds.contains(nodeId)) {
                        // 添加概念节点
                        nodes.add(KnowledgeGraphVO.GraphNode.builder()
                            .id(nodeId)
                            .type("Concept")
                            .label(concept.getName())
                            .properties(KnowledgeGraphVO.NodeProperties.builder()
                                .name(concept.getName())
                                .description(concept.getDescription())
                                .category(concept.getType())
                                .field(concept.getField())
                                .importance(concept.getImportance())
                                .frequency(concept.getFrequency())
                                .build())
                            .build());
                        
                        addedConceptIds.add(nodeId);
                        conceptCount++;

                        // 添加概念间的关系（查找相关概念，限制数量）
                        List<ConceptNode> relatedConcepts = findRelatedConcepts(conceptName, 1, 3);
                        
                        for (ConceptNode related : relatedConcepts) {
                            String relatedNodeId = "concept-" + related.getId();
                            
                            // 添加相关概念节点（如果还没添加）
                            if (!addedConceptIds.contains(relatedNodeId)) {
                                nodes.add(KnowledgeGraphVO.GraphNode.builder()
                                    .id(relatedNodeId)
                                    .type("Concept")
                                    .label(related.getName())
                                    .properties(KnowledgeGraphVO.NodeProperties.builder()
                                        .name(related.getName())
                                        .description(related.getDescription())
                                        .category(related.getType())
                                        .field(related.getField())
                                        .importance(related.getImportance())
                                        .frequency(related.getFrequency())
                                        .build())
                                    .build());
                                
                                addedConceptIds.add(relatedNodeId);
                                conceptCount++;
                            }
                            
                            // 添加关系
                            relationships.add(KnowledgeGraphVO.GraphRelationship.builder()
                                .id("rel-" + nodeId + "-" + relatedNodeId)
                                .source(nodeId)
                                .target(relatedNodeId)
                                .type("RELATED_TO")
                                .label("相关")
                                .properties(KnowledgeGraphVO.RelationshipProperties.builder()
                                    .weight(0.8)
                                    .confidence(0.9)
                                    .build())
                                .build());
                        }
                    }
                }
            }
            
            // 2. 添加文档节点
            Set<String> addedDocumentIds = new HashSet<>();
            for (Long docId : documentIds.stream().limit(8).toList()) {
                Optional<DocumentNode> docOpt = documentNodeRepository.findByDocumentId(docId);
                if (docOpt.isPresent()) {
                    DocumentNode doc = docOpt.get();
                    String nodeId = "document-" + doc.getDocumentId();
                    
                    if (!addedDocumentIds.contains(nodeId)) {
                        // 添加文档节点
                        nodes.add(KnowledgeGraphVO.GraphNode.builder()
                            .id(nodeId)
                            .type("Document")
                            .label(doc.getTitle())
                            .properties(KnowledgeGraphVO.NodeProperties.builder()
                                .name(doc.getTitle())
                                .category(doc.getType())
                                .documentId(doc.getDocumentId())
                                .title(doc.getTitle())
                                .userId(doc.getUserId())
                                .build())
                            .build());
                        
                        addedDocumentIds.add(nodeId);
                        documentCount++;
                        
                        // 3. 添加文档与概念的关系
                        for (String conceptName : concepts.stream().limit(5).toList()) {
                            Optional<ConceptNode> conceptOpt = conceptRepository.findByName(conceptName);
                            if (conceptOpt.isPresent()) {
                                String conceptNodeId = "concept-" + conceptOpt.get().getId();
                                
                                if (addedConceptIds.contains(conceptNodeId)) {
                                    // 检查文档是否真的包含这个概念（简化版，实际应该查询关系）
                                    relationships.add(KnowledgeGraphVO.GraphRelationship.builder()
                                        .id("rel-" + nodeId + "-" + conceptNodeId)
                                        .source(nodeId)
                                        .target(conceptNodeId)
                                        .type("CONTAINS")
                                        .label("包含")
                                        .properties(KnowledgeGraphVO.RelationshipProperties.builder()
                                            .weight(0.7)
                                            .confidence(0.85)
                                            .description("文档包含此概念")
                                            .build())
                                        .build());
                                }
                            }
                        }
                    }
                }
            }
            
            // 4. 构建统计信息
            KnowledgeGraphVO.GraphStats stats = 
                KnowledgeGraphVO.GraphStats.builder()
                    .totalNodes(nodes.size())
                    .totalRelationships(relationships.size())
                    .conceptNodes(conceptCount)
                    .documentNodes(documentCount)
                    .authorNodes(0)  // 暂时不包含作者节点
                    .build();
            
            log.info("知识图谱构建完成: {} 个节点, {} 个关系", nodes.size(), relationships.size());
            
            return KnowledgeGraphVO.builder()
                .nodes(nodes)
                .relationships(relationships)
                .stats(stats)
                .build();
                
        } catch (Exception e) {
            log.error("构建知识图谱可视化数据失败", e);
            // 返回空图谱
            return KnowledgeGraphVO.builder()
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .stats(KnowledgeGraphVO.GraphStats.builder()
                    .totalNodes(0)
                    .totalRelationships(0)
                    .conceptNodes(0)
                    .documentNodes(0)
                    .authorNodes(0)
                    .build())
                .build();
        }
    }

    @Override
    public KnowledgeGraphVO buildKnowledgeBaseGraph(Long knowledgeBaseId, String query, int limit) {
        try {
            log.info("构建知识库知识图谱: knowledgeBaseId={}, query={}, limit={}", knowledgeBaseId, query, limit);

            Long userId = getCurrentUserId();

            // 1. 查询知识库中的所有文档
            String findDocsCypher = """
                    MATCH (doc:Document {userId: $userId, kbId: $kbId})
                    RETURN doc.documentId AS documentId
                    LIMIT $limit
                    """;

            Collection<Long> documentIds = neo4jClient.query(findDocsCypher)
                    .bind(userId).to("userId")
                    .bind(knowledgeBaseId).to("kbId")
                    .bind(limit).to("limit")
                    .fetchAs(Long.class)
                    .mappedBy((typeSystem, record) -> record.get("documentId").asLong())
                    .all();

            if (documentIds.isEmpty()) {
                log.warn("知识库中没有文档: knowledgeBaseId={}", knowledgeBaseId);
                return buildEmptyGraph();
            }

            // 2. 提取查询概念（如果有查询）
            List<String> concepts = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                concepts = extractConceptsFromQuery(query);
            }

            // 3. 构建知识图谱可视化
            return buildKnowledgeGraphVisualization(concepts, new HashSet<>(documentIds), userId);

        } catch (Exception e) {
            log.error("构建知识库知识图谱失败: knowledgeBaseId={}", knowledgeBaseId, e);
            return buildEmptyGraph();
        }
    }

    @Override
    public KnowledgeGraphVO buildGlobalKnowledgeGraph(String query, int limit) {
        try {
            log.info("构建全局知识图谱: query={}, limit={}", query, limit);

            Long userId = getCurrentUserId();

            // 1. 查询用户的所有文档
            String findDocsCypher = """
                    MATCH (doc:Document {userId: $userId})
                    RETURN doc.documentId AS documentId
                    ORDER BY doc.createTime DESC
                    LIMIT $limit
                    """;

            Collection<Long> documentIds = neo4jClient.query(findDocsCypher)
                    .bind(userId).to("userId")
                    .bind(limit).to("limit")
                    .fetchAs(Long.class)
                    .mappedBy((typeSystem, record) -> record.get("documentId").asLong())
                    .all();

            if (documentIds.isEmpty()) {
                log.warn("用户没有文档: userId={}", userId);
                return buildEmptyGraph();
            }

            // 2. 提取查询概念（如果有查询）
            List<String> concepts = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                concepts = extractConceptsFromQuery(query);
            } else {
                // 如果没有查询，获取最热门的概念
                String findTopConceptsCypher = """
                        MATCH (doc:Document {userId: $userId})-[:CONTAINS]->(c:Concept)
                        RETURN c.name AS conceptName, count(doc) AS docCount
                        ORDER BY docCount DESC, c.importance DESC
                        LIMIT 20
                        """;

                Collection<String> topConcepts = neo4jClient.query(findTopConceptsCypher)
                        .bind(userId).to("userId")
                        .fetchAs(String.class)
                        .mappedBy((typeSystem, record) -> record.get("conceptName").asString())
                        .all();

                concepts = new ArrayList<>(topConcepts);
            }

            // 3. 构建知识图谱可视化
            return buildKnowledgeGraphVisualization(concepts, new HashSet<>(documentIds), userId);

        } catch (Exception e) {
            log.error("构建全局知识图谱失败", e);
            return buildEmptyGraph();
        }
    }

    /**
     * 构建空图谱
     */
    private KnowledgeGraphVO buildEmptyGraph() {
        return KnowledgeGraphVO.builder()
                .nodes(new ArrayList<>())
                .relationships(new ArrayList<>())
                .stats(KnowledgeGraphVO.GraphStats.builder()
                        .totalNodes(0)
                        .totalRelationships(0)
                        .conceptNodes(0)
                        .documentNodes(0)
                        .authorNodes(0)
                        .build())
                .build();
    }


}