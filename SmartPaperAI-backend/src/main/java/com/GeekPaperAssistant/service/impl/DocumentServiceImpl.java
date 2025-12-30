package com.GeekPaperAssistant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base62;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.GeekPaperAssistant.config.properties.RAGProperties;
import com.GeekPaperAssistant.model.dto.DocumentQueryDTO;
import com.GeekPaperAssistant.model.entity.DocumentChunkDO;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import com.GeekPaperAssistant.model.entity.DocumentVectorDO;
import com.GeekPaperAssistant.model.entity.KnowledgeBaseDO;
import com.GeekPaperAssistant.model.vo.DocumentFileVO;
import com.GeekPaperAssistant.model.vo.DocumentMetadataVO;
import com.GeekPaperAssistant.model.vo.DocumentSearchResultVO;
import com.GeekPaperAssistant.model.vo.DocumentUploadVO;
import com.GeekPaperAssistant.model.vo.DocumentVO;
import com.GeekPaperAssistant.mapper.DocumentChunkMapper;
import com.GeekPaperAssistant.mapper.DocumentMapper;
import com.GeekPaperAssistant.mapper.DocumentVectorMapper;
import com.GeekPaperAssistant.mapper.KnowledgeBaseMapper;
import com.GeekPaperAssistant.service.DocumentService;
import com.GeekPaperAssistant.service.DocumentESService;
import com.GeekPaperAssistant.service.DocumentParserService;
import com.GeekPaperAssistant.service.FileStorageService;
import com.GeekPaperAssistant.service.DocumentTaskService;
import com.GeekPaperAssistant.service.GrobidMetadataService;
import com.GeekPaperAssistant.service.SmartChunkingService;
import com.GeekPaperAssistant.service.KnowledgeGraphBuilderService;
import com.GeekPaperAssistant.mq.DocumentProcessingProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 * 
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final VectorStore vectorStore;
    private final RAGProperties ragConfig;
    private final DocumentParserService documentParserService;
    private final FileStorageService fileStorageService;
    private final DocumentVectorMapper documentVectorMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentProcessingProducer documentProcessingProducer;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentTaskService documentTaskService;
    private final DocumentESService documentESService;
    private final GrobidMetadataService grobidMetadataService;
    private final SmartChunkingService smartChunkingService;
    private final KnowledgeGraphBuilderService knowledgeGraphBuilderService;
    
    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentUploadVO uploadDocument(MultipartFile file, String type, Long userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 上传文件到存储服务
        String fileUrl = fileStorageService.uploadFile(file, "documents");

        // 3. 创建文档记录
        DocumentDO document = new DocumentDO();
        document.setUserId(userId);
        document.setTitle(file.getOriginalFilename());
        document.setKbId(-1L); // 默认不属于任何知识库
        document.setType(type);
        document.setFileSize(file.getSize());
        document.setFileUrl(fileUrl);
        document.setStatus("pending");
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        documentMapper.insert(document);

        log.info("文档上传成功: id={}, title={}, fileUrl={}", document.getId(), document.getTitle(), fileUrl);

        // 4. 创建任务跟踪记录
        Long taskId = documentTaskService.createTask(document.getId(), "document_processing");
        log.info("创建文档处理任务: documentId={}, taskId={}", document.getId(), taskId);

        // 5. 更新状态为处理中（在事务内完成）
        document.setStatus("processing");
        documentMapper.updateById(document);

        // 更新任务状态为处理中
        documentTaskService.updateTaskStatus(taskId, "processing", 10, null);

        Long documentId = document.getId();

        // 6. 注册事务提交后的回调：发送异步处理消息
        // 使用 TransactionSynchronizationManager 确保消息在事务提交后才发送
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // 事务已提交，现在可以安全地发送消息了
                            documentProcessingProducer.sendDocumentProcessingMessage(documentId, userId, taskId);
                            log.info("文档已提交异步处理队列（事务已提交）: id={}", documentId);
                        } catch (Exception e) {
                            log.error("提交文档处理消息失败: id={}", documentId, e);
                            // 注意：这里事务已经提交，无法回滚
                            // 需要通过补偿机制处理，例如更新状态为failed
                            try {
                                DocumentDO failedDoc = documentMapper.selectById(documentId);
                                if (failedDoc != null) {
                                    failedDoc.setStatus("failed");
                                    failedDoc.setUpdateTime(LocalDateTime.now());
                                    documentMapper.updateById(failedDoc);
                                }
                                documentTaskService.updateTaskStatus(taskId, "failed", 0, e.getMessage());
                            } catch (Exception ex) {
                                log.error("更新失败状态异常: id={}", documentId, ex);
                            }
                        }
                    }
                });

        // 返回包含 documentId 和 taskId 的 VO
        return DocumentUploadVO.builder()
                .documentId(documentId)
                .taskId(taskId)
                .title(document.getTitle())
                .taskStatusUrl("/api/documents/tasks/" + taskId)
                .build();
    }

    @Override
    public IPage<DocumentVO> pageUserDocuments(Long userId, DocumentQueryDTO queryDTO) {
        // 创建分页对象
        Page<DocumentDO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 构建查询条件
        LambdaQueryWrapper<DocumentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentDO::getUserId, userId)
                .eq(DocumentDO::getIsDeleted, 0);

        // 标题模糊查询
        if (StrUtil.isNotBlank(queryDTO.getTitle())) {
            wrapper.like(DocumentDO::getTitle, queryDTO.getTitle());
        }

        // 类型过滤
        if (StrUtil.isNotBlank(queryDTO.getType())) {
            wrapper.eq(DocumentDO::getType, queryDTO.getType());
        }

        // 状态过滤
        if (StrUtil.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(DocumentDO::getStatus, queryDTO.getStatus());
        }

        // 知识库ID过滤
        if (queryDTO.getKbId() != null) {
            wrapper.eq(DocumentDO::getKbId, queryDTO.getKbId());
        }

        // 排序
        String sortField = queryDTO.getSortField();
        String sortOrder = queryDTO.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByDesc(getColumnByField(sortField));
            } else {
                wrapper.orderByAsc(getColumnByField(sortField));
            }
        } else {
            // 默认按创建时间降序
            wrapper.orderByDesc(DocumentDO::getCreateTime);
        }

        // 执行分页查询
        IPage<DocumentDO> documentPage = documentMapper.selectPage(page, wrapper);

        // 转换为 VO
        IPage<DocumentVO> voPage = new Page<>(documentPage.getCurrent(), documentPage.getSize(),
                documentPage.getRecords().size());
        voPage.setRecords(documentPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 根据字段名获取列函数（用于动态排序）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<DocumentDO, ?> getColumnByField(String field) {
        return switch (field) {
            case "title" -> DocumentDO::getTitle;
            case "type" -> DocumentDO::getType;
            case "status" -> DocumentDO::getStatus;
            case "fileSize" -> DocumentDO::getFileSize;
            case "updateTime" -> DocumentDO::getUpdateTime;
            default -> DocumentDO::getCreateTime;
        };
    }

    @Override
    public DocumentVO getDocument(Long documentId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || (document.getIsDeleted() != null && document.getIsDeleted() == 1)) {
            throw new BusinessException("文档不存在");
        }

        // 权限验证
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权访问该文档");
        }

        return convertToVO(document);
    }

    @Override
    public String parseDocument(Long documentId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || (document.getIsDeleted() != null && document.getIsDeleted() == 1)) {
            throw new BusinessException("文档不存在");
        }

        try {
            // 从存储服务下载文件
            byte[] fileData = fileStorageService.downloadFile(document.getFileUrl());

            // 根据文件类型解析
            String filename = document.getTitle();
            int dot = filename.lastIndexOf('.');
            if (dot < 0 || dot == filename.length() - 1) {
                throw new BusinessException("无法识别的文件类型");
            }
            String extension = filename.substring(dot + 1).toLowerCase();

            String content;
            // 根据文件扩展名选择解析器
            content = switch (extension) {
                case "pdf" -> documentParserService.parsePDFFromBytes(fileData);
                case "docx", "doc" -> documentParserService.parseWordFromBytes(fileData);
                case "txt" -> documentParserService.parseTextFromBytes(fileData);
                case "md", "markdown" -> documentParserService.parseMarkdownFromBytes(fileData);
                default -> throw new BusinessException("不支持的文件格式: " + extension);
            };

            log.info("文档解析成功: id={}, contentLength={}, fileSize={}", documentId, content.length(), fileData.length);
            return content;
        } catch (Exception e) {
            log.error("文档解析失败: id={}", documentId, e);
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vectorizeAndStore(Long documentId, String content, Long userId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        // 验证用户权限
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException("无权处理该文档");
        }

        try {
            // 🆕 1. GROBID 元数据提取(仅针对 PDF 学术论文)
            DocumentMetadataVO grobidMetadata = null;
            if ("pdf".equalsIgnoreCase(document.getType()) && grobidMetadataService.isAvailable()) {
                try {
                    log.info("尝试使用 GROBID 提取文档结构: documentId={}", documentId);

                    // 从存储服务下载 PDF 文件
                    byte[] pdfData = fileStorageService.downloadFile(document.getFileUrl());

                    // 调用 GROBID 提取元数据
                    grobidMetadata = grobidMetadataService.extractMetadata(pdfData, document.getTitle());

                    if (grobidMetadata != null && grobidMetadata.getSections() != null) {
                        log.info("GROBID 提取成功: documentId={}, sections={}",
                                documentId, grobidMetadata.getSections().size());
                    }

                } catch (Exception e) {
                    log.warn("GROBID 提取失败,继续使用普通分块: documentId={}", documentId, e);
                }
            }

            // 🆕 2. 智能分块(替代 TokenTextSplitter)
            List<SmartChunkingService.ChunkResult> smartChunks = smartChunkingService.smartChunk(
                    content,
                    grobidMetadata, // 有章节信息时使用章节分块
                    ragConfig.getDocument().getChunkSize(),
                    ragConfig.getDocument().getChunkOverlap());
            log.info("智能分块完成: documentId={}, chunks={}, strategy={}",
                    documentId, smartChunks.size(),
                    smartChunks.isEmpty() ? "none" : smartChunks.get(0).getType());

            List<Document> documents = new ArrayList<>();
            int globalChunkIndex = 0;

            for (int i = 0; i < smartChunks.size(); i++) {
                SmartChunkingService.ChunkResult chunk = smartChunks.get(i);

                // 估算 token 数 (保守估计,留足余量)
                int estimatedTokens = estimateTokenCount(chunk.getContent());

                List<String> subChunks;
                if (estimatedTokens > maxTokens) {
                    // 分块过长，需要二次分割
                    log.warn("检测到超长分块: chunkIndex={}, estimatedTokens={}, 进行二次分割",
                            i, estimatedTokens);
                    subChunks = splitLongText(chunk.getContent(), maxTokens);
                    log.info("二次分割完成: 原始1块 -> {}块", subChunks.size());
                } else {
                    subChunks = List.of(chunk.getContent());
                }

                // 为每个子分块创建 Document
                for (int j = 0; j < subChunks.size(); j++) {
                    String subContent = subChunks.get(j);
                    Map<String, Object> metadata = new HashMap<>();

                    // 基础元数据
                    metadata.put("documentId", Base62.encode(String.valueOf(documentId)));
                    metadata.put("userId", Base62.encode(String.valueOf(userId)));
                    metadata.put("title", document.getTitle());
                    metadata.put("type", document.getType());
                    metadata.put("chunkIndex", globalChunkIndex++);
                    metadata.put("timestamp", System.currentTimeMillis());

                    // 🆕 智能分块元数据
                    metadata.put("chunkType", chunk.getType());
                    metadata.put("tokenCount", estimateTokenCount(subContent));

                    // 如果是二次分割的子块，标记原始分块索引
                    if (subChunks.size() > 1) {
                        metadata.put("originalChunkIndex", i);
                        metadata.put("subChunkIndex", j);
                    }

                    // 🆕 章节信息(如果是章节分块)
                    if ("section".equals(chunk.getType())) {
                        metadata.put("sectionTitle", chunk.getSectionTitle());
                        metadata.put("sectionLevel", chunk.getSectionLevel());
                    }

                    documents.add(new Document(subContent, metadata));
                }
            }

            log.info("文档分块处理完成: 原始分块={}, 最终分块={}", smartChunks.size(), documents.size());

            // 🔒 最终安全检查: 确保所有分块都不超过限制
            int safeMaxTokens = 6000;
            List<Document> safeDocuments = new ArrayList<>();
            for (Document doc : documents) {
                int tokens = estimateTokenCount(doc.getText());
                if (tokens > safeMaxTokens) {
                    log.error("发现超限分块! tokens={}, 内容预览: {}",
                            tokens, doc.getText().substring(0, Math.min(100, doc.getText().length())));
                    // 跳过此分块,避免导致整个批次失败
                    continue;
                }
                safeDocuments.add(doc);
            }

            if (safeDocuments.size() < documents.size()) {
                log.warn("过滤了 {} 个超限分块,剩余 {} 个安全分块",
                        documents.size() - safeDocuments.size(), safeDocuments.size());
            }

            // 3. 向量化并存储 - 分批处理以符合阿里云嵌入模型的批量大小限制(最多10个)
            // 优化：记录每个向量的ID和分块内容到数据库
            int batchSize = 10; // 阿里云 text-embedding-v4 限制
            int totalBatches = (int) Math.ceil((double) safeDocuments.size() / batchSize);

            for (int i = 0; i < safeDocuments.size(); i += batchSize) {
                int end = Math.min(i + batchSize, safeDocuments.size());
                List<Document> batch = safeDocuments.subList(i, end);
                int currentBatch = (i / batchSize) + 1;

                log.info("向量化批次 {}/{}: 处理 {} 个文档块", currentBatch, totalBatches, batch.size());

                // 最后再检查一次批次中的每个文档
                boolean batchSafe = true;
                for (Document doc : batch) {
                    int tokens = estimateTokenCount(doc.getText());
                    if (tokens > safeMaxTokens) {
                        log.error("批次检查失败! 发现超限文档: tokens={}", tokens);
                        batchSafe = false;
                        break;
                    }
                }

                if (!batchSafe) {
                    log.warn("跳过不安全的批次 {}/{}", currentBatch, totalBatches);
                    continue;
                }

                // 添加到向量库（向量库会为每个Document生成ID）
                vectorStore.add(batch);

                // 保存向量ID映射和分块内容到数据库
                for (Document doc : batch) {
                    try {
                        Integer chunkIndex = (Integer) doc.getMetadata().get("chunkIndex");
                        String vectorId = doc.getId();

                        // 保存向量ID映射（用于后续删除）
                        DocumentVectorDO vectorMapping = DocumentVectorDO.builder()
                                .documentId(documentId)
                                .vectorId(vectorId)
                                .chunkIndex(chunkIndex)
                                .createTime(LocalDateTime.now())
                                .build();
                        documentVectorMapper.insert(vectorMapping);

                        // 保存分块内容到document_chunks表
                        DocumentChunkDO chunk = DocumentChunkDO.builder()
                                .documentId(documentId)
                                .chunkIndex(chunkIndex)
                                .content(doc.getText())
                                .vectorId(vectorId)
                                .tokens(doc.getText().length() / 4) // 粗略估算token数
                                .metadata(doc.getMetadata())
                                .createTime(LocalDateTime.now())
                                .isDeleted(0)
                                .build();
                        documentChunkMapper.insert(chunk);

                    } catch (Exception e) {
                        log.warn("保存文档块数据失败: documentId={}, vectorId={}", documentId, doc.getId(), e);
                        // 不影响主流程，向量已经存储
                    }
                }
            }

            log.info("文档向量化完成: id={}, vectors={}", documentId, documents.size());

            // 🆕 同步到 Elasticsearch 全文索引 (用于混合搜索)
            try {
                documentESService.indexDocument(
                        documentId,
                        userId,
                        document.getTitle(),
                        content, // 完整内容用于全文搜索
                        document.getType(),
                        document.getFileSize(),
                        "completed");
                log.info("文档已同步到ES全文索引: documentId={}", documentId);
            } catch (Exception esError) {
                log.warn("同步ES索引失败(不影响主流程): documentId={}, error={}",
                        documentId, esError.getMessage());
                // 不抛异常,避免影响向量存储主流程
            }

        } catch (Exception e) {
            log.error("文档向量化失败: id={}", documentId, e);
            throw new BusinessException("文档向量化失败: " + e.getMessage());
        }
    }

    @Override
    public List<Document> searchSimilarDocuments(String query, int topK) {
        try {
            // 获取当前用户ID
            Long userId = StpUtil.getLoginIdAsLong();

            // 获取配置的相似度阈值
            double threshold = ragConfig.getRetrieval().getSimilarityThreshold();
            log.info("开始文档检索: userId={}, query={}, topK={}, similarityThreshold={}",
                    userId, query, topK, threshold);

            // 🆕 构建用户过滤条件 (只返回当前用户的文档)
            Filter.Expression userFilter = new FilterExpressionBuilder()
                    .eq("userId", Base62.encode(String.valueOf(userId)))
                    .build();

            // 构建检索请求
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(threshold)
                    .filterExpression(userFilter) // 添加用户过滤
                    .build();

            // 执行向量检索
            List<Document> results = vectorStore.similaritySearch(request);

            if (results.isEmpty()) {
                log.warn("未找到相似文档: query={}, threshold={}, 建议降低相似度阈值或检查向量库中是否有数据",
                        query, threshold);
                return results;
            }
            // 输出检索结果详情（包含相似度得分）
            log.info("文档检索完成: query={}, results={}, threshold={}", query, results.size(), threshold);
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                if (doc.getText() != null) {
                    log.debug("检索结果[{}]: score={}, content={}, metadata={}",
                            i, doc.getScore(),
                            doc.getText().substring(0, Math.min(100, doc.getText().length())),
                            doc.getMetadata());
                }
            }

            return results;
        } catch (Exception e) {
            log.error("文档检索失败: query={}", query, e);
            throw new BusinessException("文档检索失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        // 权限验证
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权删除该文档");
        }

        try {
            // 1. 从向量库删除（优化：使用映射表快速获取向量ID）
            try {
                // 从映射表查询该文档的所有向量ID
                List<DocumentVectorDO> vectorMappings = documentVectorMapper.selectList(
                        new LambdaQueryWrapper<DocumentVectorDO>()
                                .eq(DocumentVectorDO::getDocumentId, documentId));

                if (!vectorMappings.isEmpty()) {
                    // 提取向量ID列表
                    List<String> vectorIds = vectorMappings.stream()
                            .map(DocumentVectorDO::getVectorId)
                            .toList();

                    // 批量删除向量
                    vectorStore.delete(vectorIds);
                    log.info("从向量库删除文档向量: documentId={}, count={}", documentId, vectorIds.size());

                    // 删除映射记录
                    documentVectorMapper.delete(new LambdaQueryWrapper<DocumentVectorDO>()
                            .eq(DocumentVectorDO::getDocumentId, documentId));

                    // 同步删除 ES 全文索引
                    try {
                        documentESService.deleteDocument(documentId);
                        log.info("从ES删除文档索引: documentId={}", documentId);
                    } catch (Exception esError) {
                        log.warn("删除ES索引失败: documentId={}, error={}", documentId, esError.getMessage());
                    }

                    // 同步删除知识图谱数据
                    try {
                        knowledgeGraphBuilderService.deleteGraphForDocument(documentId);
                        log.info("从Neo4j删除文档知识图谱: documentId={}", documentId);
                    } catch (Exception graphError) {
                        log.warn("删除知识图谱失败: documentId={}, error={}", documentId, graphError.getMessage());
                    }
                } else {
                    log.warn("未找到文档向量映射记录，尝试通过metadata查询删除: documentId={}", documentId);

                    // 后备方案：通过metadata查询删除（兼容旧数据）
                    SearchRequest searchRequest = SearchRequest.builder()
                            .query("") // 空查询
                            .topK(1000) // 获取尽可能多的结果
                            .similarityThreshold(0.0) // 最低阈值
                            .filterExpression(new FilterExpressionBuilder().eq("documentId",
                                    Base62.encode(String.valueOf(documentId))).build())
                            .build();

                    List<Document> documents = vectorStore.similaritySearch(searchRequest);
                    if (!documents.isEmpty()) {
                        vectorStore.delete(documents.stream()
                                .map(Document::getId)
                                .toList());
                        log.info("通过metadata删除文档向量: documentId={}, count={}", documentId, documents.size());
                    }
                }
            } catch (Exception e) {
                log.error("从向量库删除文档失败: documentId={}", documentId, e);
                // 向量删除失败不影响数据库删除
            }
            documentMapper.deleteById(documentId);
            log.info("逻辑删除文档: documentId={}", documentId);

            // 3. 更新知识库文档数量(递减)
            if (document.getKbId() != null) {
                try {
                    // 使用SQL递减操作,避免并发问题
                    knowledgeBaseMapper.update(null,
                            new UpdateWrapper<KnowledgeBaseDO>()
                                    .setSql("document_count = GREATEST(document_count - 1, 0)")
                                    .eq("id", document.getKbId()));
                    log.info("更新知识库文档数量: kbId={}", document.getKbId());
                } catch (Exception e) {
                    log.warn("更新知识库文档数量失败: kbId={}, error={}", document.getKbId(), e.getMessage());
                }
            }
            
            // 4. 从文件存储删除
            try {
                fileStorageService.deleteFile(document.getFileUrl());
                log.info("从文件存储删除文件: documentId={}, fileUrl={}", documentId, document.getFileUrl());
            } catch (Exception e) {
                log.warn("删除文件失败: fileUrl={}", document.getFileUrl(), e);
                // 文件删除失败不影响整体流程
            }

            log.info("文档删除成功: id={}", documentId);
        } catch (Exception e) {
            log.error("文档删除失败: id={}", documentId, e);
            throw new BusinessException("文档删除失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件大小
        long maxSize = ragConfig.getDocument().getMaxFileSize() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制: " + ragConfig.getDocument().getMaxFileSize() + "MB");
        }

        // 验证文件类型
        String filename = file.getOriginalFilename();
        if (StrUtil.isBlank(filename)) {
            throw new BusinessException("文件名不能为空");
        }

        // 检查文件扩展名
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new BusinessException("无法识别的文件类型：文件缺少扩展名");
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        if (!ragConfig.getDocument().getAllowedTypesList().contains(extension)) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        // 验证是否已上传（检查当前用户是否已有同名且同大小的文件）
        Long currentUserId = StpUtil.getLoginIdAsLong();
        DocumentDO existingDoc = documentMapper.selectOne(
                new LambdaQueryWrapper<DocumentDO>()
                        .eq(DocumentDO::getUserId, currentUserId)
                        .eq(DocumentDO::getTitle, filename)
                        .eq(DocumentDO::getFileSize, file.getSize())
                        .eq(DocumentDO::getIsDeleted, 0)
                        .last("limit 1"));

        if (existingDoc != null) {
            throw new BusinessException("文件已存在: " + filename + " (大小: " + file.getSize() + " 字节)");
        }
    }

    /**
     * 转换为 VO
     */
    private DocumentVO convertToVO(DocumentDO document) {
        return DocumentVO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .type(document.getType())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    /**
     * RAG-05: 重建文档向量索引
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reindexDocument(Long documentId) {
        log.info("开始重建文档向量索引: documentId={}", documentId);

        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || (document.getIsDeleted() != null && document.getIsDeleted() == 1)) {
            throw new BusinessException("文档不存在");
        }

        // 权限验证
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作该文档");
        }

        try {
            // 标记处理中
            document.setStatus("processing");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            // 1. 先清除旧向量
            purgeDocumentVectors(documentId);

            // 2. 重新解析文档（使用现有方法）
            String content = parseDocument(documentId);

            if (StrUtil.isBlank(content)) {
                throw new BusinessException("文档内容为空，无法建立索引");
            }

            // 3. 重新向量化
            vectorizeAndStore(documentId, content, document.getUserId());

            // 4. 更新文档状态
            document.setStatus("completed");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);

            // 5. 查询新向量数量
            int vectorCount = documentVectorMapper.selectCount(
                    new LambdaQueryWrapper<DocumentVectorDO>()
                            .eq(DocumentVectorDO::getDocumentId, documentId))
                    .intValue();

            // 6. 重建知识图谱
            try {
                log.info("开始重建知识图谱: documentId={}", documentId);
                // 先删除旧的知识图谱数据
                knowledgeGraphBuilderService.deleteGraphForDocument(documentId);
                // 重新构建知识图谱
                knowledgeGraphBuilderService.buildGraphForDocument(
                        documentId,
                        document.getTitle(),
                        content,
                        document.getUserId(),
                        document.getKbId()
                );
                log.info("知识图谱重建成功: documentId={}", documentId);
            } catch (Exception graphError) {
                log.warn("知识图谱重建失败(不影响主流程): documentId={}, error={}",
                        documentId, graphError.getMessage());
                // 知识图谱重建失败不影响向量索引重建
            }

            log.info("文档向量索引重建成功: documentId={}, vectorCount={}", documentId, vectorCount);
            return vectorCount;

        } catch (Exception e) {
            log.error("重建文档向量索引失败: documentId={}", documentId, e);
            document.setStatus("failed");
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            throw new BusinessException("重建索引失败: " + e.getMessage());
        }
    }

    /**
     * RAG-05: 清除文档向量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeDocumentVectors(Long documentId) {
        log.info("开始清除文档向量: documentId={}", documentId);

        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || (document.getIsDeleted() != null && document.getIsDeleted() == 1)) {
            throw new BusinessException("文档不存在");
        }

        // 权限验证
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作该文档");
        }

        int deletedCount = 0;

        try {
            // 从映射表查询向量ID
            List<DocumentVectorDO> vectorMappings = documentVectorMapper.selectList(
                    new LambdaQueryWrapper<DocumentVectorDO>()
                            .eq(DocumentVectorDO::getDocumentId, documentId));

            if (!vectorMappings.isEmpty()) {
                // 提取向量ID
                List<String> vectorIds = vectorMappings.stream()
                        .map(DocumentVectorDO::getVectorId)
                        .toList();

                // 从向量库删除
                vectorStore.delete(vectorIds);
                deletedCount = vectorIds.size();

                // 删除映射记录
                documentVectorMapper.delete(
                        new LambdaQueryWrapper<DocumentVectorDO>()
                                .eq(DocumentVectorDO::getDocumentId, documentId));

                // 删除块记录
                documentChunkMapper.delete(
                        new LambdaQueryWrapper<DocumentChunkDO>()
                                .eq(DocumentChunkDO::getDocumentId, documentId));

                log.info("文档向量清除成功: documentId={}, count={}", documentId, deletedCount);
            } else {
                log.info("文档没有向量记录: documentId={}", documentId);
            }

            return deletedCount;

        } catch (Exception e) {
            log.error("清除文档向量失败: documentId={}", documentId, e);
            throw new BusinessException("清除向量失败: " + e.getMessage());
        }
    }

    /**
     * RAG-05: 批量重建向量索引
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchReindexDocuments(List<Long> documentIds) {
        log.info("开始批量重建向量索引: documentIds={}", documentIds);

        if (documentIds == null || documentIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long documentId : documentIds) {
            try {
                reindexDocument(documentId);
                successCount++;
            } catch (Exception e) {
                log.error("重建文档索引失败: documentId={}", documentId, e);
                errors.add("文档 " + documentId + ": " + e.getMessage());
            }
        }

        log.info("批量重建完成: total={}, success={}, failed={}",
                documentIds.size(), successCount, documentIds.size() - successCount);

        if (!errors.isEmpty()) {
            throw new BusinessException("部分文档重建失败: " + String.join("; ", errors));
        }

        return successCount;
    }

    /**
     * 获取文档文件用于预览或下载
     */
    @Override
    public DocumentFileVO getDocumentFile(Long documentId) {
        log.info("获取文档文件: documentId={}", documentId);

        // 1. 查询文档记录
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || (document.getIsDeleted() != null && document.getIsDeleted() == 1)) {
            throw new BusinessException("文档不存在");
        }

        // 2. 权限验证
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!document.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权访问该文档");
        }

        try {
            // 3. 从存储服务下载文件
            byte[] fileData = fileStorageService.downloadFile(document.getFileUrl());

            // 4. 确定内容类型
            String filename = document.getTitle();
            String contentType = determineContentType(filename);

            log.info("文档文件获取成功: documentId={}, filename={}, size={}",
                    documentId, filename, fileData.length);

            return DocumentFileVO.builder()
                    .filename(filename)
                    .contentType(contentType)
                    .data(fileData)
                    .build();

        } catch (Exception e) {
            log.error("获取文档文件失败: documentId={}", documentId, e);
            throw new BusinessException("获取文档文件失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件名确定内容类型
     */
    private String determineContentType(String filename) {
        if (StrUtil.isBlank(filename)) {
            return "application/octet-stream";
        }

        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "application/octet-stream";
        }

        String extension = filename.substring(dot + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            case "md", "markdown" -> "text/markdown";
            case "html", "htm" -> "text/html";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }

    /**
     * 🆕 混合搜索 - 结合向量检索和全文检索(RRF融合)
     * 
     * <p>
     * 算法流程:
     * </p>
     * <ol>
     * <li>向量检索: 语义理解,召回相关文档</li>
     * <li>全文检索: BM25算法,召回关键词匹配文档</li>
     * <li>RRF融合: 融合两种检索结果,提高准确率</li>
     * </ol>
     * 
     * <p>
     * RRF (Reciprocal Rank Fusion) 公式:
     * </p>
     * 
     * <pre>
     * score(doc) = Σ [1 / (k + rank_i)]
     * k = 60 (常数,降低高排名文档的权重差异)
     * </pre>
     * 
     * @param query 搜索查询
     * @param topK  最终返回数量
     * @return 融合后的搜索结果(含高亮信息)
     */
    public List<DocumentSearchResultVO> hybridSearch(String query, int topK) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            // 1. 向量检索 (召回 topK*2 个候选)
            List<Document> vectorResults = searchSimilarDocuments(query, topK * 2);
            log.info("向量检索完成: results={}", vectorResults.size());

            // 2. ES 全文检索 (召回 topK*2 个候选)
            List<DocumentSearchResultVO> fulltextResults = documentESService.fullTextSearchWithHighlight(userId, null, query,
                    topK * 2);
            log.info("全文检索完成: results={}", fulltextResults.size());

            // 3. RRF 融合算法
            final int K = 60; // RRF 常数
            var rrfScores = new java.util.HashMap<Long, Double>();

            // 3.1 向量检索结果加权
            for (int i = 0; i < vectorResults.size(); i++) {
                Document doc = vectorResults.get(i);
                Long docId = extractDocumentId(doc);
                if (docId != null) {
                    double rrfScore = 1.0 / (K + i + 1); // rank从0开始,+1转为从1开始
                    rrfScores.merge(docId, rrfScore, Double::sum);
                }
            }

            // 3.2 全文检索结果加权
            for (int i = 0; i < fulltextResults.size(); i++) {
                Long docId = fulltextResults.get(i).getDocumentId();
                double rrfScore = 1.0 / (K + i + 1);
                rrfScores.merge(docId, rrfScore, Double::sum);
            }

            // 4. 按 RRF 分数排序,取 topK
            List<Long> rankedDocIds = rrfScores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // 降序
                    .limit(topK)
                    .map(java.util.Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toList());

            // 5. 构建最终结果(优先使用全文检索结果,因为它有高亮信息)
            List<DocumentSearchResultVO> finalResults = new ArrayList<>();
            for (Long docId : rankedDocIds) {
                // 优先从全文检索结果中查找
                fulltextResults.stream()
                        .filter(r -> r.getDocumentId().equals(docId))
                        .findFirst()
                        .ifPresentOrElse(
                                result -> {
                                    result.setScore(rrfScores.get(docId)); // 更新为 RRF 分数
                                    result.setSource("hybrid"); // 标记为混合搜索
                                    finalResults.add(result);
                                },
                                () -> {
                                    // 如果全文检索中没有,从向量检索中提取
                                    vectorResults.stream()
                                            .filter(doc -> docId.equals(extractDocumentId(doc)))
                                            .findFirst()
                                            .ifPresent(doc -> {
                                                DocumentSearchResultVO result = buildResultFromVectorDoc(doc, query,
                                                        rrfScores.get(docId));
                                                // 只有成功构建结果才添加
                                                if (result != null) {
                                                    finalResults.add(result);
                                                }
                                            });
                                });
            }

            log.info("混合搜索完成: query={}, vectorResults={}, fulltextResults={}, fusedResults={}",
                    query, vectorResults.size(), fulltextResults.size(), finalResults.size());

            return finalResults;

        } catch (Exception e) {
            log.error("混合搜索失败: query={}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * 估算文本的 token 数量
     * 保守估算策略:
     * - 中文字符: 1.2 tokens/字 (考虑标点和特殊字符)
     * - 英文单词: 1.5 tokens/词 (考虑长单词会被分割)
     * - 数字和符号: 1.5 tokens/字符
     * - 总是向上取整并添加 20% 安全边际
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;
        int chineseChars = 0;
        int otherChars = 0;
        int englishWords = 0;

        // 统计不同类型的字符
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                // 中文字符
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                // 非空白字符(英文、数字、符号等)
                otherChars++;
            }
        }

        // 估算英文单词数
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.matches(".*[a-zA-Z].*")) {
                englishWords++;
            }
        }

        // 保守估算 (向上取整)
        totalTokens += (int) Math.ceil(chineseChars * 1.2); // 中文: 1.2 tokens/字
        totalTokens += (int) Math.ceil(englishWords * 1.5); // 英文: 1.5 tokens/词
        totalTokens += (int) Math.ceil(otherChars * 0.5); // 其他字符

        // 添加 20% 安全边际
        totalTokens = (int) Math.ceil(totalTokens * 1.2);

        return totalTokens;
    }

    /**
     * 分割过长的文本为多个子块
     * 按句子边界分割,尽量保持语义完整性
     * 使用更保守的阈值确保不会超限
     */
    private List<String> splitLongText(String text, int maxTokens) {
        List<String> chunks = new ArrayList<>();

        // 使用更保守的分割阈值 (70% 而不是 90%)
        int safeMaxTokens = (int) (maxTokens * 0.7);

        // 按句子分割(支持中英文句子)
        String[] sentences = text.split("(?<=[。！？\\.!?])\\s*");

        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }

            int sentenceTokens = estimateTokenCount(sentence);

            // 单个句子超过限制,强制按字符分割
            if (sentenceTokens > safeMaxTokens) {
                // 先保存当前累积的分块
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }

                // 按更小的单位分割超长句子
                // 每次取 500 个字符(约 600 tokens)
                int charLimit = 500;
                for (int i = 0; i < sentence.length(); i += charLimit) {
                    int end = Math.min(i + charLimit, sentence.length());
                    String subSentence = sentence.substring(i, end);

                    // 确保子句不超过限制
                    if (estimateTokenCount(subSentence) > safeMaxTokens) {
                        // 继续减半
                        int halfLimit = charLimit / 2;
                        for (int j = i; j < end; j += halfLimit) {
                            int halfEnd = Math.min(j + halfLimit, end);
                            chunks.add(sentence.substring(j, halfEnd).trim());
                        }
                    } else {
                        chunks.add(subSentence.trim());
                    }
                }
                continue;
            }

            // 检查添加当前句子是否会超过限制
            if (currentTokens + sentenceTokens > safeMaxTokens) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
            }

            // 添加句子到当前分块
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
            }
            currentChunk.append(sentence);
            currentTokens += sentenceTokens;
        }

        // 添加最后一个分块
        if (currentChunk.length() > 0) {
            String lastChunk = currentChunk.toString().trim();
            if (!lastChunk.isEmpty()) {
                chunks.add(lastChunk);
            }
        }

        // 安全检查: 确保所有分块都不超过限制
        List<String> safeChunks = new ArrayList<>();
        for (String chunk : chunks) {
            if (estimateTokenCount(chunk) > safeMaxTokens) {
                // 如果还是超限,按字符强制截断
                log.warn("分块仍然超限,强制截断: estimatedTokens={}", estimateTokenCount(chunk));
                int charLimit = 400; // 更保守的限制
                for (int i = 0; i < chunk.length(); i += charLimit) {
                    int end = Math.min(i + charLimit, chunk.length());
                    safeChunks.add(chunk.substring(i, end).trim());
                }
            } else {
                safeChunks.add(chunk);
            }
        }

        // 如果最终还是没有分块(不应该发生),返回截断的文本
        if (safeChunks.isEmpty()) {
            log.error("文本分割失败,使用截断策略");
            int charLimit = 400;
            for (int i = 0; i < text.length(); i += charLimit) {
                int end = Math.min(i + charLimit, text.length());
                safeChunks.add(text.substring(i, end).trim());
            }
        }

        return safeChunks;
    }

    /**
     * 从 Spring AI Document 中提取文档 ID
     */
    private Long extractDocumentId(Document doc) {
        try {
            // 从 metadata 中获取 Base62 编码的 documentId
            Object documentIdObj = doc.getMetadata().get("documentId");
            if (documentIdObj == null) {
                log.warn("文档 metadata 中缺少 documentId: vectorId={}", doc.getId());
                return null;
            }

            String encodedId = documentIdObj.toString();
            // Base62.decode() 返回 byte[], 需要先转为 String 再转 Long
            String decodedStr = new String(Base62.decode(encodedId));
            return Long.valueOf(decodedStr);
        } catch (Exception e) {
            log.warn("解析文档ID失败: vectorId={}, metadata={}", doc.getId(), doc.getMetadata(), e);
            return null;
        }
    }

    /**
     * 从向量检索结果构建 DocumentSearchResultVO
     */
    private DocumentSearchResultVO buildResultFromVectorDoc(Document doc, String query, Double rrfScore) {
        Long docId = extractDocumentId(doc);
        if (docId == null) {
            log.warn("无法提取文档ID,跳过该结果: vectorId={}", doc.getId());
            return null;
        }

        String content = doc.getText();

        // 提取关键词
        List<String> keywords = Arrays.stream(query.trim().split("\\s+"))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        // 提取片段
        String snippet = content.length() > 300
                ? content.substring(0, 300) + "..."
                : content;

        return DocumentSearchResultVO.builder()
                .documentId(docId)
                .title(doc.getMetadata().getOrDefault("title", "未知标题").toString())
                .type(doc.getMetadata().getOrDefault("type", "unknown").toString())
                .fileSize(null) // 向量库中未存储文件大小
                .snippet(snippet)
                .keywords(keywords)
                .score(rrfScore)
                .source("hybrid")
                .build();
    }
}
