
package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天消息响应")
public class ChatMessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息角色")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
