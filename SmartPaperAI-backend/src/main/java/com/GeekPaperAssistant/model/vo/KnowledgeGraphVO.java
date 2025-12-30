package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识图谱可视化数据 VO
 * 用于前端图谱展示（类似 Neo4j Browser 效果）
 * 
 * @author 席崇援
 * @since 2025-10-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识图谱可视化数据")
public class KnowledgeGraphVO {

    @Schema(description = "节点列表")
    private List<GraphNode> nodes;

    @Schema(description = "关系列表")
    private List<GraphRelationship> relationships;

    @Schema(description = "统计信息")
    private GraphStats stats;

    /**
     * 图节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图节点")
    public static class GraphNode {
        
        @Schema(description = "节点ID（唯一标识）")
        private String id;
        
        @Schema(description = "节点类型", example = "Concept, Document, Author")
        private String type;
        
        @Schema(description = "节点显示名称")
        private String label;
        
        @Schema(description = "节点详细信息")
        private NodeProperties properties;
    }

    /**
     * 节点属性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "节点属性")
    public static class NodeProperties {
        
        @Schema(description = "名称")
        private String name;
        
        @Schema(description = "描述")
        private String description;
        
        @Schema(description = "类型/分类")
        private String category;
        
        @Schema(description = "所属领域")
        private String field;
        
        @Schema(description = "重要度评分（0-1）")
        private Double importance;
        
        @Schema(description = "频次")
        private Integer frequency;
        
        @Schema(description = "文档ID（仅Document类型）")
        private Long documentId;
        
        @Schema(description = "文档标题（仅Document类型）")
        private String title;
        
        @Schema(description = "用户ID（仅Document类型）")
        private Long userId;
    }

    /**
     * 图关系
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图关系")
    public static class GraphRelationship {
        
        @Schema(description = "关系ID")
        private String id;
        
        @Schema(description = "源节点ID")
        private String source;
        
        @Schema(description = "目标节点ID")
        private String target;
        
        @Schema(description = "关系类型", example = "CONTAINS, RELATED_TO, CITES, SIMILAR_TO")
        private String type;
        
        @Schema(description = "关系显示标签")
        private String label;
        
        @Schema(description = "关系属性")
        private RelationshipProperties properties;
    }

    /**
     * 关系属性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "关系属性")
    public static class RelationshipProperties {
        
        @Schema(description = "权重/强度（0-1）")
        private Double weight;
        
        @Schema(description = "置信度（0-1）")
        private Double confidence;
        
        @Schema(description = "关系描述")
        private String description;
    }

    /**
     * 图统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图统计信息")
    public static class GraphStats {
        
        @Schema(description = "节点总数")
        private Integer totalNodes;
        
        @Schema(description = "关系总数")
        private Integer totalRelationships;
        
        @Schema(description = "概念节点数")
        private Integer conceptNodes;
        
        @Schema(description = "文档节点数")
        private Integer documentNodes;
        
        @Schema(description = "作者节点数")
        private Integer authorNodes;
    }
}
