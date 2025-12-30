package com.GeekPaperAssistant.service;

import org.springframework.ai.vectorstore.SearchRequest;

/**
 * 动态检索策略服务
 * 
 * @author 席崇援
 */
public interface DynamicRetrievalService {
    
    /**
     * 根据问题特征动态调整检索参数
     * 
     * @param query 查询文本
     * @param baseRequest 基础搜索请求
     * @return 调整后的搜索请求
     */
    SearchRequest adjustSearchRequest(String query, SearchRequest baseRequest);
    
    /**
     * 分析问题类型
     * 
     * @param query 查询文本
     * @return 问题类型（fact, concept, comparison, summary, etc.）
     */
    String analyzeQueryType(String query);
    
    /**
     * 计算推荐的 topK 值
     * 
     * @param query 查询文本
     * @return 推荐的 topK
     */
    int calculateOptimalTopK(String query);
    
    /**
     * 计算推荐的相似度阈值
     * 
     * @param query 查询文本
     * @param queryType 问题类型
     * @return 推荐的阈值
     */
    double calculateOptimalThreshold(String query, String queryType);
}
