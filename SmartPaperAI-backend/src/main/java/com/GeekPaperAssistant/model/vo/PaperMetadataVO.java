package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 论文元数据 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "论文元数据")
public class PaperMetadataVO {

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "论文标题")
    private String title;

    @Schema(description = "作者列表")
    private List<String> authors;

    @Schema(description = "发表年份")
    private String year;

    @Schema(description = "期刊/会议名称")
    private String publication;

    @Schema(description = "卷号")
    private String volume;

    @Schema(description = "期号")
    private String issue;

    @Schema(description = "页码")
    private String pages;

    @Schema(description = "DOI")
    private String doi;

    @Schema(description = "关键词")
    private List<String> keywords;

    @Schema(description = "摘要")
    private String abstractText;

    @Schema(description = "引用数")
    private Integer citationCount;

    @Schema(description = "领域/学科")
    private String field;
}
