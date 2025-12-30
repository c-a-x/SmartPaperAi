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
 * 知识库实体
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_base")
public class KnowledgeBaseDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private Long userId;
    
    private Integer documentCount;
    
    private Integer isDeleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
