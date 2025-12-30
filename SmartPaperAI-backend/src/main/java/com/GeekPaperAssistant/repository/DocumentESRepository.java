package com.GeekPaperAssistant.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import com.GeekPaperAssistant.model.entity.DocumentES;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Elasticsearch 文档仓库
 * 使用原生 Elasticsearch Java API 8.15.0
 * 
 * @author 席崇援
 * @since 2025-10-08
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DocumentESRepository {
    
    private final ElasticsearchClient elasticsearchClient;
    
    private static final String INDEX_NAME = "ican_documents";
    
    /**
     * 保存文档到 Elasticsearch
     */
    public DocumentES save(DocumentES document) {
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(document.getId().toString())
                .document(document)
            );
            
            log.debug("文档已索引: id={}, result={}", document.getId(), response.result());
            return document;
        } catch (IOException e) {
            log.error("保存文档到ES失败: id={}", document.getId(), e);
            throw new RuntimeException("保存文档失败", e);
        }
    }
    
    /**
     * 根据ID查询文档
     */
    public Optional<DocumentES> findById(Long id) {
        try {
            GetResponse<DocumentES> response = elasticsearchClient.get(g -> g
                .index(INDEX_NAME)
                .id(id.toString()),
                DocumentES.class
            );
            
            if (response.found()) {
                DocumentES doc = response.source();
                if (doc != null) {
                    doc.setId(id);  // 确保ID被设置
                }
                return Optional.ofNullable(doc);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("根据ID查询文档失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据用户ID查询文档
     */
    public List<DocumentES> findByUserId(Long userId) {
        try {
            SearchResponse<DocumentES> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .term(t -> t
                        .field("userId")
                        .value(userId)
                    )
                )
                .size(1000),  // 最多返回1000条
                DocumentES.class
            );
            
            return response.hits().hits().stream()
                .map(hit -> {
                    DocumentES doc = hit.source();
                    if (doc != null && hit.id() != null) {
                        doc.setId(Long.parseLong(hit.id()));
                    }
                    return doc;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("根据用户ID查询文档失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据用户ID和标题模糊查询
     */
    public List<DocumentES> findByUserIdAndTitleContaining(Long userId, String title) {
        try {
            SearchResponse<DocumentES> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.match(t -> t.field("title").query(title)))
                    )
                )
                .size(100),
                DocumentES.class
            );
            
            return response.hits().hits().stream()
                .map(hit -> {
                    DocumentES doc = hit.source();
                    if (doc != null && hit.id() != null) {
                        doc.setId(Long.parseLong(hit.id()));
                    }
                    return doc;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("根据标题查询文档失败: userId={}, title={}", userId, title, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据用户ID和内容模糊查询
     */
    public List<DocumentES> findByUserIdAndContentContaining(Long userId, String content) {
        try {
            SearchResponse<DocumentES> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.match(t -> t.field("content").query(content)))
                    )
                )
                .size(100),
                DocumentES.class
            );
            
            return response.hits().hits().stream()
                .map(hit -> {
                    DocumentES doc = hit.source();
                    if (doc != null && hit.id() != null) {
                        doc.setId(Long.parseLong(hit.id()));
                    }
                    return doc;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("根据内容查询文档失败: userId={}, content={}", userId, content, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 删除文档
     */
    public void deleteById(Long id) {
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d
                .index(INDEX_NAME)
                .id(id.toString())
            );
            log.debug("删除文档: id={}, result={}", id, response.result());
        } catch (IOException e) {
            log.error("删除文档失败: id={}", id, e);
            throw new RuntimeException("删除文档失败", e);
        }
    }
    
    /**
     * 全文搜索 - 在标题和内容中搜索
     */
    public List<DocumentES> fullTextSearch(Long userId, String query, int size) {
        try {
            SearchResponse<DocumentES> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                            .query(query)
                            .fields("title", "content")
                        ))
                    )
                )
                .size(size),
                DocumentES.class
            );
            
            return response.hits().hits().stream()
                .map(hit -> {
                    DocumentES doc = hit.source();
                    if (doc != null && hit.id() != null) {
                        doc.setId(Long.parseLong(hit.id()));
                    }
                    return doc;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("全文搜索失败: userId={}, query={}", userId, query, e);
            return new ArrayList<>();
        }
    }
}
