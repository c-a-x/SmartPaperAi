package com.GeekPaperAssistant.config;

import com.GeekPaperAssistant.config.properties.RAGProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置类
 *
 * <p>使用 @EnableConfigurationProperties 启用配置属性绑定</p>
 * <p>将配置属性和Bean定义分离，符合单一职责原则</p>
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Configuration
@EnableConfigurationProperties(RAGProperties.class)
public class RAGConfiguration {
    // 如需要定义额外的Bean，可在此添加
    // 纯配置属性已移至 RAGProperties
}
