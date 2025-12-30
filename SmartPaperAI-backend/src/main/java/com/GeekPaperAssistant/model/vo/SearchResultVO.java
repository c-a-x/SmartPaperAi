package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索结果 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索结果")
public class SearchResultVO {
    
    @Schema(description = "文档ID")
    private Long documentId;
    
    @Schema(description = "文档标题")
    private String title;
    
    @Schema(description = "文档类型")
    private String type;
    
    @Schema(description = "相关性分数")
    private Double score;
    
    @Schema(description = "高亮片段列表")
    private List<HighlightSnippet> snippets;
    
    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
    
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "高亮片段")
    public static class HighlightSnippet {
        
        @Schema(description = "片段内容（包含高亮标记）")
        private String content;
        
        @Schema(description = "片段在文档中的位置")
        private Integer position;
        
        @Schema(description = "相关性分数")
        private Double score;
    }
}
