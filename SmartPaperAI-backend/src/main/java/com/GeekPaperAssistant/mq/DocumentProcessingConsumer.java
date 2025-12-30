package com.GeekPaperAssistant.mq;

import com.GeekPaperAssistant.config.RabbitMQConfig;
import com.GeekPaperAssistant.model.dto.DocumentProcessingMessage;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import com.GeekPaperAssistant.mapper.DocumentMapper;
import com.GeekPaperAssistant.service.DocumentService;
import com.GeekPaperAssistant.service.DocumentESService;
import com.GeekPaperAssistant.service.DocumentTaskService;
import com.GeekPaperAssistant.service.PaperAnalysisService;
import com.GeekPaperAssistant.service.KnowledgeGraphBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 文档处理消费者
 *
 * @author 席崇援
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingConsumer {

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;
    private final DocumentESService documentESService;
    private final DocumentTaskService documentTaskService;
    private final PaperAnalysisService paperAnalysisService;  // 用于自动抽取元数据
    private final KnowledgeGraphBuilderService knowledgeGraphBuilderService;  // 用于构建知识图谱
    
    /**
     * 处理文档
     * 流程: 解析 → 向量化 → ES索引 → 更新状态
     */
    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_PROCESSING_QUEUE)
    public void processDocument(DocumentProcessingMessage message) {
        log.info("收到文档处理消息: documentId={}, type={}", 
            message.getDocumentId(), message.getProcessingType());
        
        Long documentId = message.getDocumentId();
        Long taskId = message.getTaskId(); // 从消息中获取taskId
        DocumentDO document = null;
        
        try {
            // 1. 查询文档
            document = documentMapper.selectById(documentId);
            if (document == null) {
                log.error("文档不存在: documentId={}", documentId);
                if (taskId != null) {
                    documentTaskService.updateTaskStatus(taskId, "failed", 0, "文档不存在");
                }
                return;
            }
            
            // 2. 更新状态为处理中（进度20%）
            document.setStatus("processing");
            documentMapper.updateById(document);
            if (taskId != null) {
                documentTaskService.updateTaskStatus(taskId, "processing", 20, null);
            }
            log.info("开始处理文档: documentId={}, title={}", documentId, document.getTitle());
            
            // 3. 解析文档内容（进度40%）
            String content = documentService.parseDocument(documentId);
            if (taskId != null) {
                documentTaskService.updateTaskStatus(taskId, "processing", 40, null);
            }
            log.info("文档解析完成: documentId={}, contentLength={}", documentId, content.length());
            
            // 4. 向量化并存储到向量数据库（进度70%）
            documentService.vectorizeAndStore(documentId, content, document.getUserId());
            if (taskId != null) {
                documentTaskService.updateTaskStatus(taskId, "processing", 70, null);
            }
            log.info("文档向量化完成: documentId={}", documentId);
            
            // 5. 索引到Elasticsearch（进度90%）
            try {
                documentESService.indexDocument(
                    documentId,
                    document.getUserId(),
                    document.getTitle(),
                    content,
                    document.getType(),
                    document.getFileSize(),
                    "completed"
                );
                if (taskId != null) {
                    documentTaskService.updateTaskStatus(taskId, "processing", 90, null);
                }
                log.info("文档索引到ES完成: documentId={}", documentId);
            } catch (Exception esError) {
                log.error("ES索引失败，但不影响主流程: documentId={}", documentId, esError);
                // ES失败影响主流程，不继续执行
                //更新错误信息
                if (taskId != null) {
                    documentTaskService.updateTaskStatus(taskId, "failed", 90, "ES索引失败: " + esError.getMessage());
                }
                return;
            }
            
            // 6. 更新状态为完成（进度100%）
            document.setStatus("completed");
            documentMapper.updateById(document);
            if (taskId != null) {
                documentTaskService.updateTaskStatus(taskId, "completed", 100, null);
            }
            
            log.info("文档处理完成: documentId={}, title={}", documentId, document.getTitle());

            // 7. 异步抽取论文元数据并缓存到Redis(不阻塞主流程)
            asyncExtractAndCacheMetadata(documentId);

            // 8. 异步构建知识图谱（不阻塞主流程）
            asyncBuildKnowledgeGraph(documentId, document.getTitle(), content, document.getUserId(), document.getKbId());
            
        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", documentId, e);
            
            // 更新状态为失败
            try {
                if (document == null) {
                    document = documentMapper.selectById(documentId);
                }
                if (document != null) {
                    document.setStatus("failed");
                    documentMapper.updateById(document);
                    
                    // 同步更新ES状态
                    try {
                        documentESService.updateDocumentStatus(documentId, "failed");
                    } catch (Exception ignored) {
                        // 忽略ES更新失败
                    }
                }
                
                // 更新任务状态为失败
                if (taskId != null) {
                    documentTaskService.updateTaskStatus(taskId, "failed", 0, e.getMessage());
                }
            } catch (Exception updateError) {
                log.error("更新失败状态异常: documentId={}", documentId, updateError);
            }
        }
    }
    
    /**
     * 异步抽取并缓存论文元数据
     * 在文档处理完成后自动触发,提前缓存元数据以加速首次访问
     *
     * @param documentId 文档ID
     */
    @Async
    private void asyncExtractAndCacheMetadata(Long documentId) {
        try {
            log.info("开始异步抽取论文元数据: documentId={}", documentId);
            long startTime = System.currentTimeMillis();

            // 调用元数据抽取服务(会自动缓存到Redis)
            paperAnalysisService.extractPaperMetadata(documentId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("论文元数据抽取并缓存成功: documentId={}, 耗时={}ms", documentId, duration);

        } catch (Exception e) {
            // 元数据抽取失败不影响主流程,只记录日志
            log.warn("自动抽取论文元数据失败(不影响文档处理): documentId={}, error={}",
                documentId, e.getMessage());
        }
    }

    /**
     * 异步构建知识图谱
     * 在文档处理完成后自动触发，提取概念并构建图谱关系
     *
     * @param documentId 文档ID
     * @param title 文档标题
     * @param content 文档内容
     * @param userId 用户ID
     * @param kbId 知识库ID
     */
    @Async
    private void asyncBuildKnowledgeGraph(Long documentId, String title, String content, Long userId, Long kbId) {
        try {
            log.info("开始异步构建知识图谱: documentId={}", documentId);
            long startTime = System.currentTimeMillis();

            // 调用知识图谱构建服务
            var result = knowledgeGraphBuilderService.buildGraphForDocument(
                documentId, title, content, userId, kbId
            );

            long duration = System.currentTimeMillis() - startTime;

            if (result.getSuccess()) {
                log.info("知识图谱构建成功: documentId={}, 概念数={}, 新增概念={}, 关系数={}, 耗时={}ms",
                    documentId, result.getConceptCount(), result.getNewConceptCount(),
                    result.getRelationshipCount(), duration);
            } else {
                log.warn("知识图谱构建失败: documentId={}, error={}", documentId, result.getErrorMessage());
            }

        } catch (Exception e) {
            // 知识图谱构建失败不影响主流程,只记录日志
            log.warn("自动构建知识图谱失败(不影响文档处理): documentId={}, error={}",
                documentId, e.getMessage());
        }
    }
}
