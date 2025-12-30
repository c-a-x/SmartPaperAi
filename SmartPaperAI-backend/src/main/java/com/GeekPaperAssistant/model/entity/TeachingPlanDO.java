package com.GeekPaperAssistant.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学设计实体
 * 
 * @author 席崇援
 * @since 2025-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "teaching_plans", autoResultMap = true)
public class TeachingPlanDO {
    
    /**
     * 教案ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 创建用户ID
     */
    private Long userId;
    
    /**
     * 课题名称
     */
    private String title;
    
    /**
     * 学段
     */
    private String grade;
    
    /**
     * 学科
     */
    private String subject;
    
    /**
     * 教案内容(结构化 JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String content;
    
    /**
     * 参考论文ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> sourcePapers;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否删除
     */
    private Integer isDeleted;
}
