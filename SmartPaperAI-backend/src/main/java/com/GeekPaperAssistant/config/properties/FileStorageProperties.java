package com.GeekPaperAssistant.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性
 *
 * <p>只负责属性绑定，不包含Bean定义</p>
 * <p>通过 @EnableConfigurationProperties 在配置类中启用</p>
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Data
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 本地存储基础路径
     */
    private String basePath = "./uploads";

    /**
     * 是否启用 MinIO
     */
    private Boolean minioEnabled = false;

    /**
     * MinIO 配置
     */
    private MinioConfig minio = new MinioConfig();

    @Data
    public static class MinioConfig {
        /**
         * MinIO 服务地址
         */
        private String endpoint;

        /**
         * 访问密钥
         */
        private String accessKey;

        /**
         * 秘密密钥
         */
        private String secretKey;

        /**
         * 存储桶名称
         */
        private String bucket;
    }
}
