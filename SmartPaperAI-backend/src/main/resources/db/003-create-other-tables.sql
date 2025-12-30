-- ============================
-- SmartPaperAI RAG文档管理与知识库表
-- ============================

USE `ican`;

-- 文档表
CREATE TABLE IF NOT EXISTS `documents` (
    `id` BIGINT(20) NOT NULL COMMENT '文档ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '上传用户ID',
    `kb_id` BIGINT(20) DEFAULT NULL COMMENT '所属知识库ID(可选)',
    `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
    `type` VARCHAR(50) NOT NULL COMMENT '文档类型: research_paper, teaching_material, other',
    `file_url` VARCHAR(500) NOT NULL COMMENT 'MinIO文件路径',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `status` VARCHAR(20) COMMENT '处理状态: pending, processing, completed, failed',
    `metadata` JSON COMMENT '文档元数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_kb_id` (`kb_id`) USING BTREE,
    KEY `idx_type` (`type`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档表';

-- 文档块表
CREATE TABLE IF NOT EXISTS `document_chunks` (
    `id` BIGINT(20) NOT NULL COMMENT '块ID',
    `document_id` BIGINT(20) NOT NULL COMMENT '文档ID',
    `chunk_index` INT NOT NULL COMMENT '块索引',
    `content` TEXT NOT NULL COMMENT '文本内容',
    `vector_id` VARCHAR(255) COMMENT 'Redis向量ID',
    `tokens` INT COMMENT 'Token数量',
    `metadata` JSON COMMENT '块元数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_document_id` (`document_id`) USING BTREE,
    KEY `idx_chunk_index` (`chunk_index`) USING BTREE,
    KEY `idx_vector_id` (`vector_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档块表';

-- 文档向量映射表
CREATE TABLE IF NOT EXISTS `document_vectors` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `document_id` BIGINT(20) NOT NULL COMMENT '文档ID',
    `vector_id` VARCHAR(255) NOT NULL COMMENT '向量ID(由向量库生成)',
    `chunk_index` INT COMMENT '分块索引',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_document_id` (`document_id`) USING BTREE,
    KEY `idx_vector_id` (`vector_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档向量ID映射表';

-- 文档处理任务表
CREATE TABLE IF NOT EXISTS `document_task` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `document_id` BIGINT(20) NOT NULL COMMENT '文档ID',
    `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型: parse, vectorize, extract_metadata',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending, processing, completed, failed',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retries` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `error_message` TEXT COMMENT '错误信息',
    `progress` INT NOT NULL DEFAULT 0 COMMENT '进度(0-100)',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_document_id` (`document_id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE,
    KEY `idx_task_type` (`task_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档处理任务表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) COMMENT '知识库描述',
    `user_id` BIGINT(20) NOT NULL COMMENT '创建用户ID',
    `document_count` INT NOT NULL DEFAULT 0 COMMENT '文档数量',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `color` VARCHAR(20) COMMENT '标签颜色',
    `user_id` BIGINT(20) NOT NULL COMMENT '创建用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_name` (`name`) USING BTREE,
    UNIQUE KEY `uk_user_name` (`user_id`, `name`) USING BTREE COMMENT '同一用户标签名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

-- 文档标签关联表
CREATE TABLE IF NOT EXISTS `document_tag` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `document_id` BIGINT(20) NOT NULL COMMENT '文档ID',
    `tag_id` BIGINT(20) NOT NULL COMMENT '标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_document_id` (`document_id`) USING BTREE,
    KEY `idx_tag_id` (`tag_id`) USING BTREE,
    UNIQUE KEY `uk_doc_tag` (`document_id`, `tag_id`) USING BTREE COMMENT '文档标签关联唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文档标签关联表';

-- 教学设计表
CREATE TABLE IF NOT EXISTS `teaching_plans` (
    `id` BIGINT(20) NOT NULL COMMENT '教案ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '创建用户ID',
    `title` VARCHAR(255) NOT NULL COMMENT '课题名称',
    `grade` VARCHAR(50) COMMENT '学段',
    `subject` VARCHAR(50) COMMENT '学科',
    `content` JSON COMMENT '教案内容(结构化JSON)',
    `source_papers` JSON COMMENT '参考论文ID列表',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除(0:否 1:是)',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_grade` (`grade`) USING BTREE,
    KEY `idx_subject` (`subject`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学设计表';

-- ============================
-- 说明:
-- 1. 文档表(documents): 存储上传的文档基本信息
-- 2. 文档块表(document_chunks): 存储文档分块后的内容
-- 3. 文档向量映射表(document_vectors): 记录向量ID与文档的映射关系，便于删除
-- 4. 文档处理任务表(document_task): 跟踪文档处理任务的状态和进度
-- 5. 知识库表(knowledge_base): 存储用户创建的知识库
-- 6. 标签表(tag): 存储用户自定义的标签
-- 7. 文档标签关联表(document_tag): 多对多关联文档和标签
-- 8. 教学设计表(teaching_plans): 存储AI生成的教学设计方案
-- ============================
