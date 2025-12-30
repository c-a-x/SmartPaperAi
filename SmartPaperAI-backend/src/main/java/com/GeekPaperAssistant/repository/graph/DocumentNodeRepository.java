package com.GeekPaperAssistant.repository.graph;

import com.GeekPaperAssistant.model.graph.DocumentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 文档节点仓库接口
 * 
 * @author 席崇援
 * @since 2025-10-16
 */
@Repository
public interface DocumentNodeRepository extends Neo4jRepository<DocumentNode, Long> {
    
    /**
     * 根据文档ID查找
     */
    Optional<DocumentNode> findByDocumentId(Long documentId);
    
    /**
     * 根据用户ID查找所有文档
     */
    List<DocumentNode> findByUserId(Long userId);
    
    /**
     * 根据知识库ID查找文档
     */
    List<DocumentNode> findByKbId(Long kbId);
    
    /**
     * 根据文档类型查找
     */
    List<DocumentNode> findByType(String type);
    
    /**
     * 查找包含指定概念的文档
     */
    @Query("""
        MATCH (doc:Document)-[:CONTAINS]->(concept:Concept {name: $conceptName})
        WHERE doc.userId = $userId
        RETURN doc
        ORDER BY doc.createTime DESC
        """)
    List<DocumentNode> findDocumentsByConceptAndUser(
        @Param("conceptName") String conceptName,
        @Param("userId") Long userId
    );
    
    /**
     * 查找与指定文档相似的文档
     */
    @Query("""
        MATCH (doc:Document {documentId: $documentId})-[r:SIMILAR_TO]-(similar:Document)
        RETURN similar, r.similarity as similarity
        ORDER BY r.similarity DESC
        LIMIT $limit
        """)
    List<DocumentNode> findSimilarDocuments(
        @Param("documentId") Long documentId,
        @Param("limit") int limit
    );

    /**
     * 通过概念网络查找相关文档
     */
    @Query("""
        MATCH (concept:Concept)-[:RELATED_TO*1..$depth]-(related:Concept)
        WHERE concept.name IN $concepts
        MATCH (doc:Document {userId: $userId})-[:CONTAINS]->(related)
        RETURN DISTINCT doc, count(related) as relevanceScore
        ORDER BY relevanceScore DESC
        LIMIT $limit
        """)
    List<DocumentNode> findDocumentsByConceptNetwork(
        @Param("concepts") Set<String> concepts,
        @Param("userId") Long userId,
        @Param("depth") int depth,
        @Param("limit") int limit
    );
    
    /**
     * 查找文档的概念分布
     */
    @Query("""
        MATCH (doc:Document {documentId: $documentId})-[r:CONTAINS]->(concept:Concept)
        RETURN concept.name as conceptName, concept.type as conceptType, r.importance as importance
        ORDER BY r.importance DESC
        """)
    List<Object[]> getDocumentConceptDistribution(@Param("documentId") Long documentId);
    
    /**
     * 创建文档-概念关系
     */
    @Query("""
        MATCH (doc:Document {documentId: $documentId}), (concept:Concept {name: $conceptName})
        MERGE (doc)-[r:CONTAINS]->(concept)
        SET r.importance = $importance, r.frequency = $frequency, r.confidence = $confidence
        RETURN r
        """)
    void createDocumentConceptRelation(
        @Param("documentId") Long documentId,
        @Param("conceptName") String conceptName,
        @Param("importance") Double importance,
        @Param("frequency") Integer frequency,
        @Param("confidence") Double confidence
    );
    
}