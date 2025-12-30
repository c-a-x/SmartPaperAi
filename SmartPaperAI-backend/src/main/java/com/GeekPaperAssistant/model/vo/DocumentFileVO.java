package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档文件 VO
 * 用于文档预览和下载
 * 
 * @author 席崇援
 * @since 2025-10-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileVO {
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 内容类型 (MIME Type)
     */
    private String contentType;
    
    /**
     * 文件数据
     */
    private byte[] data;
}
