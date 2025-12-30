package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传结果 VO
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResultVO {
    
    /**
     * 总数
     */
    private Integer total;
    
    /**
     * 成功数
     */
    private Integer success;
    
    /**
     * 失败数
     */
    private Integer failure;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 每个文件的上传结果
     */
    private List<FileUploadResult> results;
    
    /**
     * 单个文件上传结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUploadResult {
        /**
         * 文件名
         */
        private String filename;
        
        /**
         * 是否成功
         */
        private Boolean success;
        
        /**
         * 文档ID
         */
        private Long documentId;
        
        /**
         * 任务ID
         */
        private Long taskId;
        
        /**
         * 标题
         */
        private String title;
        
        /**
         * 消息
         */
        private String message;
        
        /**
         * 任务状态URL
         */
        private String taskStatusUrl;
    }
}
