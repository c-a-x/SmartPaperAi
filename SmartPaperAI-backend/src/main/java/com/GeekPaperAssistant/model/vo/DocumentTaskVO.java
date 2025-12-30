package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档任务状态 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档任务状态")
public class DocumentTaskVO {
    
    @Schema(description = "任务ID")
    private Long id;
    
    @Schema(description = "文档ID")
    private Long documentId;
    
    @Schema(description = "任务类型")
    private String taskType;
    
    @Schema(description = "状态")
    private String status;
    
    @Schema(description = "重试次数")
    private Integer retryCount;
    
    @Schema(description = "最大重试次数")
    private Integer maxRetries;
    
    @Schema(description = "错误信息")
    private String errorMessage;
    
    @Schema(description = "进度(0-100)")
    private Integer progress;
    
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    
    @Schema(description = "耗时(秒)")
    private Long duration;
}
