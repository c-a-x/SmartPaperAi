
package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天响应")
public class ChatResponseVO {

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "用户消息")
    private String userMessage;

    @Schema(description = "AI 回复")
    private String aiResponse;

    @Schema(description = "响应时间")
    private LocalDateTime timestamp;
}
