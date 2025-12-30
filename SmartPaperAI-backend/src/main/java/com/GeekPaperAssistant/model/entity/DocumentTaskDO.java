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
 * 文档处理任务实体
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("document_task")
public class DocumentTaskDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long documentId;
    
    private String taskType; // parse, vectorize, extract_metadata
    
    private String status; // pending, processing, completed, failed
    
    private Integer retryCount;
    
    private Integer maxRetries;
    
    private String errorMessage;
    
    private Integer progress; // 0-100
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
