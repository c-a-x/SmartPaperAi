package com.GeekPaperAssistant.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 相关配置，提供 RedisTemplate<String, Object>
 * 避免因三方 Redisson Starter 未暴露标准 RedisTemplate 而导致的注入失败。
 */
@Configuration
@EnableRedisRepositories(basePackages = {}) // 禁用Redis repositories自动扫描，避免与Neo4j repositories冲突
public class RedisConfig {

    /**
     * 提供默认的 RedisTemplate<String, Object>
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // key 序列化使用 String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // value 使用 String 序列化，确保与 StringRedisTemplate 兼容
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
