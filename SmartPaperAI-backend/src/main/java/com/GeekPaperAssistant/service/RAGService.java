package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.RagChatResultVO;

/**
 * RAG 服务接口 - 核心对话功能
 * 
 * 说明：
 * - 论文分析相关功能已迁移至 {@link PaperAnalysisService}
 * - 教学设计相关功能已迁移至 {@link TeachingPlanService}
 * 
 * @author 席崇援
 */
public interface RAGService {
    
    /**
     * RAG 问答（带引用）
     * 基于当前用户的所有文档进行向量检索，生成带引用的回答
     * 
     * @param conversationId 会话ID
     * @param query 用户问题
     * @return AI 回答和引用列表
     */
    RagChatResultVO ragChat(String conversationId, String query);
    
    /**
     * 基于文档的问答（带引用）
     * 限定在单一文档的上下文中进行问答
     * 
     * @param documentId 文档ID
     * @param query 用户问题
     * @return AI 回答和引用列表
     */
    RagChatResultVO documentChat(Long documentId, String query);
    
    /**
     * 基于知识库的问答（带引用）
     * 在指定知识库的所有文档中进行检索和问答
     * 
     * @param knowledgeBaseId 知识库ID
     * @param query 用户问题
     * @return AI 回答和引用列表
     */
    RagChatResultVO knowledgeBaseChat(Long knowledgeBaseId, String query);
}

