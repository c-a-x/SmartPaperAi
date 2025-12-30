package com.GeekPaperAssistant.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文档块实体
 * 
 * @author 席崇援
 * @since 2025-10-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "document_chunks", autoResultMap = true)
public class DocumentChunkDO {
    
    /**
     * 块ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 块索引
     */
    private Integer chunkIndex;
    
    /**
     * 文本内容
     */
    private String content;
    
    /**
     * Redis向量ID
     */
    private String vectorId;
    
    /**
     * Token数量
     */
    private Integer tokens;
    
    /**
     * 块元数据 (JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 是否删除(0:否 1:是)
     */
    private Integer isDeleted;
}
