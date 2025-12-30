package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 论文对比 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论文对比信息")
public class PaperComparisonVO {

    @Schema(description = "对比维度列表")
    private List<ComparisonDimension> dimensions;

    @Schema(description = "论文列表")
    private List<PaperInComparison> papers;

    @Schema(description = "对比矩阵(dimension -> paper -> value)")
    private List<ComparisonRow> matrix;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "对比维度")
    public static class ComparisonDimension {
        @Schema(description = "维度ID")
        private String id;

        @Schema(description = "维度名称")
        private String name;

        @Schema(description = "维度描述")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "对比中的论文")
    public static class PaperInComparison {
        @Schema(description = "文档ID")
        private Long documentId;

        @Schema(description = "论文标题")
        private String title;

        @Schema(description = "作者")
        private String authors;

        @Schema(description = "年份")
        private String year;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "对比矩阵行")
    public static class ComparisonRow {
        @Schema(description = "维度ID")
        private String dimensionId;

        @Schema(description = "维度名称")
        private String dimensionName;

        @Schema(description = "各论文在该维度的值")
        private List<String> values;
    }
}
