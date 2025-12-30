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
 * 标签实体
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tag")
public class TagDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String color;
    
    private Long userId;
    
    private LocalDateTime createTime;
}
