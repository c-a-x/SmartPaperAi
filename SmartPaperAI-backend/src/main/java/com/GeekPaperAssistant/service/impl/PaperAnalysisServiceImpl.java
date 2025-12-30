package com.GeekPaperAssistant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.GeekPaperAssistant.config.properties.RAGProperties;
import com.GeekPaperAssistant.mapper.DocumentChunkMapper;
import com.GeekPaperAssistant.mapper.DocumentMapper;
import com.GeekPaperAssistant.model.entity.DocumentChunkDO;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import com.GeekPaperAssistant.model.vo.*;
import com.GeekPaperAssistant.service.CitationBuilderService;
import com.GeekPaperAssistant.service.FileStorageService;
import com.GeekPaperAssistant.service.GrobidMetadataService;
import com.GeekPaperAssistant.service.PaperAnalysisService;
import com.GeekPaperAssistant.utils.LLMJsonUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import cn.hutool.core.codec.Base62;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 论文分析服务实现类
 *
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAnalysisServiceImpl implements PaperAnalysisService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChatClient ragChatClient;
    private final VectorStore vectorStore;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RAGProperties ragConfig;
    private final CitationBuilderService citationBuilderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🆕 GROBID 元数据解析服务
    private final GrobidMetadataService grobidMetadataService;
    // 🆕 文件存储服务(用于获取PDF文件)
    private final FileStorageService fileStorageService;

    // 注入提示词模板
    @Qualifier("paperSummaryPromptTemplate")
    private final String paperSummaryPromptTemplate;
    @Qualifier("paperComparisonPromptTemplate")
    private final String paperComparisonPromptTemplate;
    @Qualifier("paperMetadataExtractionPromptTemplate")
    private final String paperMetadataExtractionPromptTemplate;
    @Qualifier("innovationExtractionPromptTemplate")
    private final String innovationExtractionPromptTemplate;
    @Qualifier("innovationClusteringPromptTemplate")
    private final String innovationClusteringPromptTemplate;
    @Qualifier("literatureReviewPromptTemplate")
    private final String literatureReviewPromptTemplate;

    // Redis 缓存键前缀和过期时间
    private static final String PAPER_METADATA_CACHE_PREFIX = "paper:metadata:";
    private static final long CACHE_EXPIRE_DAYS = 30L;

    @Override
    public RagAnswerVO<PaperSummaryVO> summarizePaper(Long documentId) {
        log.info("论文总结: documentId={}", documentId);

        // 验证文档权限
        Long userId = StpUtil.getLoginIdAsLong();
        validateDocumentAccess(documentId, userId);

        // 获取所有分块
        List<DocumentChunkDO> allChunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                        .eq(DocumentChunkDO::getIsDeleted, 0)
                        .orderByAsc(DocumentChunkDO::getChunkIndex)
        );

        if (allChunks.isEmpty()) {
            throw new BusinessException("文档内容为空，无法生成总结");
        }

        log.info("获取到 {} 个分块用于生成总结", allChunks.size());

        try {
            // 🔥 使用 RAG 方式进行论文总结（取代低效的两阶段批处理）
            log.info("使用 RAG 方式进行论文总结，基于 {} 个分块", allChunks.size());
            
            // 构建过滤条件：只检索当前文档的分块
            Filter.Expression filterExpression = new FilterExpressionBuilder()
                    .eq("documentId", Base62.encode(String.valueOf(documentId)))
                    .build();
            
            // 构建检索请求（使用配置的 topK 值）
            int topK = ragConfig.getRetrieval().getTopK();
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("请总结这篇论文的核心内容、主要创新点、研究方法和实验结果")
                    .similarityThreshold(0.3)  // 降低阈值以获取更多相关内容
                    .topK(topK)  // 使用配置的 topK 值
                    .filterExpression(filterExpression)
                    .build();
            
            // 构建 QuestionAnswerAdvisor
            QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(searchRequest)
                    .build();

            // 增强system message,强调JSON格式和内容完整性
            String systemMessage = """
                你是一位资深的学术论文分析专家，拥有多年的学术审稿和论文写作经验。
                
                核心要求：
                1. 必须返回完整填充的JSON对象，所有字段都要有实质性内容
                2. 不允许返回空字符串("")或空数组([])
                3. 基于论文内容深度分析，每个描述性字段至少200字
                4. 只返回纯JSON，绝对不要markdown标记（如```json）
                5. 如果某些信息在文中不明确，请根据上下文合理推断和概括
                
                记住：你的目标是生成一份完整、详细、有价值的论文总结，而不是空壳结构！
                """;
            
            // 🔥 使用 QuestionAnswerAdvisor 进行 RAG 增强的论文总结
            log.info("开始调用 LLM 生成论文总结（使用 RAG）");
            String rawResponse = ragChatClient.prompt()
                    .advisors(qaAdvisor)  // 🎯 RAG 会自动检索相关分块并注入上下文
                    .system(systemMessage)
                    .user(paperSummaryPromptTemplate.replace("{context}", "基于检索到的相关内容"))
                    .call()
                    .content();
            
            log.info("LLM 响应接收完成，开始解析 JSON");
            
            // 解析 JSON 响应
            PaperSummaryVO summary;
            try {
                summary = objectMapper.readValue(rawResponse, PaperSummaryVO.class);
            } catch (Exception parseEx) {
                log.error("JSON 解析失败: {}", rawResponse, parseEx);
                throw new BusinessException("解析论文总结失败: " + parseEx.getMessage());
            }
            
            log.info("结构化输出成功: title={}", summary.getTitle());

            // 验证和填充默认值
            summary.validate();

            // 查询文档标题
            DocumentDO document = documentMapper.selectById(documentId);
            String documentTitle = (document != null && document.getTitle() != null)
                    ? document.getTitle() : "未知文档";

            // 构建引用列表
            List<Document> retrievedDocs = new ArrayList<>();
            for (DocumentChunkDO chunk : allChunks) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", String.valueOf(documentId));
                metadata.put("chunkIndex", chunk.getChunkIndex());
                metadata.put("title", documentTitle);

                Document doc = Document.builder()
                        .id(chunk.getVectorId())
                        .score(1.0)
                        .text(chunk.getContent())
                        .metadata(metadata)
                        .build();
                retrievedDocs.add(doc);
            }

            List<CitationVO> citations = citationBuilderService.buildCitations(retrievedDocs, "论文总结", 500);

            log.info("论文总结完成: documentId={}, citations={}", documentId, citations.size());

            return RagAnswerVO.<PaperSummaryVO>builder()
                    .answer(summary)
                    .citations(citations)
                    .build();
        } catch (Exception e) {
            log.error("论文总结失败: documentId={}", documentId, e);
            throw new BusinessException("论文总结失败: " + e.getMessage());
        }
    }

    @Override
    public RagAnswerVO<PaperComparisonVO> comparePapers(List<Long> documentIds, List<String> dimensions) {
        log.info("论文对比: documentIds={}, dimensions={}", documentIds, dimensions);

        // 验证文档权限
        Long userId = StpUtil.getLoginIdAsLong();
        List<DocumentDO> documents = documentMapper.selectList(
                new LambdaQueryWrapper<DocumentDO>()
                        .in(DocumentDO::getId, documentIds)
                        .eq(DocumentDO::getIsDeleted, 0));

        if (documents.size() != documentIds.size()) {
            throw new BusinessException("部分文档不存在");
        }

        for (DocumentDO doc : documents) {
            if (!doc.getUserId().equals(userId)) {
                throw new BusinessException("无权访问文档: " + doc.getTitle());
            }
        }

        // 默认对比维度
        if (dimensions == null || dimensions.isEmpty()) {
            dimensions = List.of("研究方法", "使用数据集", "主要创新点", "研究结论", "实验结果");
        }

        // 获取每篇论文的前30个chunk
        Map<Long, List<DocumentChunkDO>> paperChunksMap = new HashMap<>();
        for (Long docId : documentIds) {
            List<DocumentChunkDO> chunks = documentChunkMapper.selectList(
                    new LambdaQueryWrapper<DocumentChunkDO>()
                            .eq(DocumentChunkDO::getDocumentId, docId)
                            .eq(DocumentChunkDO::getIsDeleted, 0)
                            .orderByAsc(DocumentChunkDO::getChunkIndex)
                            .last("LIMIT 30"));
            paperChunksMap.put(docId, chunks);
        }

        // 构建结构化的对比prompt
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("【输出格式】\n");
        contextBuilder.append(paperComparisonPromptTemplate);
        contextBuilder.append("\n\n");
        
        contextBuilder.append("【对比维度】\n");
        for (String dim : dimensions) {
            contextBuilder.append("- ").append(dim).append("\n");
        }
        contextBuilder.append("\n");
        
        contextBuilder.append("【待对比论文】\n");
        for (int i = 0; i < documents.size(); i++) {
            DocumentDO doc = documents.get(i);
            List<DocumentChunkDO> chunks = paperChunksMap.get(doc.getId());
            
            contextBuilder.append("\n论文").append(i + 1).append(":\n");
            contextBuilder.append("ID: ").append(doc.getId()).append("\n");
            contextBuilder.append("标题: ").append(doc.getTitle()).append("\n");
            
            if (chunks != null && !chunks.isEmpty()) {
                contextBuilder.append("关键内容:\n");
                for (int j = 0; j < Math.min(chunks.size(), 3); j++) {
                    String content = chunks.get(j).getContent();
                    if (content.length() > 500) {
                        content = content.substring(0, 500) + "...";
                    }
                    contextBuilder.append(content).append("\n");
                }
            }
        }
        
        contextBuilder.append("\n\n【任务】\n");
        contextBuilder.append("基于上述").append(documents.size()).append("篇论文的内容，");
        contextBuilder.append("按照给定的").append(dimensions.size()).append("个对比维度进行详细分析对比，");
        contextBuilder.append("严格按照输出格式返回完整的JSON结构。\n");
        contextBuilder.append("注意: dimensions、papers、matrix 三个数组都必须包含实际数据，不能为空。");

        // 构建引用列表
        List<Document> allRetrievedDocs = new ArrayList<>();
        for (Map.Entry<Long, List<DocumentChunkDO>> entry : paperChunksMap.entrySet()) {
            Long docId = entry.getKey();
            List<DocumentChunkDO> chunks = entry.getValue();
            String docTitle = documents.stream()
                    .filter(d -> d.getId().equals(docId))
                    .findFirst()
                    .map(DocumentDO::getTitle)
                    .orElse("未知文档");
            
            for (DocumentChunkDO chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", String.valueOf(docId));
                metadata.put("chunkIndex", chunk.getChunkIndex());
                metadata.put("title", docTitle);
                
                Document doc = Document.builder()
                        .id(chunk.getVectorId())
                        .score(1.0)
                        .text(chunk.getContent())
                        .metadata(metadata)
                        .build();
                allRetrievedDocs.add(doc);
            }
        }

        try {
            String systemMessage = String.format(
                "你是学术论文对比专家。当前需要对比 %d 篇论文的 %d 个维度。" +
                "必须返回包含 dimensions、papers、matrix 三个数组的完整JSON，每个数组都必须有实际内容。",
                documents.size(), dimensions.size()
            );
            
            long startTime = System.currentTimeMillis();

            // 使用 ChatClient.entity() 直接获取结构化输出
            PaperComparisonVO comparison = ragChatClient.prompt()
                    .system(systemMessage)
                    .user(contextBuilder.toString())
                    .call()
                    .entity(PaperComparisonVO.class);
            
            long llmTime = System.currentTimeMillis() - startTime;
            log.info("LLM 响应完成,耗时: {}ms", llmTime);
            
            if (comparison == null || 
                comparison.getDimensions() == null || comparison.getDimensions().isEmpty() ||
                comparison.getPapers() == null || comparison.getPapers().isEmpty() ||
                comparison.getMatrix() == null || comparison.getMatrix().isEmpty()) {
                throw new BusinessException("LLM 返回了不完整的对比结果,请重试或减少对比维度");
            }
            
            log.info("论文对比解析成功: dimensions={}, papers={}, matrix={}", 
                    comparison.getDimensions().size(), 
                    comparison.getPapers().size(),
                    comparison.getMatrix().size());

            List<CitationVO> citations = citationBuilderService.buildCitations(allRetrievedDocs, "论文对比", 200);

            return RagAnswerVO.<PaperComparisonVO>builder()
                    .answer(comparison)
                    .citations(citations)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("论文对比失败: documentIds={}", documentIds, e);
            throw new BusinessException("论文对比失败: " + e.getMessage());
        }
    }

    @Override
    public PaperMetadataVO extractPaperMetadata(Long documentId) {
        log.info("抽取论文元数据: documentId={}", documentId);

        // 先尝试从缓存读取
        String cacheKey = PAPER_METADATA_CACHE_PREFIX + documentId;
        try {
            Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
            if (cachedObj != null) {
                String cachedJson = cachedObj.toString();
                PaperMetadataVO cached = objectMapper.readValue(cachedJson, PaperMetadataVO.class);
                if (cached != null && cached.getTitle() != null && !cached.getTitle().isBlank()) {
                    log.info("使用Redis缓存的元数据: documentId={}", documentId);
                    cached.setDocumentId(documentId);
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("读取Redis缓存失败: documentId={}", documentId, e);
        }

        DocumentDO document = documentMapper.selectById(documentId);

        // 🆕 优先使用 GROBID 提取元数据(仅PDF,精度更高)
        PaperMetadataVO grobidExtractedMetadata = null;
        if ("pdf".equalsIgnoreCase(document.getType()) && grobidMetadataService.isAvailable()) {
            try {
                log.info("尝试使用 GROBID 提取论文元数据: documentId={}", documentId);
                
                // 获取 PDF 文件
                byte[] pdfData = fileStorageService.downloadFile(document.getFileUrl());
                MultipartFile pdfFile = createMultipartFile(pdfData, document.getTitle());
                
                // 使用 GROBID 提取
                DocumentMetadataVO grobidMetadata = grobidMetadataService.extractMetadata(pdfFile);
                
                if (grobidMetadata != null && grobidMetadata.getTitle() != null) {
                    log.info("GROBID 提取成功: documentId={}, title={}", documentId, grobidMetadata.getTitle());
                    
                    // 转换为 PaperMetadataVO
                    grobidExtractedMetadata = convertGrobidToPaperMetadata(grobidMetadata, documentId, document.getTitle());
                    
                    // 检查是否需要LLM补充(如果关键字段缺失)
                    if (isMetadataComplete(grobidExtractedMetadata)) {
                        log.info("GROBID提取的元数据已完整,直接返回: documentId={}", documentId);
                        cacheMetadata(cacheKey, grobidExtractedMetadata);
                        return grobidExtractedMetadata;
                    } else {
                        log.info("GROBID提取的元数据不完整,将使用LLM补充: documentId={}", documentId);
                    }
                }
            } catch (Exception e) {
                log.warn("GROBID 提取失败,降级到 LLM 提取: documentId={}", documentId, e);
            }
        }

        // 🔻 降级方案: 使用 LLM 提取(保留原有逻辑)
        // 获取文档的前20个chunk
        List<DocumentChunkDO> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                        .eq(DocumentChunkDO::getIsDeleted, 0)
                        .orderByAsc(DocumentChunkDO::getChunkIndex)
                        .last("LIMIT 20"));

        if (chunks.isEmpty()) {
            log.warn("文档没有chunk数据: documentId={}", documentId);
            // 如果有GROBID数据,返回GROBID数据;否则返回空数据
            if (grobidExtractedMetadata != null) {
                log.info("文档无chunk,返回GROBID提取的部分元数据: documentId={}", documentId);
                return grobidExtractedMetadata;
            }
            PaperMetadataVO fallback = new PaperMetadataVO();
            fallback.setDocumentId(documentId);
            fallback.setTitle(document.getTitle());
            return fallback;
        }

        StringBuilder contextBuilder = new StringBuilder();
        
        // 如果有GROBID数据,添加到上下文中辅助LLM
        if (grobidExtractedMetadata != null) {
            contextBuilder.append("【已提取的部分信息(请补充缺失字段)】\n");
            contextBuilder.append("标题: ").append(grobidExtractedMetadata.getTitle()).append("\n");
            
            // 尝试从标题中提取年份作为提示
            String titleHint = extractYearFromTitle(grobidExtractedMetadata.getTitle());
            if (titleHint != null) {
                contextBuilder.append("⚠️ 标题中包含年份: ").append(titleHint).append("\n");
            }
            
            if (grobidExtractedMetadata.getAuthors() != null && !grobidExtractedMetadata.getAuthors().isEmpty()) {
                contextBuilder.append("作者: ").append(String.join(", ", grobidExtractedMetadata.getAuthors())).append("\n");
            }
            if (grobidExtractedMetadata.getYear() != null && !grobidExtractedMetadata.getYear().isBlank()) {
                contextBuilder.append("年份: ").append(grobidExtractedMetadata.getYear()).append("\n");
            } else {
                contextBuilder.append("⚠️ 年份缺失,需要从标题或文档内容提取\n");
            }
            if (grobidExtractedMetadata.getAbstractText() != null && !grobidExtractedMetadata.getAbstractText().isBlank()) {
                contextBuilder.append("摘要: ").append(grobidExtractedMetadata.getAbstractText()).append("\n");
            }
            contextBuilder.append("\n【必须完成的任务】\n");
            contextBuilder.append("1. 如果年份为空,从标题(特别注意数字如2025/2025)或文档中提取\n");
            contextBuilder.append("2. 根据标题、摘要、关键词推断学科领域(不能为空)\n");
            contextBuilder.append("3. 尽可能从文档中找到期刊/会议信息\n\n");
        } else {
            contextBuilder.append("【文档标题(重要!)】\n");
            contextBuilder.append(document.getTitle()).append("\n");
            
            // 尝试从标题中提取年份作为提示
            String titleHint = extractYearFromTitle(document.getTitle());
            if (titleHint != null) {
                contextBuilder.append("\n⚠️ 重要提示: 标题中包含年份 ").append(titleHint).append(",请务必提取!\n");
            }
            
            contextBuilder.append("\n【必须完成的任务】\n");
            contextBuilder.append("1. 从标题中提取年份(如果标题包含2025、2025等数字)\n");
            contextBuilder.append("2. 根据标题和文档内容推断学科领域(必填)\n");
            contextBuilder.append("3. 提取所有可获得的元数据信息\n\n");
        }
        
        contextBuilder.append("【文档开头内容(包含首页、摘要、关键词等重要信息)】\n");
        contextBuilder.append("=".repeat(50)).append("\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkDO chunk = chunks.get(i);
            contextBuilder.append("【第 ").append(i + 1).append(" 段】\n");
            contextBuilder.append(chunk.getContent()).append("\n\n");
        }

        try {
            String prompt = paperMetadataExtractionPromptTemplate
                    .replace("{context}", contextBuilder.toString())
                    .replace("{documentId}", String.valueOf(documentId));

            PaperMetadataVO metadata = ragChatClient.prompt()
                    .system("你是一位专业的学术文献分析专家，擅长从论文中准确抽取题录信息。" +
                            "你特别擅长:\n" +
                            "1. 从标题中识别年份(如'专家共识2025'中的2025)\n" +
                            "2. 根据论文内容和关键词推断学科领域\n" +
                            "3. 从首页、页眉、页脚提取期刊/会议信息\n" +
                            "4. 区分不同类型的学术文献(期刊论文、会议论文、专家共识、技术报告等)")
                    .user(prompt)
                    .call()
                    .entity(PaperMetadataVO.class);

            if (metadata != null) {
                metadata.setDocumentId(documentId);
                
                // 如果有GROBID数据,合并结果(LLM优先,但保留GROBID的非空值)
                if (grobidExtractedMetadata != null) {
                    metadata = mergeMetadata(grobidExtractedMetadata, metadata, document.getTitle());
                } else {
                    if (metadata.getTitle() == null || metadata.getTitle().isBlank()) {
                        metadata.setTitle(document.getTitle());
                    }
                }
                
                // 缓存到Redis
                cacheMetadata(cacheKey, metadata);
            }

            return metadata;

        } catch (Exception e) {
            log.error("论文元数据抽取失败: documentId={}", documentId, e);
            
            // 如果LLM失败但有GROBID数据,返回GROBID数据
            if (grobidExtractedMetadata != null) {
                log.warn("LLM提取失败,返回GROBID部分数据: documentId={}", documentId);
                return grobidExtractedMetadata;
            }
            
            throw new BusinessException("元数据抽取失败,请稍后重试");
        }
    }

    @Override
    public void clearPaperMetadataCache(Long documentId) {
        String cacheKey = PAPER_METADATA_CACHE_PREFIX + documentId;
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("已清除元数据缓存: documentId={}", documentId);
            }
        } catch (Exception e) {
            log.error("清除元数据缓存失败: documentId={}", documentId, e);
        }
    }

    @Override
    public List<InnovationClusterVO> aggregateInnovations(List<Long> documentIds) {
        log.info("创新点聚合: documentIds={}", documentIds);

        Long userId = StpUtil.getLoginIdAsLong();

        for (Long docId : documentIds) {
            validateDocumentAccess(docId, userId);
        }

        // 为每篇论文提取创新点
        List<InnovationClusterVO.InnovationPoint> allInnovations = documentIds.stream()
                .flatMap(docId -> {
                    try {
                        return extractInnovations(docId).stream();
                    } catch (Exception e) {
                        log.error("提取文档创新点失败: documentId={}", docId, e);
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();

        if (allInnovations.isEmpty()) {
            return List.of();
        }

        // 使用LLM对创新点进行聚类
        try {
            String innovationList = allInnovations.stream()
                    .map(inn -> String.format("- %s (来自: %s, documentId: %d, noveltyScore: %.2f)",
                            inn.getDescription(),
                            inn.getPaperTitle(),
                            inn.getDocumentId(),
                            inn.getNoveltyScore()))
                    .collect(Collectors.joining("\n"));
            
            String prompt = innovationClusteringPromptTemplate.replace("{innovationList}", innovationList);

            String response = ragChatClient.prompt()
                    .system("你是一位科研创新分析专家，擅长识别研究主题和创新趋势。")
                    .user(prompt)
                    .call()
                    .content();

            List<InnovationClusterVO> clusters = parseInnovationClusters(response);

            log.info("创新点聚合完成: 聚类数={}", clusters.size());
            return clusters;

        } catch (Exception e) {
            log.error("创新点聚合失败", e);
            throw new BusinessException("创新点聚合失败,请稍后重试");
        }
    }

    // ==================== 私有辅助方法 ====================

    // 🗑️ 已删除低效的两阶段批处理方法（buildDirectContext, buildTwoStageSummary, generateIntermediateSummary）
    // 现在使用 QuestionAnswerAdvisor + RAG 进行高效的论文总结

    /**
     * 提取单篇论文的创新点
     */
    private List<InnovationClusterVO.InnovationPoint> extractInnovations(Long documentId) {
        DocumentDO document = documentMapper.selectById(documentId);

        List<DocumentChunkDO> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                        .eq(DocumentChunkDO::getIsDeleted, 0)
                        .orderByAsc(DocumentChunkDO::getChunkIndex)
        );

        if (chunks.isEmpty()) {
            return List.of();
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("论文标题: ").append(document.getTitle()).append("\n\n");
        contextBuilder.append("论文核心内容片段:\n");
        contextBuilder.append("=".repeat(50)).append("\n\n");

        for (int i = 0; i < Math.min(chunks.size(), 10); i++) {
            DocumentChunkDO chunk = chunks.get(i);
            contextBuilder.append("【片段 ").append(i + 1).append("】\n");
            contextBuilder.append(chunk.getContent()).append("\n\n");
        }

        String prompt = innovationExtractionPromptTemplate.replace("{context}", contextBuilder.toString());

        try {
            String response = ragChatClient.prompt()
                    .system("你是一位资深的科研论文审稿专家，擅长识别论文的核心贡献和创新价值。")
                    .user(prompt)
                    .call()
                    .content();

            List<InnovationClusterVO.InnovationPoint> innovations = parseInnovationPoints(response);

            innovations.forEach(inn -> {
                inn.setPaperTitle(document.getTitle());
                inn.setDocumentId(documentId);
            });

            return innovations;

        } catch (Exception e) {
            log.error("提取创新点失败: documentId={}", documentId, e);
            return List.of();
        }
    }

    /**
     * 验证文档访问权限
     */
    private void validateDocumentAccess(Long documentId, Long userId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该文档");
        }
    }

    /**
     * 解析创新点列表
     * 保留 LLMJsonUtils - 这是纯 JSON 解析,不涉及 LLM 调用
     */
    private List<InnovationClusterVO.InnovationPoint> parseInnovationPoints(String json) {
        try {
            return LLMJsonUtils.parseArray(json, InnovationClusterVO.InnovationPoint.class);
        } catch (Exception e) {
            log.warn("解析创新点失败", e);
            return List.of();
        }
    }

    /**
     * 解析创新点聚类结果
     * 保留 LLMJsonUtils - 这是纯 JSON 解析,不涉及 LLM 调用
     */
    private List<InnovationClusterVO> parseInnovationClusters(String json) {
        try {
            return LLMJsonUtils.parseArray(json, InnovationClusterVO.class);
        } catch (Exception e) {
            log.warn("解析创新聚类失败", e);
            return List.of();
        }
    }
    
    /**
     * 🆕 将 GROBID 元数据转换为 PaperMetadataVO
     */
    private PaperMetadataVO convertGrobidToPaperMetadata(DocumentMetadataVO grobidMetadata, Long documentId, String fallbackTitle) {
        PaperMetadataVO paperMetadata = new PaperMetadataVO();
        paperMetadata.setDocumentId(documentId);
        
        // 标题
        paperMetadata.setTitle(grobidMetadata.getTitle() != null ? grobidMetadata.getTitle() : fallbackTitle);
        
        // 作者列表
        if (grobidMetadata.getAuthors() != null && !grobidMetadata.getAuthors().isEmpty()) {
            List<String> authorNames = grobidMetadata.getAuthors().stream()
                .map(DocumentMetadataVO.Author::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
            paperMetadata.setAuthors(authorNames);
        }
        
        // 摘要
        paperMetadata.setAbstractText(grobidMetadata.getAbstractText());
        
        // 关键词
        paperMetadata.setKeywords(grobidMetadata.getKeywords());
        
        // 出版信息
        paperMetadata.setYear(grobidMetadata.getPublicationYear());  // year 字段
        paperMetadata.setPublication(grobidMetadata.getVenue());  // publication 字段
        paperMetadata.setDoi(grobidMetadata.getDoi());
        
        // 引用数量 (PaperMetadataVO 用 citationCount 而非 referenceCount)
        if (grobidMetadata.getReferenceCount() != null) {
            paperMetadata.setCitationCount(grobidMetadata.getReferenceCount());
        }
        
        return paperMetadata;
    }
    
    /**
     * 🆕 创建 MultipartFile 对象
     */
    private MultipartFile createMultipartFile(byte[] content, String filename) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return "application/pdf";
            }

            @Override
            public boolean isEmpty() {
                return content == null || content.length == 0;
            }

            @Override
            public long getSize() {
                return content != null ? content.length : 0;
            }

            @Override
            public byte[] getBytes() {
                return content;
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    fos.write(content);
                }
            }
        };
    }
    
    /**
     * 检查元数据是否完整(所有重要字段都已填充)
     * 
     * 判断标准:
     * - 必须有: title, authors, abstractText, keywords
     * - 最好有: year, publication, field
     * - 可选: volume, issue, pages, doi
     */
    private boolean isMetadataComplete(PaperMetadataVO metadata) {
        if (metadata == null) {
            return false;
        }
        
        // 必须字段
        boolean hasTitle = metadata.getTitle() != null && !metadata.getTitle().isBlank();
        boolean hasAuthors = metadata.getAuthors() != null && !metadata.getAuthors().isEmpty();
        boolean hasAbstract = metadata.getAbstractText() != null && !metadata.getAbstractText().isBlank();
        boolean hasKeywords = metadata.getKeywords() != null && !metadata.getKeywords().isEmpty();
        
        // 重要字段(建议有)
        boolean hasYear = metadata.getYear() != null && !metadata.getYear().isBlank();
        boolean hasPublication = metadata.getPublication() != null && !metadata.getPublication().isBlank();
        boolean hasField = metadata.getField() != null && !metadata.getField().isBlank();
        
        // 如果任何必须字段缺失,则不完整
        if (!hasTitle || !hasAuthors || !hasAbstract || !hasKeywords) {
            log.debug("元数据缺少必须字段: title={}, authors={}, abstract={}, keywords={}", 
                hasTitle, hasAuthors, hasAbstract, hasKeywords);
            return false;
        }
        
        // 如果缺少2个或以上重要字段,也认为不完整,需要LLM补充
        int missingImportantFields = 0;
        if (!hasYear) missingImportantFields++;
        if (!hasPublication) missingImportantFields++;
        if (!hasField) missingImportantFields++;
        
        if (missingImportantFields >= 2) {
            log.debug("元数据缺少多个重要字段: year={}, publication={}, field={}", 
                hasYear, hasPublication, hasField);
            return false;
        }
        
        return true;
    }
    
    /**
     * 缓存元数据到Redis
     */
    private void cacheMetadata(String cacheKey, PaperMetadataVO metadata) {
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(
                cacheKey,
                metadataJson,
                CACHE_EXPIRE_DAYS,
                java.util.concurrent.TimeUnit.DAYS
            );
        } catch (Exception e) {
            log.warn("缓存元数据失败: cacheKey={}", cacheKey, e);
        }
    }
    
    /**
     * 合并GROBID和LLM提取的元数据
     * 策略: LLM提取的数据优先,但如果LLM某字段为空而GROBID有值,则使用GROBID的值
     */
    private PaperMetadataVO mergeMetadata(PaperMetadataVO grobidData, PaperMetadataVO llmData, String fallbackTitle) {
        PaperMetadataVO merged = new PaperMetadataVO();
        
        // documentId
        merged.setDocumentId(llmData.getDocumentId());
        
        // 标题: LLM > GROBID > fallback
        if (llmData.getTitle() != null && !llmData.getTitle().isBlank()) {
            merged.setTitle(llmData.getTitle());
        } else if (grobidData.getTitle() != null && !grobidData.getTitle().isBlank()) {
            merged.setTitle(grobidData.getTitle());
        } else {
            merged.setTitle(fallbackTitle);
        }
        
        // 作者: 优先LLM,其次GROBID
        if (llmData.getAuthors() != null && !llmData.getAuthors().isEmpty()) {
            merged.setAuthors(llmData.getAuthors());
        } else {
            merged.setAuthors(grobidData.getAuthors());
        }
        
        // 年份: 优先LLM,其次GROBID
        String year = isNotEmpty(llmData.getYear()) ? llmData.getYear() : grobidData.getYear();
        merged.setYear(year);
        if (!isNotEmpty(year)) {
            log.warn("合并后年份仍为空 - GROBID: {}, LLM: {}", grobidData.getYear(), llmData.getYear());
        }
        
        // 期刊/会议: 优先LLM,其次GROBID
        String publication = isNotEmpty(llmData.getPublication()) ? llmData.getPublication() : grobidData.getPublication();
        merged.setPublication(publication);
        if (!isNotEmpty(publication)) {
            log.debug("合并后期刊/会议为空");
        }
        
        // 卷号、期号、页码: 优先LLM
        merged.setVolume(llmData.getVolume());
        merged.setIssue(llmData.getIssue());
        merged.setPages(llmData.getPages());
        
        // DOI: 优先LLM,其次GROBID
        merged.setDoi(isNotEmpty(llmData.getDoi()) ? llmData.getDoi() : grobidData.getDoi());
        
        // 关键词: 优先LLM,其次GROBID
        if (llmData.getKeywords() != null && !llmData.getKeywords().isEmpty()) {
            merged.setKeywords(llmData.getKeywords());
        } else {
            merged.setKeywords(grobidData.getKeywords());
        }
        
        // 摘要: 优先GROBID(通常更完整),其次LLM
        if (grobidData.getAbstractText() != null && !grobidData.getAbstractText().isBlank()) {
            merged.setAbstractText(grobidData.getAbstractText());
        } else {
            merged.setAbstractText(llmData.getAbstractText());
        }
        
        // 引用数: 优先LLM
        merged.setCitationCount(llmData.getCitationCount() != null && llmData.getCitationCount() > 0 
            ? llmData.getCitationCount() 
            : grobidData.getCitationCount());
        
        // 领域: LLM提取(GROBID通常没有此字段)
        String field = llmData.getField();
        merged.setField(field);
        if (!isNotEmpty(field)) {
            log.warn("合并后领域仍为空 - LLM未成功提取领域信息");
        }
        
        log.info("元数据合并完成 - 年份: {}, 期刊: {}, 领域: {}", 
            merged.getYear(), merged.getPublication(), merged.getField());
        
        return merged;
    }
    
    /**
     * 检查字符串是否非空
     */
    private boolean isNotEmpty(String str) {
        return str != null && !str.isBlank();
    }
    
    /**
     * 从标题中提取年份(如果存在)
     * 例如: "专家共识2025" → "2025"
     */
    private String extractYearFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        
        // 匹配4位数字的年份(2000-2099)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(20\\d{2})");
        java.util.regex.Matcher matcher = pattern.matcher(title);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    // ==================== 文献综述功能 ====================
    
    @Override
    public RagAnswerVO<LiteratureReviewVO> generateLiteratureReview(List<Long> documentIds) {
        log.info("生成文献综述: documentIds={}", documentIds);
        
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BusinessException("文档列表不能为空");
        }
        
        if (documentIds.size() < 2) {
            throw new BusinessException("文献综述至少需要2篇论文");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 验证所有文档权限
        for (Long docId : documentIds) {
            validateDocumentAccess(docId, userId);
        }
        
        try {
            // 1. 为每篇论文生成总结
            log.info("为 {} 篇论文生成总结...", documentIds.size());
            List<PaperSummaryInfo> paperSummaries = documentIds.stream()
                    .map(this::generatePaperSummaryForReview)
                    .filter(Objects::nonNull)
                    .toList();
            
            if (paperSummaries.isEmpty()) {
                throw new BusinessException("无法提取论文信息，请确保文档已正确处理");
            }
            
            // 2. 构建提示词输入
            String paperSummariesText = buildPaperSummariesText(paperSummaries);
            String prompt = literatureReviewPromptTemplate.replace("{paperSummaries}", paperSummariesText);
            
            log.info("开始生成文献综述，论文数量: {}", paperSummaries.size());
            
            // 3. 调用LLM生成综述
            String response = ragChatClient.prompt()
                    .system("你是一位资深的学术研究专家，擅长撰写高质量的文献综述。请仔细分析提供的论文信息，生成结构化、有深度的综述报告。")
                    .user(prompt)
                    .call()
                    .content();
            
            log.info("LLM返回综述内容，长度: {} 字符", response.length());
            
            // 4. 解析JSON响应
            LiteratureReviewVO review = parseLiteratureReview(response);
            review.validate();
            
            // 5. 构建引用信息（从所有论文的分块中提取）
            List<CitationVO> citations = buildReviewCitations(documentIds);
            
            log.info("文献综述生成成功，主题: {}, 论文数: {}", review.getTopic(), review.getPaperCount());
            
            return RagAnswerVO.<LiteratureReviewVO>builder()
                    .answer(review)
                    .citations(citations)
                    .build();
                    
        } catch (Exception e) {
            log.error("生成文献综述失败", e);
            throw new BusinessException("生成文献综述失败: " + e.getMessage());
        }
    }
    
    /**
     * 为综述生成单篇论文的总结信息
     */
    private PaperSummaryInfo generatePaperSummaryForReview(Long documentId) {
        try {
            DocumentDO document = documentMapper.selectById(documentId);
            if (document == null) {
                log.warn("文档不存在: documentId={}", documentId);
                return null;
            }
            
            // 获取论文内容片段
            List<DocumentChunkDO> chunks = documentChunkMapper.selectList(
                    new LambdaQueryWrapper<DocumentChunkDO>()
                            .eq(DocumentChunkDO::getDocumentId, documentId)
                            .eq(DocumentChunkDO::getIsDeleted, 0)
                            .orderByAsc(DocumentChunkDO::getChunkIndex)
                            .last("LIMIT 10")  // 只取前10个分块
            );
            
            if (chunks.isEmpty()) {
                log.warn("文档内容为空: documentId={}", documentId);
                return null;
            }
            
            // 构建论文摘要
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("【论文标题】\n").append(document.getTitle()).append("\n\n");
            
            for (int i = 0; i < Math.min(chunks.size(), 5); i++) {
                contentBuilder.append("【片段 ").append(i + 1).append("】\n");
                contentBuilder.append(chunks.get(i).getContent()).append("\n\n");
            }
            
            // 提取元数据（年份、作者等）
            PaperMetadataVO metadata = null;
            try {
                metadata = extractPaperMetadata(documentId);
            } catch (Exception e) {
                log.warn("提取元数据失败: documentId={}", documentId, e);
            }
            
            return PaperSummaryInfo.builder()
                    .documentId(documentId)
                    .title(document.getTitle())
                    .authors(metadata != null && metadata.getAuthors() != null ? metadata.getAuthors() : List.of())
                    .year(metadata != null && metadata.getYear() != null ? metadata.getYear() : extractYearFromTitle(document.getTitle()))
                    .contentSummary(contentBuilder.toString())
                    .build();
                    
        } catch (Exception e) {
            log.error("生成论文总结信息失败: documentId={}", documentId, e);
            return null;
        }
    }
    
    /**
     * 构建论文总结文本（用于提示词）
     */
    private String buildPaperSummariesText(List<PaperSummaryInfo> paperSummaries) {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < paperSummaries.size(); i++) {
            PaperSummaryInfo summary = paperSummaries.get(i);
            builder.append("=".repeat(60)).append("\n");
            builder.append(String.format("【论文 %d/%d】\n", i + 1, paperSummaries.size()));
            builder.append(String.format("DocumentID: %d\n", summary.getDocumentId()));
            builder.append(String.format("标题: %s\n", summary.getTitle()));
            
            if (summary.getAuthors() != null && !summary.getAuthors().isEmpty()) {
                builder.append(String.format("作者: %s\n", String.join(", ", summary.getAuthors())));
            }
            
            if (summary.getYear() != null) {
                builder.append(String.format("年份: %s\n", summary.getYear()));
            }
            
            builder.append("\n【内容摘要】\n");
            builder.append(summary.getContentSummary()).append("\n\n");
        }
        
        return builder.toString();
    }
    
    /**
     * 构建综述的引用信息
     */
    private List<CitationVO> buildReviewCitations(List<Long> documentIds) {
        List<CitationVO> citations = new ArrayList<>();
        
        for (Long docId : documentIds) {
            try {
                DocumentDO document = documentMapper.selectById(docId);
                if (document == null) continue;
                
                // 获取代表性分块
                List<DocumentChunkDO> chunks = documentChunkMapper.selectList(
                        new LambdaQueryWrapper<DocumentChunkDO>()
                                .eq(DocumentChunkDO::getDocumentId, docId)
                                .eq(DocumentChunkDO::getIsDeleted, 0)
                                .orderByAsc(DocumentChunkDO::getChunkIndex)
                                .last("LIMIT 3")
                );
                
                for (DocumentChunkDO chunk : chunks) {
                    citations.add(CitationVO.builder()
                            .documentId(docId)
                            .title(document.getTitle())
                            .snippet(chunk.getContent())
                            .chunkIndex(chunk.getChunkIndex())
                            .score(0.8)  // 默认相关性
                            .build());
                }
                
            } catch (Exception e) {
                log.error("构建引用失败: documentId={}", docId, e);
            }
        }
        
        return citations;
    }
    
    /**
     * 解析文献综述JSON
     */
    private LiteratureReviewVO parseLiteratureReview(String jsonResponse) {
        try {
            String cleanJson = LLMJsonUtils.cleanJsonResponse(jsonResponse);
            LiteratureReviewVO review = objectMapper.readValue(cleanJson, LiteratureReviewVO.class);
            
            if (review == null) {
                throw new BusinessException("解析综述JSON失败：结果为空");
            }
            
            return review;
            
        } catch (Exception e) {
            log.error("解析文献综述JSON失败，原始响应: {}", jsonResponse, e);
            throw new BusinessException("解析综述结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 论文总结信息（用于综述生成）
     */
    @Data
    @Builder
    private static class PaperSummaryInfo {
        private Long documentId;
        private String title;
        private List<String> authors;
        private String year;
        private String contentSummary;
    }
}
