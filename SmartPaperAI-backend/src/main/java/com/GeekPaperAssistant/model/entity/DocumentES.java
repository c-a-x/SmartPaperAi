package com.GeekPaperAssistant.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Elasticsearch 文档实体
 * 用于全文检索 (使用原生 Elasticsearch Java API)
 * 
 * <p>索引名称: ican_documents</p>
 * <p>索引配置: 1 shard, 0 replicas</p>
 * 
 * @author 席崇援
 * @since 2025-10-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentES {
    
    /**
     * 文档ID（与数据库ID一致）
     * 映射为 ES 的 _id
     */
    @JsonProperty("id")
    private Long id;
    
    /**
     * 用户ID
     * ES 字段类型: long
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 知识库ID
     * ES 字段类型: long
     */
    @JsonProperty("kbId")
    private Long kbId;

    /**
     * 文档标题
     * ES 字段类型: text (使用 standard 分词器)
     */
    @JsonProperty("title")
    private String title;
    
    /**
     * 文档内容（全文）
     * ES 字段类型: text (使用 standard 分词器)
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * 文档类型
     * ES 字段类型: keyword (不分词)
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 文件大小（字节）
     * ES 字段类型: long
     */
    @JsonProperty("fileSize")
    private Long fileSize;
    
    /**
     * 文档状态
     * ES 字段类型: keyword (不分词)
     */
    @JsonProperty("status")
    private String status;
    
    /**
     * 创建时间
     * ES 字段类型: date
     */
    @JsonProperty("createTime")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     * ES 字段类型: date
     */
    @JsonProperty("updateTime")
    private LocalDateTime updateTime;
}
