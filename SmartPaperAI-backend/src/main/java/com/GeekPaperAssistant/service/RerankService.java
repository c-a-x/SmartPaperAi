package com.GeekPaperAssistant.service;

import java.util.List;
import java.util.Map;

/**
 * Rerank 服务接口
 * 
 * @author 席崇援
 */
public interface RerankService {
    
    /**
     * 对候选文档进行重排序
     * 
     * @param query 查询问题
     * @param candidates 候选文档列表(chunkId -> content)
     * @param topK 返回前K个结果
     * @return 排序后的文档ID列表
     */
    List<Long> rerank(String query, Map<Long, String> candidates, int topK);
    
    /**
     * 计算查询和文档的相关性分数
     * 
     * @param query 查询问题
     * @param document 文档内容
     * @return 相关性分数 (0-1)
     */
    double computeRelevanceScore(String query, String document);
}
