package com.GeekPaperAssistant.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.continew.starter.core.exception.BusinessException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM JSON 响应清洗工具类
 * 专门处理 LLM 返回的非标准 JSON 格式
 *
 * @author 席崇援
 */
@Slf4j
public class LLMJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Markdown 代码块模式
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    // JSON 对象模式
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    // JSON 数组模式
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*]");

    /**
     * 清洗 LLM 返回的 JSON 响应
     * - 移除 Markdown 代码块标记
     * - 移除多余的空白和换行
     * - 处理转义字符
     *
     * @param rawResponse LLM 原始响应
     * @return 清洗后的 JSON 字符串
     */
    public static String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new BusinessException("LLM 响应为空");
        }

        String cleaned = rawResponse.trim();

        // 1. 移除 Markdown 代码块标记
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(cleaned);
        if (codeMatcher.find()) {
            cleaned = codeMatcher.group(1).trim();
        }

        // 2. 提取 JSON 内容（优先提取对象，其次提取数组）
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(cleaned);
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(cleaned);

        if (objectMatcher.find()) {
            cleaned = objectMatcher.group();
        } else if (arrayMatcher.find()) {
            cleaned = arrayMatcher.group();
        }

        // 3. 移除多余的空白字符（保留 JSON 内部结构）
        cleaned = cleaned
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\t", " ")
                .trim();

        return cleaned;
    }

    /**
     * 从 LLM 响应中提取 JSON 数组
     * 专门处理返回数组的情况
     *
     * @param text LLM 原始响应
     * @return 提取的 JSON 数组字符串
     */
    public static String extractJsonArray(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "[]";
        }

        String cleaned = text.trim();

        // 移除 Markdown 代码块
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(cleaned);
        if (codeMatcher.find()) {
            cleaned = codeMatcher.group(1).trim();
        }

        // 提取 JSON 数组
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(cleaned);
        if (arrayMatcher.find()) {
            return arrayMatcher.group().trim();
        }

        // 如果没有找到数组，返回空数组
        log.warn("未能从响应中提取 JSON 数组，返回空数组");
        return "[]";
    }

    /**
     * 清洗并解析 JSON 对象
     *
     * @param rawResponse LLM 原始响应
     * @param clazz       目标类型
     * @param <T>         泛型类型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String rawResponse, Class<T> clazz) {
        try {
            String cleaned = cleanJsonResponse(rawResponse);
            return OBJECT_MAPPER.readValue(cleaned, clazz);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", rawResponse, e);
            throw new BusinessException("JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 清洗并解析 JSON 数组
     *
     * @param rawResponse LLM 原始响应
     * @param clazz       元素类型
     * @param <T>         泛型类型
     * @return 解析后的列表
     */
    public static <T> List<T> parseArray(String rawResponse, Class<T> clazz) {
        try {
            String cleaned = extractJsonArray(rawResponse);
            return OBJECT_MAPPER.readValue(
                    cleaned,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (Exception e) {
            log.error("JSON 数组解析失败: {}", rawResponse, e);
            throw new BusinessException("JSON 数组解析失败: " + e.getMessage());
        }
    }

    /**
     * 清洗并解析 JSON（支持 TypeReference）
     *
     * @param rawResponse   LLM 原始响应
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 解析后的对象
     */
    public static <T> T parse(String rawResponse, TypeReference<T> typeReference) {
        try {
            String cleaned = cleanJsonResponse(rawResponse);
            return OBJECT_MAPPER.readValue(cleaned, typeReference);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", rawResponse, e);
            throw new BusinessException("JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 验证字符串是否为有效的 JSON
     *
     * @param jsonString JSON 字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
