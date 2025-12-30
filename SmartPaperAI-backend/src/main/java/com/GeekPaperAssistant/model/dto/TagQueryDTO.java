package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签查询DTO
 * 
 * @author ican
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "标签查询参数")
public class TagQueryDTO extends PageQueryDTO {
    
    /**
     * 标签名称（模糊查询）
     */
    @Schema(description = "标签名称", example = "机器学习")
    private String name;
    
    /**
     * 标签颜色
     */
    @Schema(description = "标签颜色", example = "#ff0000")
    private String color;
}
