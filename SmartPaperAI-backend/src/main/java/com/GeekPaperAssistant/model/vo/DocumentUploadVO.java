package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应 VO
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadVO {
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 任务状态 URL
     */
    private String taskStatusUrl;
}
