package com.GeekPaperAssistant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * GROBID 专用 HTTP 客户端配置
 *
 * 设置合理的连接/读取超时，避免外部服务故障情况下阻塞业务线程。
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@RequiredArgsConstructor
public class GrobidHttpClientConfig {

    private final GrobidProperties grobidProperties;

    @Bean("grobidRestTemplate")
    public RestTemplate grobidRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 从配置文件读取超时时间
        factory.setConnectTimeout((int) grobidProperties.getConnectionTimeout().toMillis());
        factory.setReadTimeout((int) grobidProperties.getReadTimeout().toMillis());
        return new RestTemplate(factory);
    }
}
