package com.GeekPaperAssistant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Elasticsearch 配置属性
 *
 * <p>只负责属性绑定，不包含Bean定义</p>
 * <p>通过 @EnableConfigurationProperties 在配置类中启用</p>
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Data
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    /**
     * Elasticsearch节点URI列表
     */
    private List<String> uris;

    /**
     * 连接超时时间
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * 读取超时时间
     */
    private Duration socketTimeout = Duration.ofSeconds(60);
}
