package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 引用片段 VO
 * 
 * 前端高亮方案:
 * - snippet: 原始文本片段
 * - keywords: 关键词列表,前端使用 Mark.js 等库进行高亮渲染
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "引用片段信息")
public class CitationVO {

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "文档块ID")
    private Long chunkId;

    @Schema(description = "文档块索引")
    private Integer chunkIndex;

    @Schema(description = "相似度得分")
    private Double score;

    @Schema(description = "引用片段内容(原始文本)")
    private String snippet;

    @Schema(description = "关键词列表(前端用于高亮,如使用 Mark.js)")
    private List<String> keywords;

    @Schema(description = "页码/位置信息")
    private String position;

    @Schema(description = "元数据（JSON格式）")
    private String metadata;
}
