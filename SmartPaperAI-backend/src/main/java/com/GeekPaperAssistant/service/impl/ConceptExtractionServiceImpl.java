package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.service.ConceptExtractionService;
import com.GeekPaperAssistant.utils.LLMJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 概念提取服务实现
 * 使用 LLM 从用户查询中提取关键概念
 *
 * @author Claude
 * @since 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConceptExtractionServiceImpl implements ConceptExtractionService {

    private final ChatClient ragChatClient;

    @Override
    public List<String> extractConcepts(String query) {
        return extractConcepts(query, 5);
    }

    @Override
    @Cacheable(value = "conceptExtraction", key = "#query + '_' + #maxConcepts")
    public List<String> extractConcepts(String query, int maxConcepts) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = String.format("""
                从以下用户问题中提取 %d 个最关键的学术概念或专业术语：

                问题：%s

                要求：
                1. 只提取专业术语、技术名词、学术概念
                2. 优先提取核心概念（如：机器学习、深度学习、神经网络）
                3. 忽略常用词（如：研究、方法、应用、技术等）
                4. 返回纯 JSON 数组格式，不要 markdown 标记
                5. 如果问题太简单或没有明显概念，返回空数组 []

                示例输出：
                ["深度学习", "卷积神经网络", "图像识别"]

                现在请提取概念：
                """, maxConcepts, query);

            log.debug("提取概念 Prompt: {}", prompt);

            String response = ragChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("LLM 响应: {}", response);

            // 解析 JSON 数组
            List<String> concepts = LLMJsonUtils.parseArray(response, String.class);

            // 过滤空字符串和过长的概念
            concepts = concepts.stream()
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .filter(c -> c.length() <= 50)  // 概念不应该太长
                    .limit(maxConcepts)
                    .toList();

            log.info("从查询中提取概念: query='{}', concepts={}", query, concepts);
            return concepts;

        } catch (Exception e) {
            log.warn("概念提取失败，返回空列表: query='{}', error={}", query, e.getMessage());
            return new ArrayList<>();
        }
    }
}
