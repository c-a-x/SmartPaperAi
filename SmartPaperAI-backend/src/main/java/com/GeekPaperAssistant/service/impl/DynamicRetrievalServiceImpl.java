package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.service.DynamicRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 动态检索策略服务实现
 * 基于问题长度、类型动态调整 topK 和相似度阈值
 * 
 * @author 席崇援
 */
@Slf4j
@Service
public class DynamicRetrievalServiceImpl implements DynamicRetrievalService {
    
    // 问题类型关键词模式
    private static final Pattern FACT_PATTERN = Pattern.compile(".*?(是什么|什么是|定义|含义|概念).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPARISON_PATTERN = Pattern.compile(".*?(对比|比较|区别|差异|相同|不同).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(".*?(总结|概括|归纳|综述|梳理).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOW_TO_PATTERN = Pattern.compile(".*?(如何|怎么|怎样|方法|步骤).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHY_PATTERN = Pattern.compile(".*?(为什么|原因|为何).*", Pattern.CASE_INSENSITIVE);
    
    @Override
    public SearchRequest adjustSearchRequest(String query, SearchRequest baseRequest) {
        if (query == null || query.trim().isEmpty()) {
            return baseRequest;
        }
        
        String queryType = analyzeQueryType(query);
        int optimalTopK = calculateOptimalTopK(query);
        double optimalThreshold = calculateOptimalThreshold(query, queryType);
        
        log.debug("Dynamic retrieval adjustment - Query: '{}', Type: {}, TopK: {} -> {}, Threshold: {} -> {}", 
            query.substring(0, Math.min(50, query.length())),
            queryType, 
            baseRequest.getTopK(), optimalTopK,
            baseRequest.getSimilarityThreshold(), optimalThreshold);
        
        return SearchRequest.builder()
            .query(baseRequest.getQuery())
            .topK(optimalTopK)
            .similarityThreshold(optimalThreshold)
            .filterExpression(baseRequest.getFilterExpression())
            .build();
    }
    
    @Override
    public String analyzeQueryType(String query) {
        if (FACT_PATTERN.matcher(query).matches()) {
            return "fact";
        } else if (COMPARISON_PATTERN.matcher(query).matches()) {
            return "comparison";
        } else if (SUMMARY_PATTERN.matcher(query).matches()) {
            return "summary";
        } else if (HOW_TO_PATTERN.matcher(query).matches()) {
            return "how-to";
        } else if (WHY_PATTERN.matcher(query).matches()) {
            return "why";
        } else {
            return "general";
        }
    }
    
    @Override
    public int calculateOptimalTopK(String query) {
        int length = query.length();
        
        // 短问题（<20字）- 较少文档
        if (length < 20) {
            return 5;
        }
        // 中等问题（20-50字）- 中等数量
        else if (length < 50) {
            return 8;
        }
        // 长问题（50-100字）- 更多文档
        else if (length < 100) {
            return 12;
        }
        // 超长问题（>100字）- 最多文档
        else {
            return 15;
        }
    }
    
    @Override
    public double calculateOptimalThreshold(String query, String queryType) {
        // 根据问题类型调整阈值
        switch (queryType) {
            case "fact":
                // 事实性问题要求高相关性
                return 0.5;  // 降低阈值：0.65 -> 0.5
            
            case "comparison":
                // 对比问题需要多样性，降低阈值
                return 0.4;  // 降低阈值：0.50 -> 0.4
            
            case "summary":
                // 总结问题需要广泛覆盖
                return 0.35;  // 降低阈值：0.45 -> 0.35
            
            case "how-to":
                // 方法类问题需要精确匹配
                return 0.45;  // 降低阈值：0.60 -> 0.45
            
            case "why":
                // 原因分析需要相关性
                return 0.45;  // 降低阈值：0.58 -> 0.45
            
            default:
                // 通用问题（大幅降低阈值以提高召回率）
                return 0.2;  // 降低阈值：0.3 -> 0.2
        }
    }
}
