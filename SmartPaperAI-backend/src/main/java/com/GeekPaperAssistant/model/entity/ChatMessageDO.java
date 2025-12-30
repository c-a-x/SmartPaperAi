
package com.GeekPaperAssistant.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 聊天消息实体
 */
@Data
@TableName("ai_chat_message")
public class ChatMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 消息角色 (USER/ASSISTANT/SYSTEM)
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDeleted;
}
