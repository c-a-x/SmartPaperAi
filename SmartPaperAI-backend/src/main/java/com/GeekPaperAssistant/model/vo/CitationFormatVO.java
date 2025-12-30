package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用格式结果 VO
 *
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "引用格式结果")
public class CitationFormatVO {

    /**
     * 引用格式类型
     */
    @Schema(description = "引用格式类型(apa/bibtex/mla/gbt7714)")
    private String format;

    /**
     * 引用内容
     */
    @Schema(description = "引用内容")
    private String citation;
}
