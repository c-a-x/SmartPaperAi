package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.GeekPaperAssistant.model.vo.KnowledgeGraphVO;
import com.GeekPaperAssistant.model.vo.RagChatResultVO;
import com.GeekPaperAssistant.service.RAGService;
import com.GeekPaperAssistant.service.GraphEnhancedRAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 对话控制器
 * 
 * <p>专注于 RAG 对话功能，支持知识图谱增强检索</p>
 * <p>论文分析相关接口已迁移至 DocumentController</p>
 * 
 * @author 席崇援
 * @since 2025-10-06
 */
@Slf4j
@Tag(name = "RAG 对话", description = "检索增强生成对话功能，支持知识图谱增强")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@SaCheckLogin
public class RAGController {
    
    private final RAGService ragService;   
    private final GraphEnhancedRAGService graphEnhancedRAGService;
    /**
     * 文档问答（带引用）
     *
     * <p>限定单一文档上下文进行问答，结果附带引用片段</p>
     */
    @Operation(summary = "文档问答", description = "限定单一文档上下文进行问答，结果附带引用片段")
    @PostMapping("/document-chat")
    public RagChatResultVO documentChat(
            @Parameter(description = "文档ID") @RequestParam Long documentId,
            @Parameter(description = "用户问题") @RequestParam String query) {
        return ragService.documentChat(documentId, query);
    }

    /**
     * 知识库问答（带引用）
     *
     * <p>在整个知识库的所有文档中进行检索问答，结果附带引用片段</p>
     */
    @Operation(summary = "知识库问答", description = "在整个知识库的所有文档中进行检索问答，结果附带引用片段")
    @PostMapping("/knowledge-base-chat")
    public RagChatResultVO knowledgeBaseChat(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeBaseId,
            @Parameter(description = "用户问题") @RequestParam String query) {
        return ragService.knowledgeBaseChat(knowledgeBaseId, query);
    }

    /**
     * 构建文档知识图谱
     *
     * <p>基于文档内容和用户查询，提取相关概念并构建知识图谱可视化数据</p>
     * <p>独立接口，可用于前端单独请求图谱数据</p>
     */
    @Operation(summary = "构建文档知识图谱", description = "基于文档内容和用户查询构建知识图谱，返回可视化数据")
    @GetMapping("/document-knowledge-graph")
    public KnowledgeGraphVO buildDocumentKnowledgeGraph(
            @Parameter(description = "文档ID") @RequestParam Long documentId,
            @Parameter(description = "用户查询（用于提取相关概念）") @RequestParam String query) {
        return graphEnhancedRAGService.buildDocumentKnowledgeGraph(documentId, query);
    }

    /**
     * 构建知识库知识图谱
     *
     * <p>展示整个知识库中的概念网络和文档关系</p>
     * <p>如果提供查询，会筛选相关概念；否则显示所有核心概念</p>
     */
    @Operation(summary = "构建知识库知识图谱", description = "展示知识库的概念网络，支持通过查询筛选相关概念")
    @GetMapping("/knowledge-base-knowledge-graph")
    public KnowledgeGraphVO buildKnowledgeBaseGraph(
            @Parameter(description = "知识库ID") @RequestParam Long knowledgeBaseId,
            @Parameter(description = "用户查询（可选，用于筛选相关概念）") @RequestParam(required = false) String query,
            @Parameter(description = "限制返回的节点数量（默认50）") @RequestParam(defaultValue = "50") int limit) {
        return graphEnhancedRAGService.buildKnowledgeBaseGraph(knowledgeBaseId, query, limit);
    }

    /**
     * 构建全局知识图谱
     *
     * <p>展示用户所有文档的概念网络和文档关系</p>
     * <p>如果提供查询，会筛选相关概念；否则显示最热门的概念</p>
     */
    @Operation(summary = "构建全局知识图谱", description = "展示用户所有文档的概念网络，支持通过查询筛选相关概念")
    @GetMapping("/global-knowledge-graph")
    public KnowledgeGraphVO buildGlobalKnowledgeGraph(
            @Parameter(description = "用户查询（可选，用于筛选相关概念）") @RequestParam(required = false) String query,
            @Parameter(description = "限制返回的节点数量（默认100）") @RequestParam(defaultValue = "100") int limit) {
        return graphEnhancedRAGService.buildGlobalKnowledgeGraph(query, limit);
    }
}

