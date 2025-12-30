-- 创建 AI 聊天会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
    `id` BIGINT(20) NOT NULL COMMENT '会话ID',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话标识符(用于 ChatMemory)',
    `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
    `title` VARCHAR(255) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_conversation_id` (`conversation_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_update_time` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 聊天会话表';

-- 创建 AI 聊天消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id` BIGINT(20) NOT NULL COMMENT '消息ID',
    `session_id` BIGINT(20) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '消息角色(USER/ASSISTANT/SYSTEM)',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_session_id` (`session_id`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 聊天消息表';
