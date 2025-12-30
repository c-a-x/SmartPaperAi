package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.entity.DocumentES;
import com.GeekPaperAssistant.model.vo.DocumentSearchResultVO;

import java.util.List;

/**
 * Elasticsearch 文档服务接口 - 增强版
 * 
 * 提供基于 Elasticsearch 的高级文档搜索功能:
 * - 全文搜索 (BM25 算法)
 * - 混合搜索 (向量相似度 + BM25)
 * - 高亮片段提取
 * 
 * @author 席崇援
 */
public interface DocumentESService {
    
    /**
     * 索引文档到 Elasticsearch
     * 
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param title 标题
     * @param content 内容
     * @param type 类型
     * @param fileSize 文件大小
     * @param status 状态
     */
    void indexDocument(Long documentId, Long userId, String title, String content, 
                      String type, Long fileSize, String status);
    
    /**
     * 更新文档状态
     * 
     * @param documentId 文档ID
     * @param status 状态
     */
    void updateDocumentStatus(Long documentId, String status);
     
    /**
     * 全文搜索文档 - 支持按文档ID过滤
     * 
     * <p>用于文档问答场景,只在指定文档中搜索</p>
     * 
     * @param userId 用户ID(安全过滤)
     * @param documentId 文档ID(可选,为null时搜索所有文档)
     * @param query 搜索查询
     * @param topK 返回数量
     * @return 搜索结果列表
     */
    List<DocumentSearchResultVO> fullTextSearchWithHighlight(Long userId, Long documentId, String query, int topK);
    
    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     */
    void deleteDocument(Long documentId);
    
    /**
     * 获取用户的所有文档
     * 
     * @param userId 用户ID
     * @return 文档列表
     */
    List<DocumentES> getUserDocuments(Long userId);
}
