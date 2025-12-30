package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教学设计列表 VO
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教学设计列表响应")
public class TeachingPlanListVO {
    
    @Schema(description = "教案ID")
    private Long id;
    
    @Schema(description = "课题名称")
    private String title;
    
    @Schema(description = "学段")
    private String grade;
    
    @Schema(description = "学科")
    private String subject;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
