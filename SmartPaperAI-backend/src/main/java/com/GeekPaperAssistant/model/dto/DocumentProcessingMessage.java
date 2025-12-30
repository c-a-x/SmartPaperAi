package com.GeekPaperAssistant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档处理消息
 * 
 * @author 席崇援
 * @since 2025-10-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 任务ID（用于跟踪进度）
     */
    private Long taskId;
    
    /**
     * 处理类型: parse, vectorize, index
     */
    private String processingType;
    
    /**
     * 额外参数
     */
    private String metadata;
}

