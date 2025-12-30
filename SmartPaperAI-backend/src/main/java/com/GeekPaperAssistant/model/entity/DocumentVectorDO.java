package com.GeekPaperAssistant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档向量ID映射实体
 * 用于记录文档分块的向量ID，便于删除向量
 * 
 * @author 席崇援
 * @since 2025-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_vectors")
public class DocumentVectorDO {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 向量ID（由向量库生成）
     */
    private String vectorId;
    
    /**
     * 分块索引
     */
    private Integer chunkIndex;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
