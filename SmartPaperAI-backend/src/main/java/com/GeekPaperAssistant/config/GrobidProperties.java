package com.GeekPaperAssistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * GROBID 服务配置属性
 *
 * @author ican
 * @since 2025-10-27
 */
@Data
@Component
@ConfigurationProperties(prefix = "grobid")
public class GrobidProperties {

    /**
     * 是否启用 GROBID
     */
    private boolean enabled = true;

    /**
     * GROBID 服务地址
     */
    private String serverUrl = "http://localhost:8070";

    /**
     * 连接超时时间（Duration 类型，支持 30s、3000ms 等格式）
     */
    private Duration connectionTimeout = Duration.ofSeconds(30);

    /**
     * 读取超时时间（Duration 类型，支持 60s、10000ms 等格式）
     */
    private Duration readTimeout = Duration.ofSeconds(60);
}
