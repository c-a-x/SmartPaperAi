package com.GeekPaperAssistant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.GeekPaperAssistant.config.properties.RAGProperties;
import com.GeekPaperAssistant.mapper.DocumentChunkMapper;
import com.GeekPaperAssistant.mapper.DocumentMapper;
import com.GeekPaperAssistant.mapper.KnowledgeBaseMapper;
import com.GeekPaperAssistant.model.entity.DocumentChunkDO;
import com.GeekPaperAssistant.model.entity.KnowledgeBaseDO;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import com.GeekPaperAssistant.model.vo.CitationVO;
import com.GeekPaperAssistant.model.vo.RagChatResultVO;
import com.GeekPaperAssistant.service.CitationBuilderService;
import com.GeekPaperAssistant.service.DocumentESService;
import com.GeekPaperAssistant.service.DynamicRetrievalService;
import com.GeekPaperAssistant.service.RAGService;
import com.GeekPaperAssistant.service.RerankService;
import com.GeekPaperAssistant.utils.NumberConversionUtils;
import com.GeekPaperAssistant.utils.RAGUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 服务实现类 - 核心对话功能
 * 
 * 说明：
 * - 本类专注于 RAG 问答和文档问答功能
 * - 论文分析功能已迁移至 {@link PaperAnalysisServiceImpl}
 * - 教学设计功能已迁移至 {@link TeachingPlanServiceImpl}
 *
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {

    private final VectorStore vectorStore;
    private final RAGProperties ragConfig;
    private final ChatMemory chatMemoryRepository;
    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DynamicRetrievalService dynamicRetrievalService;
    private final DocumentESService documentESService;
    private final RerankService rerankService;
    private final ChatClient ragChatClient;
    private final CitationBuilderService citationBuilderService;

    // 注入提示词模板
    @Qualifier("ragQAPromptTemplate")
    private final String ragQAPromptTemplate;
    @Qualifier("documentQAPromptTemplate")
    private final String documentQAPromptTemplate;

    @Override
    public RagChatResultVO ragChat(String conversationId, String query) {
        log.info("RAG问答: conversationId={}, query={}", conversationId, query);

        // 获取当前用户ID（拦截器已确保用户已登录）
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 构建基础过滤条件 - 使用 RAGUtils 统一构建
        Filter.Expression filterExpression = RAGUtils.buildUserIdFilter(userId);

        // 2. 构建检索请求（动态调整检索参数）
        SearchRequest baseRequest = SearchRequest.builder()
                .query(query)
                .similarityThreshold(ragConfig.getRetrieval().getSimilarityThreshold())
                .topK(ragConfig.getRetrieval().getTopK())
                .filterExpression(filterExpression)
                .build();

        // 动态调整检索参数（根据问题类型优化）
        SearchRequest adjustedRequest = dynamicRetrievalService.adjustSearchRequest(query, baseRequest);
        log.info("动态检索调整: topK {} -> {}, threshold {} -> {}",
                baseRequest.getTopK(), adjustedRequest.getTopK(),
                baseRequest.getSimilarityThreshold(), adjustedRequest.getSimilarityThreshold());

        // 3. 执行检索（根据配置选择检索策略）
        List<Document> retrievedDocs;
        
        if (ragConfig.getRetrieval().getEnableHybridSearch()) {
            // ✅ 启用混合检索：ES BM25 快速召回 + 向量精排
            log.info("启用混合检索: ES BM25 召回 + 向量精排");
            
            // 根据是否启用重排序，决定向量精排的严格程度
            boolean useStrictReranking = ragConfig.getRetrieval().getEnableReranking();
            retrievedDocs = performHybridSearch(userId, null, query, adjustedRequest, useStrictReranking);
            
        } else if (ragConfig.getRetrieval().getEnableReranking()) {
            // 纯向量检索 + 重排序（扩大检索量后精排）
            log.info("启用向量检索重排序: 扩大检索量 -> 精排");
            retrievedDocs = performVectorSearchWithReranking(adjustedRequest);
            
        } else {
            // 使用默认的纯向量检索
            log.debug("使用默认纯向量检索");
            retrievedDocs = vectorStore.similaritySearch(adjustedRequest);
            log.info("向量检索完成: 检索到 {} 个文档片段", retrievedDocs.size());
        }
        
        // 4. 手动构建上下文并注入到 Prompt 中
        String context = buildContext(retrievedDocs);
        log.info("构建上下文: {} 个文档片段, 上下文长度: {} 字符", retrievedDocs.size(), context.length());

        // 5. 使用提示词模板构建用户提示（注入检索到的上下文）
        String userPrompt = ragQAPromptTemplate
                .replace("{question}", query)
                .replace("{context}", context);

        // 构建包含历史对话的消息列表
        List<Message> messages = new ArrayList<>();

        // 如果 conversationId 不为空，加载历史对话
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            try {
                messages.addAll(chatMemoryRepository.get(conversationId));
                log.debug("RAG问答 - 加载历史消息: conversationId={}, historyCount={}",
                        conversationId, messages.size());
            } catch (Exception e) {
                log.warn("加载RAG对话历史失败: conversationId={}", conversationId, e);
            }
        }

        // 添加当前用户消息
        UserMessage userMessage = new UserMessage(userPrompt);
        messages.add(userMessage);

        // ✅ 直接使用已构建的上下文，不使用 QuestionAnswerAdvisor（避免重复检索）
        String response = ragChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        // 保存对话历史到记忆库
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            try {
                AssistantMessage assistantMessage = new AssistantMessage(response);
                chatMemoryRepository.add(conversationId, List.of(userMessage, assistantMessage));
                log.debug("RAG问答 - 保存到记忆库: conversationId={}", conversationId);
            } catch (Exception e) {
                log.warn("保存RAG对话历史失败: conversationId={}", conversationId, e);
            }
        }

        // 6. 构建引用列表（使用已检索的文档）
        List<CitationVO> citations = citationBuilderService.buildCitations(retrievedDocs, query, 300);

        log.info("RAG问答完成: conversationId={}, responseLength={}, citations={}", 
                conversationId, response.length(), citations.size());
        return RagChatResultVO.builder()
                .answer(response)
                .citations(citations)
                .build();
    }

    @Override
    public RagChatResultVO documentChat(Long documentId, String query) {
        log.info("文档问答: documentId={}, query={}", documentId, query);

        // 验证文档权限（拦截器已确保用户已登录）
        Long userId = StpUtil.getLoginIdAsLong();
        validateDocumentAccess(documentId, userId);
        
        // 检查是否存在文档分块
        long chunkCount = documentChunkMapper.selectCount(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId));
        log.info("文档分块数量: documentId={}, chunkCount={}", documentId, chunkCount);
        
        if (chunkCount == 0) {
            log.error("❌ 文档没有分块数据！documentId={}", documentId);
            return RagChatResultVO.builder()
                    .answer("该文档尚未完成处理，无法进行问答。请稍后再试。")
                    .citations(Collections.emptyList())
                    .build();
        }

        // 构建过滤条件 - 使用 RAGUtils 统一构建文档+用户过滤器
        Filter.Expression filterExpression = RAGUtils.buildDocumentUserFilter(documentId, userId);
        log.info("文档问答过滤条件: documentId={}, userId={}", documentId, userId);

        // 构建检索请求（针对指定文档）
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .similarityThreshold(ragConfig.getRetrieval().getSimilarityThreshold())
                .topK(ragConfig.getRetrieval().getTopK())
                .filterExpression(filterExpression)
                .build();
        
        log.info("检索参数: query={}, topK={}, threshold={}", 
                query, searchRequest.getTopK(), searchRequest.getSimilarityThreshold());

        // 执行检索（根据配置选择检索策略）
        List<Document> docRetrievedDocs;
        
        if (ragConfig.getRetrieval().getEnableHybridSearch()) {
            // ✅ 启用混合检索（针对单个文档）
            log.info("文档问答启用混合检索: documentId={}", documentId);
            boolean useStrictReranking = ragConfig.getRetrieval().getEnableReranking();
            docRetrievedDocs = performHybridSearch(userId, documentId, query, searchRequest, useStrictReranking);
            
        } else if (ragConfig.getRetrieval().getEnableReranking()) {
            // 纯向量检索 + 重排序
            log.info("文档问答启用重排序: documentId={}", documentId);
            docRetrievedDocs = performVectorSearchWithReranking(searchRequest);
        } else {
            // 使用默认的纯向量检索
            docRetrievedDocs = vectorStore.similaritySearch(searchRequest);
        }
        
        log.info("文档问答检索完成: documentId={}, 检索到 {} 个片段", documentId, docRetrievedDocs.size());
        
        // 构建上下文
        String context = buildContext(docRetrievedDocs);
        log.info("构建上下文: {} 个文档片段, 上下文长度: {} 字符", docRetrievedDocs.size(), context.length());

        // 使用提示词模板构建用户提示（注入上下文）
        String userPrompt = documentQAPromptTemplate
                .replace("{question}", query)
                .replace("{context}", context);
        
        // ✅ 直接使用已构建的上下文，不使用 QuestionAnswerAdvisor
        String response = ragChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
        
        // 构建引用列表
        List<CitationVO> citations = citationBuilderService.buildCitations(docRetrievedDocs, query, 300);
        return RagChatResultVO.builder()
                .answer(response)
                .citations(citations)
                .build();
    }

    @Override
    public RagChatResultVO knowledgeBaseChat(Long knowledgeBaseId, String query) {
        log.info("知识库问答: knowledgeBaseId={}, query={}", knowledgeBaseId, query);
        
        // 获取当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 验证知识库访问权限
        validateKnowledgeBaseAccess(knowledgeBaseId, userId);
        
        List<DocumentDO> kbDocuments = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>()
                        .eq(DocumentDO::getKbId, knowledgeBaseId)
                        .eq(DocumentDO::getUserId, userId)
                        .eq(DocumentDO::getStatus, "completed"));
        
        if (kbDocuments.isEmpty()) {
            log.warn("知识库为空: knowledgeBaseId={}", knowledgeBaseId);
            return RagChatResultVO.builder()
                    .answer("该知识库暂无可用文档，无法进行问答。")
                    .citations(Collections.emptyList())
                    .build();
        }
        
        // 构建文档ID列表
        List<Long> documentIds = kbDocuments.stream()
                .map(DocumentDO::getId)
                .collect(Collectors.toList());

        log.info("知识库包含文档: knowledgeBaseId={}, documentCount={}, documentIds={}",
                knowledgeBaseId, documentIds.size(), documentIds);

        // 使用 RAGUtils 构建多文档过滤条件（userId + documentId OR）
        Filter.Expression filterExpression = RAGUtils.buildMultiDocumentFilter(documentIds, userId);
        log.debug("知识库问答过滤条件: {}", filterExpression);
        
        // 2. 构建检索请求
        SearchRequest baseRequest = SearchRequest.builder()
                .query(query)
                .similarityThreshold(ragConfig.getRetrieval().getSimilarityThreshold())
                .topK(ragConfig.getRetrieval().getTopK())
                .filterExpression(filterExpression)
                .build();
        
        // 动态调整检索参数
        SearchRequest adjustedRequest = dynamicRetrievalService.adjustSearchRequest(query, baseRequest);
        
        log.info("知识库问答使用向量检索(无重排序): knowledgeBaseId={}", knowledgeBaseId);
        List<Document> retrievedDocs = vectorStore.similaritySearch(adjustedRequest);
        
        log.info("知识库问答检索完成: knowledgeBaseId={}, 检索到 {} 个片段", knowledgeBaseId, retrievedDocs.size());
        
        // 4. 构建上下文
        String context = buildContext(retrievedDocs);
        log.info("构建上下文: {} 个文档片段, 上下文长度: {} 字符", retrievedDocs.size(), context.length());

        // 5. 构建提示词
        String userPrompt = documentQAPromptTemplate
                .replace("{question}", query)
                .replace("{context}", context);
        
        // 6. ✅ 直接使用已构建的上下文，不使用 QuestionAnswerAdvisor
        String response = ragChatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
        
        // 7. 构建引用列表
        List<CitationVO> citations = citationBuilderService.buildCitations(retrievedDocs, query, 300);
        
        log.info("知识库问答完成: knowledgeBaseId={}, responseLength={}, citations={}", 
                knowledgeBaseId, response.length(), citations.size());
        
        return RagChatResultVO.builder()
                .answer(response)
                .citations(citations)
                .build();
    }

    /**
     * 混合检索：ES BM25 快速召回 + 向量精排 + LLM重排序
     * 三阶段策略：
     * 1. ES 召回大量候选（BM25 关键词匹配，快速）
     * 2. 向量精排（语义相似度，中等规模）
     * 3. LLM 重排序（精准评分，小规模）
     * 
     * @param userId 用户ID
     * @param documentId 文档ID(可选,为null时搜索所有文档)
     * @param query 查询文本
     * @param vectorRequest 向量检索请求
     * @param useStrictReranking 是否启用 LLM 重排序
     * @return 精排后的文档列表
     */
    private List<Document> performHybridSearch(Long userId, Long documentId, String query, SearchRequest vectorRequest, boolean useStrictReranking) {
        // 第一阶段：ES 快速召回
        int esTopK = ragConfig.getHybridSearch().getVectorTopK() * 
                    (useStrictReranking ? ragConfig.getRetrieval().getRerankExpandFactor() : 2);
        
        // ✅ 支持按文档ID过滤
        var esResults = documentESService.fullTextSearchWithHighlight(userId, documentId, query, esTopK);
        
        if (esResults.isEmpty()) {
            log.warn("ES 未召回文档，降级为纯向量检索");
            return vectorStore.similaritySearch(vectorRequest);
        }
        
        log.info("阶段1-ES召回: {} 个文档", esResults.size());
        
        // 第二阶段：向量精排（保留原始过滤条件）
        double threshold = useStrictReranking ? 
                ragConfig.getHybridSearch().getVectorSimilarityThreshold() : 
                vectorRequest.getSimilarityThreshold();
        
        int vectorTopK = useStrictReranking ? 
                vectorRequest.getTopK() * ragConfig.getRetrieval().getRerankExpandFactor() : 
                vectorRequest.getTopK();
        
        // ✅ 保留原始的 filterExpression，确保文档过滤生效
        SearchRequest refinedRequest = SearchRequest.builder()
                .query(vectorRequest.getQuery())
                .topK(vectorTopK)
                .similarityThreshold(threshold)
                .filterExpression(vectorRequest.getFilterExpression())  // 保留过滤条件
                .build();
        List<Document> vectorResults = vectorStore.similaritySearch(refinedRequest);
        
        // 第三阶段：LLM 重排序（可选）
        if (useStrictReranking && vectorResults.size() > vectorRequest.getTopK()) {
            return performLLMReranking(query, vectorResults, vectorRequest.getTopK() * 2); // 增加返回文档数量
        }
        
        return vectorResults.stream()
                .limit(vectorRequest.getTopK() * 2) // 增加返回文档数量
                .collect(Collectors.toList());
    }
    
    /**
     * 纯向量检索 + LLM 重排序
     * 
     * @param baseRequest 基础检索请求
     * @return 重排序后的文档列表
     */
    private List<Document> performVectorSearchWithReranking(SearchRequest baseRequest) {
        int expandedTopK = baseRequest.getTopK() * ragConfig.getRetrieval().getRerankExpandFactor();
        
        log.info("向量检索+重排序: 初始检索={}, 最终topK={}, threshold={}", 
                expandedTopK, baseRequest.getTopK(), baseRequest.getSimilarityThreshold());
        
        // 第一阶段：扩大检索
        SearchRequest expandedRequest = SearchRequest.builder()
                .query(baseRequest.getQuery())
                .topK(expandedTopK)
                .similarityThreshold(baseRequest.getSimilarityThreshold())
                .filterExpression(baseRequest.getFilterExpression())
                .build();
        
        log.debug("开始向量检索: query={}, filter={}", 
                expandedRequest.getQuery(), expandedRequest.getFilterExpression());
        
        List<Document> candidates = vectorStore.similaritySearch(expandedRequest);
        log.info("初始检索: {} 个候选文档", candidates.size());
        // 第二阶段：LLM 重排序
        if (candidates.size() > baseRequest.getTopK()) {
            return performLLMReranking(baseRequest.getQuery(), candidates, baseRequest.getTopK() * 2); // 增加返回文档数量
        }
        
        return candidates.stream()
                .limit(baseRequest.getTopK() * 2) // 增加返回文档数量
                .collect(Collectors.toList());
    }
    
    /**
     * 使用 RerankService 进行 LLM 重排序
     */
    private List<Document> performLLMReranking(String query, List<Document> candidates, int topK) {
        // 构建候选 Map: chunkId -> content
        Map<Long, String> candidateMap = new LinkedHashMap<>();
        Map<Long, Document> docMap = new HashMap<>();
        
        for (Document doc : candidates) {
            Object chunkIdObj = doc.getMetadata().get("chunkIndex");
            if (chunkIdObj != null) {
                Long chunkId = NumberConversionUtils.toLong(String.valueOf(chunkIdObj));
                candidateMap.put(chunkId, doc.getText());
                docMap.put(chunkId, doc);
            }
        }
        
        if (candidateMap.isEmpty()) {
            log.warn("无有效 chunkId，跳过 LLM 重排序");
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }
        
        // 调用 RerankService 重排序
        List<Long> rankedChunkIds = rerankService.rerank(query, candidateMap, topK);
        
        // 转换回 Document 列表
        List<Document> rerankedDocs = rankedChunkIds.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // ✅ 文档多样性过滤：确保结果来自不同文档
        List<Document> diverseDocs = ensureDocumentDiversity(rerankedDocs, topK);
        
        log.info("LLM重排序完成: {} -> {} 个文档 (多样性过滤后: {})", 
                candidates.size(), rerankedDocs.size(), diverseDocs.size());
        
        return diverseDocs;
    }
    
    /**
     * 确保文档多样性 - 限制每个文档的最大片段数
     * 
     * 策略：
     * 1. 优先选择来自不同文档的片段
     * 2. 每个文档最多贡献 maxPerDoc 个片段
     * 3. 按相似度排序保持质量
     * 
     * @param documents 已排序的文档列表
     * @param targetCount 目标数量
     * @return 多样性过滤后的文档列表
     */
    private List<Document> ensureDocumentDiversity(List<Document> documents, int targetCount) {
        // 增加目标数量以返回更多文档
        int increasedTargetCount = targetCount * 2;
        
        if (documents.size() <= increasedTargetCount) {
            return documents;
        }
        
        // 每个文档最多贡献的片段数（根据目标数量动态调整）
        int maxPerDoc = Math.max(2, increasedTargetCount / 3);  // 至少2个，最多targetCount/3个
        
        List<Document> result = new ArrayList<>();
        Map<String, Integer> docCountMap = new HashMap<>();
        
        for (Document doc : documents) {
            if (result.size() >= increasedTargetCount) {
                break;
            }
            
            // 获取文档ID
            String documentId = String.valueOf(doc.getMetadata().get("documentId"));
            int currentCount = docCountMap.getOrDefault(documentId, 0);
            
            // 如果该文档的片段数未超过限制，则添加
            if (currentCount < maxPerDoc) {
                result.add(doc);
                docCountMap.put(documentId, currentCount + 1);
            }
        }
        
        // 如果多样性过滤后数量不足，补充剩余的高分片段
        if (result.size() < increasedTargetCount) {
            for (Document doc : documents) {
                if (result.size() >= increasedTargetCount) {
                    break;
                }
                if (!result.contains(doc)) {
                    result.add(doc);
                }
            }
        }
        
        log.debug("文档多样性过滤: {} -> {}, 来自 {} 个不同文档", 
                documents.size(), result.size(), docCountMap.size());
        
        return result;
    }

    

    
    /**
     * 构建上下文字符串
     * 将检索到的文档片段合并为一个上下文字符串
     */
    private String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "未找到相关文档。";
        }
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("以下是检索到的相关文档片段：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            contextBuilder.append(String.format("[文档片段 %d]\n", i + 1));
            contextBuilder.append(doc.getText());
            contextBuilder.append("\n\n");
        }
        
        return contextBuilder.toString();
    }

    /**
     * 验证文档访问权限
     * 
     * @return 验证通过后的文档对象
     */
    private DocumentDO validateDocumentAccess(Long documentId, Long userId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该文档");
        }
        return document;
    }
    
    /**
     * 验证知识库访问权限
     */
    private void validateKnowledgeBaseAccess(Long knowledgeBaseId, Long userId) {
        KnowledgeBaseDO knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该知识库");
        }
        // 检查知识库是否被逻辑删除
        if (knowledgeBase.getIsDeleted() != null && knowledgeBase.getIsDeleted() == 1) {
            throw new BusinessException("知识库已被删除");
        }
    }
}

    
