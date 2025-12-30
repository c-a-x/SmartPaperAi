package com.GeekPaperAssistant.repository.graph;

import com.GeekPaperAssistant.model.graph.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 概念节点仓库接口
 * 
 * @author 席崇援
 * @since 2025-10-16
 */
@Repository
public interface ConceptRepository extends Neo4jRepository<ConceptNode, String> {
    
    /**
     * 根据名称查找概念
     */
    Optional<ConceptNode> findByName(String name);
    
    /**
     * 根据名称列表查找概念
     */
    List<ConceptNode> findByNameIn(Set<String> names);
    
    /**
     * 根据领域查找概念
     */
    List<ConceptNode> findByField(String field);
    
    /**
     * 根据类型查找概念
     */
    List<ConceptNode> findByType(String type);
    
    /**
     * 查找与指定概念相关的概念（指定深度）
     */
    @Query("""
        MATCH (c:Concept {name: $conceptName})-[:RELATED_TO*1..$depth]-(related:Concept)
        RETURN DISTINCT related
        ORDER BY related.importance DESC
        LIMIT $limit
        """)
    List<ConceptNode> findRelatedConcepts(
        @Param("conceptName") String conceptName,
        @Param("depth") int depth,
        @Param("limit") int limit
    );
    
    /**
     * 查找概念的相关文档数量
     */
    @Query("""
        MATCH (doc:Document)-[:CONTAINS]->(c:Concept {name: $conceptName})
        RETURN count(doc) as documentCount
        """)
    Long countRelatedDocuments(@Param("conceptName") String conceptName);

    /**
     * 查找最热门的概念（按文档提及次数）
     */
    @Query("""
        MATCH (doc:Document)-[:CONTAINS]->(c:Concept)
        RETURN c, count(doc) as documentCount
        ORDER BY documentCount DESC
        LIMIT $limit
        """)
    List<ConceptNode> findMostPopularConcepts(@Param("limit") int limit);
    
    /**
     * 查找两个概念间的最短路径
     */
    @Query("""
        MATCH path = shortestPath((c1:Concept {name: $concept1})-[:RELATED_TO*1..6]-(c2:Concept {name: $concept2}))
        RETURN nodes(path) as conceptPath, length(path) as pathLength
        """)
    List<ConceptNode> findShortestPath(
        @Param("concept1") String concept1,
        @Param("concept2") String concept2
    );
    
    /**
     * 查找跨领域的概念连接
     */
    @Query("""
        MATCH (c1:Concept {field: $field1})-[:RELATED_TO*1..$maxDepth]-(c2:Concept)
        WHERE c2.field <> $field1 AND ($field2 IS NULL OR c2.field = $field2)
        RETURN DISTINCT c2
        ORDER BY c2.importance DESC
        LIMIT $limit
        """)
    List<ConceptNode> findCrossDomainConcepts(
        @Param("field1") String field1,
        @Param("field2") String field2,
        @Param("maxDepth") int maxDepth,
        @Param("limit") int limit
    );
    
    /**
     * 更新概念的重要性分数
     */
    @Query("""
        MATCH (c:Concept {name: $conceptName})
        SET c.importance = $importance
        RETURN c
        """)
    ConceptNode updateImportance(
        @Param("conceptName") String conceptName,
        @Param("importance") Double importance
    );
    
    /**
     * 通过概念网络查找相关文档
     */
    @Query("""
        MATCH (concept:Concept)-[:RELATED_TO*1..$depth]-(related:Concept)
        WHERE concept.name IN $concepts
        MATCH (doc:Document {userId: $userId})-[:CONTAINS]->(related)
        WITH DISTINCT doc, count(related) AS relatedCount
        RETURN doc
        ORDER BY relatedCount DESC
        LIMIT $limit
        """)
    List<com.GeekPaperAssistant.model.graph.DocumentNode> findDocumentsByConceptNetwork(
        @Param("concepts") Set<String> concepts,
        @Param("userId") Long userId,
        @Param("depth") int depth,
        @Param("limit") int limit
    );

    /**
     * 创建概念间的关系
     */
    @Query("""
        MATCH (c1:Concept {name: $concept1}), (c2:Concept {name: $concept2})
        MERGE (c1)-[r:RELATED_TO]->(c2)
        SET r.strength = $strength, r.relationType = $relationType
        RETURN r
        """)
    void createConceptRelation(
        @Param("concept1") String concept1,
        @Param("concept2") String concept2,
        @Param("strength") Double strength,
        @Param("relationType") String relationType
    );
}