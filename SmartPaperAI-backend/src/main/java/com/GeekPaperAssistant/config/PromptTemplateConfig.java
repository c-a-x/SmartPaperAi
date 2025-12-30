package com.GeekPaperAssistant.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 提示词模板配置类
 * 使用Spring AI官方推荐模式：将提示词存储在resources/prompts/目录下
 *
 * <p>优点：</p>
 * <ul>
 *   <li>提示词与代码分离，便于独立修改和版本管理</li>
 *   <li>支持多语言和A/B测试（通过创建不同的提示词文件）</li>
 *   <li>减少Java文件大小，提升可维护性</li>
 *   <li>遵循Spring AI官方文档最佳实践</li>
 * </ul>
 *
 * @author 席崇援
 * @since 2025-10-08
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/prompt.html">Spring AI Prompt Documentation</a>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PromptTemplateConfig {

    private final ResourceLoader resourceLoader;

    /**
     * 从classpath加载提示词模板文件
     *
     * @param filename 文件名（不含路径，如 "rag-qa.st"）
     * @return 文件内容字符串
     */
    private String loadPromptTemplate(String filename) {
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/" + filename);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载提示词模板失败: {}", filename, e);
            throw new RuntimeException("Failed to load prompt template: " + filename, e);
        }
    }

    @Bean("ragQAPromptTemplate")
    public String ragQAPromptTemplate() {
        return loadPromptTemplate("rag-qa.st");
    }

    @Bean("documentQAPromptTemplate")
    public String documentQAPromptTemplate() {
        return loadPromptTemplate("document-qa.st");
    }

    @Bean("teachingPlanPromptTemplate")
    public String teachingPlanPromptTemplate() {
        return loadPromptTemplate("teaching-plan.st");
    }

    @Bean("sessionTitlePromptTemplate")
    public String sessionTitlePromptTemplate() {
        return loadPromptTemplate("session-title.st");
    }

    @Bean("keywordExtractionPromptTemplate")
    public String keywordExtractionPromptTemplate() {
        return loadPromptTemplate("keyword-extraction.st");
    }

    @Bean("paperSummaryPromptTemplate")
    public String paperSummaryPromptTemplate() {
        return loadPromptTemplate("paper-summary.st");
    }

    @Bean("paperMetadataExtractionPromptTemplate")
    public String paperMetadataExtractionPromptTemplate() {
        return loadPromptTemplate("paper-metadata-extraction.st");
    }

    @Bean("innovationExtractionPromptTemplate")
    public String innovationExtractionPromptTemplate() {
        return loadPromptTemplate("innovation-extraction.st");
    }

    @Bean("innovationClusteringPromptTemplate")
    public String innovationClusteringPromptTemplate() {
        return loadPromptTemplate("innovation-clustering.st");
    }

    @Bean("paperComparisonPromptTemplate")
    public String paperComparisonPromptTemplate() {
        return loadPromptTemplate("paper-comparison.st");
    }

    @Bean("literatureReviewPromptTemplate")
    public String literatureReviewPromptTemplate() {
        return loadPromptTemplate("literature-review.st");
    }
}
