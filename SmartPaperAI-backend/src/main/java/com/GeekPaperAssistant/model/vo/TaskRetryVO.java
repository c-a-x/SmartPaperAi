package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务重试结果 VO
 *
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务重试结果")
public class TaskRetryVO {

    /**
     * 提示信息
     */
    @Schema(description = "提示信息")
    private String message;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    private Long taskId;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;
}
