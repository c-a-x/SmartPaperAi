package com.GeekPaperAssistant.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.LongNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 索引初始化器
 * 应用启动时自动创建 ican_documents 索引(如果不存在)
 * 
 * @author 席崇援
 * @since 2025-10-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer implements CommandLineRunner {
    
    private final ElasticsearchClient elasticsearchClient;
    
    private static final String INDEX_NAME = "ican_documents";
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // 检查索引是否存在
            BooleanResponse exists = elasticsearchClient.indices().exists(
                ExistsRequest.of(e -> e.index(INDEX_NAME))
            );
            
            if (exists.value()) {
                log.info("Elasticsearch 索引已存在: {}", INDEX_NAME);
                return;
            }
            
            // 创建索引
            createIndex();
            log.info("Elasticsearch 索引创建成功: {}", INDEX_NAME);
            
        } catch (Exception e) {
            log.error("Elasticsearch 索引初始化失败: {}", INDEX_NAME, e);
            // 不抛出异常,允许应用继续启动
        }
    }
    
    /**
     * 创建索引并设置映射
     */
    private void createIndex() throws Exception {
        // 构建字段映射
        Map<String, Property> properties = new HashMap<>();
        
        // userId: long
        properties.put("userId", Property.of(p -> p.long_(LongNumberProperty.of(l -> l))));
        
        // title: text (分词,用于全文搜索)
        properties.put("title", Property.of(p -> p.text(TextProperty.of(t -> t
            .analyzer("standard")
        ))));
        
        // content: text (分词,用于全文搜索)
        properties.put("content", Property.of(p -> p.text(TextProperty.of(t -> t
            .analyzer("standard")
        ))));
        
        // type: keyword (不分词,用于精确匹配和聚合)
        properties.put("type", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
        
        // fileSize: long
        properties.put("fileSize", Property.of(p -> p.long_(LongNumberProperty.of(l -> l))));
        
        // status: keyword
        properties.put("status", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
        
        // createTime: date
        properties.put("createTime", Property.of(p -> p.date(DateProperty.of(d -> d))));
        
        // updateTime: date
        properties.put("updateTime", Property.of(p -> p.date(DateProperty.of(d -> d))));
        
        // 创建索引
        elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
            .index(INDEX_NAME)
            .settings(s -> s
                .numberOfShards("1")
                .numberOfReplicas("0")
            )
            .mappings(TypeMapping.of(m -> m
                .properties(properties)
            ))
        ));
    }
}
