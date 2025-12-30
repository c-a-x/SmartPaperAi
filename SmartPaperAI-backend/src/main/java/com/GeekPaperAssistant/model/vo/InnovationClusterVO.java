package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创新点聚合结果 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创新点聚合结果")
public class InnovationClusterVO {
    
    @Schema(description = "主题名称")
    private String topic;
    
    @Schema(description = "主题的详细描述")
    private String description;
    
    @Schema(description = "创新点列表")
    private List<InnovationPoint> innovations;
    
    @Schema(description = "重要性分数")
    private Double importance;
    
    @Schema(description = "相关论文数量")
    private Integer paperCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单个创新点")
    public static class InnovationPoint {
        
        @Schema(description = "创新点描述")
        private String description;
        
        @Schema(description = "论文标题")
        private String paperTitle;
        
        @Schema(description = "文档ID")
        private Long documentId;
        
        @Schema(description = "新颖性分数")
        private Double noveltyScore;
    }
}
