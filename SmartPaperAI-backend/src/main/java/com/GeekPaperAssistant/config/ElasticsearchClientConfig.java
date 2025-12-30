package com.GeekPaperAssistant.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.GeekPaperAssistant.config.properties.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置
 *
 * <p>使用 @EnableConfigurationProperties 启用配置属性绑定</p>
 * <p>将配置属性和Bean定义分离，符合单一职责原则</p>
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchClientConfig {

    private final ElasticsearchProperties properties;

    @Bean
    public RestClient restClient() {
        HttpHost[] hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        RestClient client = RestClient.builder(hosts)
                .setRequestConfigCallback(builder -> builder
                        .setConnectTimeout((int) properties.getConnectionTimeout().toMillis())
                        .setSocketTimeout((int) properties.getSocketTimeout().toMillis()))
                .build();

        return client;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        // 配置 Jackson 支持 Java 8 时间类型并使用 ISO-8601 格式
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建传输层
        RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)
        );

        log.info("ElasticsearchClient 已初始化");
        return new ElasticsearchClient(transport);
    }
}
