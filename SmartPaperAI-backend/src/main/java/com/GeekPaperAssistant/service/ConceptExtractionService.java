package com.GeekPaperAssistant.service;

import java.util.List;

/**
 * 概念提取服务接口
 * 从用户查询中提取关键概念，用于知识图谱增强检索
 *
 * @author Claude
 * @since 2025-10-29
 */
public interface ConceptExtractionService {

    /**
     * 从用户查询中提取关键概念
     *
     * @param query 用户查询文本
     * @return 提取的概念列表（3-5个关键概念）
     */
    List<String> extractConcepts(String query);

    /**
     * 从用户查询中提取关键概念（带缓存）
     *
     * @param query 用户查询文本
     * @param maxConcepts 最大概念数量
     * @return 提取的概念列表
     */
    List<String> extractConcepts(String query, int maxConcepts);
}
