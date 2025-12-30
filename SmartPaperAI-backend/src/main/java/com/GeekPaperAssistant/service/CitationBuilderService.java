package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.CitationVO;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 引用构建服务 - 统一管理引用信息的构建逻辑
 *
 * <p>消除 RAG、论文分析、教学计划等服务中的重复代码</p>
 *
 * @author ican
 * @since 2025-10-27
 */
public interface CitationBuilderService {

    /**
     * 根据检索文档构建引用列表
     *
     * @param docs 检索到的文档列表（Spring AI Document）
     * @param query 用户查询（用于提取关键词）
     * @param maxSnippetLength 文本片段最大长度
     * @return 引用信息列表
     */
    List<CitationVO> buildCitations(List<Document> docs, String query, int maxSnippetLength);
}
