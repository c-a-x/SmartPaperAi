package com.GeekPaperAssistant.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 
 * @author 席崇援
 * @since 2025-10-06
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {
    
    /**
     * 文档处理队列
     */
    public static final String DOCUMENT_PROCESSING_QUEUE = "ican.document.processing";
    
    /**
     * RAG 索引队列
     */
    public static final String RAG_INDEXING_QUEUE = "ican.rag.indexing";
    
    @Bean
    public Queue documentProcessingQueue() {
        return new Queue(DOCUMENT_PROCESSING_QUEUE, true);
    }
    
    @Bean
    public Queue ragIndexingQueue() {
        return new Queue(RAG_INDEXING_QUEUE, true);
    }
    
    /**
     * 消息转换器 - 使用JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

