package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签 VO
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "标签信息")
public class TagVO {
    
    @Schema(description = "标签ID")
    private Long id;
    
    @Schema(description = "标签名称")
    private String name;
    
    @Schema(description = "颜色")
    private String color;
    
    @Schema(description = "使用次数")
    private Integer usageCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
