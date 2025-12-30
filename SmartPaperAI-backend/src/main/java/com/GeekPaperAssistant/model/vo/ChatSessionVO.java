
package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天会话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天会话响应")
public class ChatSessionVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标识符")
    private String conversationId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
