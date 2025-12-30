package com.GeekPaperAssistant.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author 席崇援
 * @since 2025-10-08
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {
    
    private final OpenAiChatModel openAiChatModel;

    private  MessageChatMemoryAdvisor messageChatMemoryAdvisor;
    
    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private Integer maxTokens;
    
    // ==================== ChatClient Bean 配置 ====================

    @Bean("normalChatClient")
    @Primary
    public ChatClient normalChatClient() {
        log.info("初始化普通聊天 ChatClient");

        return ChatClient.builder(openAiChatModel)
            .defaultOptions(OpenAiChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build())
            .defaultSystem("你是一个智能AI助手，请友好、专业地回答用户的问题。")
            .build();
    }
    
    /**
     * RAG 聊天的 ChatClient
     */
    @Bean("ragChatClient")
    public ChatClient ragChatClient() {
        log.info("初始化 RAG ChatClient");

        return ChatClient.builder(openAiChatModel)
            .defaultOptions(OpenAiChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build())
            .defaultSystem("""
                你是一个专业的AI助手，擅长基于提供的参考资料回答问题。

                回答原则：
                1. 准确性：回答必须基于参考资料，不要编造信息
                2. 完整性：回答要全面，覆盖问题的各个方面
                3. 清晰性：用简洁明了的语言表达
                4. 引用性：如果可能，标注信息来源
                5. 诚实性：如果参考资料中没有相关信息，请如实说明
                """)
            .build();
    }
}
