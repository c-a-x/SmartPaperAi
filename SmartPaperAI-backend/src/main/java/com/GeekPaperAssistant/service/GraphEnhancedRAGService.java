package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.KnowledgeGraphVO;

import java.util.List;
import java.util.Set;

/**
 * 图增强的RAG服务接口 - 基于知识图谱的智能问答
 *
 * @author 席崇援
 * @since 2025-10-16
 */
public interface GraphEnhancedRAGService {


    /**
     * 构建文档相关的知识图谱可视化数据
     * 基于文档内容和用户查询，提取相关概念并构建图谱
     *
     * @param documentId 文档ID
     * @param query 用户查询（用于提取相关概念）
     * @return 知识图谱可视化数据
     */
    KnowledgeGraphVO buildDocumentKnowledgeGraph(Long documentId, String query);

    /**
     * 构建知识库相关的知识图谱可视化数据
     * 展示整个知识库中的概念网络和文档关系
     *
     * @param knowledgeBaseId 知识库ID
     * @param query 用户查询（用于提取相关概念，可选）
     * @param limit 限制返回的节点数量
     * @return 知识图谱可视化数据
     */
    KnowledgeGraphVO buildKnowledgeBaseGraph(Long knowledgeBaseId, String query, int limit);

    /**
     * 构建全局知识图谱可视化数据
     * 展示用户所有文档的概念网络和文档关系
     *
     * @param query 用户查询（用于提取相关概念，可选）
     * @param limit 限制返回的节点数量
     * @return 知识图谱可视化数据
     */
    KnowledgeGraphVO buildGlobalKnowledgeGraph(String query, int limit);

    /**
     * 基于概念网络的文档发现（内部使用）
     * 通过概念关联发现相关文档
     *
     * @param concepts 概念列表
     * @param userId 用户ID
     * @param maxDepth 搜索深度
     * @return 相关文档ID集合
     */
    Set<Long> findDocumentsByConceptNetwork(List<String> concepts, Long userId, int maxDepth);
}