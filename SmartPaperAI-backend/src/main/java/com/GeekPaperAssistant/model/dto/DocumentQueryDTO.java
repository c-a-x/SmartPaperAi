package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档查询DTO
 * 
 * @author ican
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文档查询参数")
public class DocumentQueryDTO extends PageQueryDTO {
    
    /**
     * 文档标题（模糊查询）
     */
    @Schema(description = "文档标题", example = "人工智能")
    private String title;
    
    /**
     * 文档类型
     */
    @Schema(description = "文档类型", example = "research_paper")
    private String type;
    
    /**
     * 处理状态
     */
    @Schema(description = "处理状态", example = "completed")
    private String status;
    
    /**
     * 知识库ID
     */
    @Schema(description = "知识库ID")
    private Long kbId;
}
