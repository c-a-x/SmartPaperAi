package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量引用导出结果 VO
 *
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量引用导出结果")
public class BatchCitationVO {

    /**
     * 引用格式类型
     */
    @Schema(description = "引用格式类型")
    private String format;

    /**
     * 引用数量
     */
    @Schema(description = "引用数量")
    private Integer count;

    /**
     * 引用列表
     */
    @Schema(description = "引用列表")
    private List<CitationItem> citations;

    /**
     * 单个引用项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单个引用项")
    public static class CitationItem {
        
        /**
         * 文档标题
         */
        @Schema(description = "文档标题")
        private String title;

        /**
         * 引用内容
         */
        @Schema(description = "引用内容")
        private String citation;

        /**
         * 错误信息(可选)
         */
        @Schema(description = "错误信息")
        private String error;
    }
}
