package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档 VO
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {
    
    /**
     * 文档ID
     */
    private Long id;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档类型
     */
    private String type;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 处理状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

