
package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求
 */
@Data
@Schema(description = "聊天请求")
public class ChatRequestDTO {

    @Schema(description = "会话ID(可选,不传则创建新会话)")
    private String conversationId;

    @Schema(description = "用户消息", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    private String message;

    @Schema(description = "是否启用RAG(检索增强生成),默认false")
    private Boolean enableRag = false;

    @Schema(description = "RAG检索文档数量,默认999")
    private Integer ragTopK = 999;
    
    @Schema(description = "自定义温度参数(0.0-2.0),覆盖默认配置")
    private Double customTemperature;
    
    @Schema(description = "自定义最大Token数,覆盖默认配置")
    private Integer customMaxTokens;
    
    @Schema(description = "自定义相似度阈值(RAG时使用),覆盖默认配置")
    private Double customSimilarityThreshold;
    
    @Schema(description = "是否启用对话记忆,默认true")
    private Boolean enableMemory = true;
}
