package com.GeekPaperAssistant.mq;

import com.GeekPaperAssistant.config.RabbitMQConfig;
import com.GeekPaperAssistant.model.dto.DocumentProcessingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 文档处理生产者
 * 
 * @author 席崇援
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingProducer {
    
    private final RabbitTemplate rabbitTemplate;

    
    /**
     * 发送文档处理消息（新方法，支持完整处理流程）
     * 
     * @param documentId 文档ID
     * @param userId 用户ID
     */
    public void sendDocumentProcessingMessage(Long documentId, Long userId) {
        sendDocumentProcessingMessage(documentId, userId, null);
    }
    
    /**
     * 发送文档处理消息（带任务跟踪）
     * 
     * @param documentId 文档ID
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    public void sendDocumentProcessingMessage(Long documentId, Long userId, Long taskId) {
        DocumentProcessingMessage message = DocumentProcessingMessage.builder()
            .documentId(documentId)
            .userId(userId)
            .taskId(taskId)
            .processingType("full")
            .build();
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.DOCUMENT_PROCESSING_QUEUE, message);
        
        log.info("发送文档处理任务到队列: documentId={}, userId={}, taskId={}", documentId, userId, taskId);
    }
}

