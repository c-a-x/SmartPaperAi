package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基础DTO
 * 
 * @author ican
 */
@Data
@Schema(description = "分页查询参数")
public class PageQueryDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页码（从1开始）
     */
    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "createTime")
    private String sortField;
    
    /**
     * 排序方式: asc, desc
     */
    @Schema(description = "排序方式", example = "desc")
    private String sortOrder = "desc";
}
