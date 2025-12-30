package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库查询DTO
 * 
 * @author ican
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库查询参数")
public class KnowledgeBaseQueryDTO extends PageQueryDTO {
    
    /**
     * 知识库名称（模糊查询）
     */
    @Schema(description = "知识库名称", example = "AI研究")
    private String name;
    
    /**
     * 描述（模糊查询）
     */
    @Schema(description = "描述关键词")
    private String description;
}
