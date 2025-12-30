package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建知识库 DTO
 * 
 * @author ican
 */
@Data
@Schema(description = "创建知识库请求")
public class CreateKBDTO {
    
    @NotBlank(message = "知识库名称不能为空")
    @Schema(description = "知识库名称")
    private String name;
    
    @Schema(description = "描述")
    private String description;
}
