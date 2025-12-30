package com.GeekPaperAssistant.utils;

import cn.hutool.core.codec.Base62;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 工具类 - 统一管理 RAG 相关的工具方法
 *
 * <p>整合了 Token 估算、Filter 构建、关键词提取、元数据构建等功能，避免代码重复</p>
 *
 * @author ican
 * @since 2025-10-27
 */
public class RAGUtils {

    // ==================== Token Estimation ====================

    /**
     * 估算文本的 Token 数量
     *
     * <p>算法：</p>
     * <ul>
     *   <li>中文: 1.2 tokens/字</li>
     *   <li>英文: 1.5 tokens/词</li>
     *   <li>其他字符: 0.5 tokens/字符</li>
     *   <li>添加 20% 安全边际</li>
     * </ul>
     *
     * @param text 待估算的文本
     * @return 估算的 Token 数量
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;
        int chineseChars = 0;
        int otherChars = 0;
        int englishWords = 0;

        // 统计不同类型的字符
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                // 中文字符
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                // 非空白字符(英文、数字、符号等)
                otherChars++;
            }
        }

        // 估算英文单词数
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.matches(".*[a-zA-Z].*")) {
                englishWords++;
            }
        }

        // 保守估算 (向上取整)
        totalTokens += (int) Math.ceil(chineseChars * 1.2); // 中文: 1.2 tokens/字
        totalTokens += (int) Math.ceil(englishWords * 1.5); // 英文: 1.5 tokens/词
        totalTokens += (int) Math.ceil(otherChars * 0.5); // 其他字符

        // 添加 20% 安全边际
        totalTokens = (int) Math.ceil(totalTokens * 1.2);

        return totalTokens;
    }

    /**
     * 估算文本的 Token 数量（简化版）
     *
     * <p>适用于快速估算，不需要太精确的场景</p>
     * <ul>
     *   <li>中文: 1.5字符/token</li>
     *   <li>英文: 4字符/token</li>
     * </ul>
     *
     * @param text 待估算的文本
     * @return 估算的 Token 数量
     */
    public static int estimateTokenCountSimple(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 检测是否包含中文
        boolean hasChinese = text.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FA5);
        return hasChinese ? (int) (text.length() / 1.5) : (int) (text.length() / 4.0);
    }

    // ==================== Filter Building ====================

    /**
     * 构建用户 ID 过滤器
     *
     * @param userId 用户ID
     * @return Filter.Expression
     */
    public static Filter.Expression buildUserIdFilter(Long userId) {
        return new FilterExpressionBuilder()
                .eq("userId", Base62.encode(String.valueOf(userId)))
                .build();
    }

    /**
     * 构建文档 ID 过滤器
     *
     * @param documentId 文档ID
     * @return Filter.Expression
     */
    public static Filter.Expression buildDocumentIdFilter(Long documentId) {
        return new FilterExpressionBuilder()
                .eq("documentId", Base62.encode(String.valueOf(documentId)))
                .build();
    }

    /**
     * 构建文档和用户联合过滤器
     *
     * @param documentId 文档ID
     * @param userId     用户ID
     * @return Filter.Expression
     */
    public static Filter.Expression buildDocumentUserFilter(Long documentId, Long userId) {
        return new FilterExpressionBuilder().and(
                new FilterExpressionBuilder().eq("documentId", Base62.encode(String.valueOf(documentId))),
                new FilterExpressionBuilder().eq("userId", Base62.encode(String.valueOf(userId)))
        ).build();
    }

    /**
     * 构建多文档 ID 过滤器（OR 逻辑）
     *
     * @param documentIds 文档ID列表
     * @param userId      用户ID
     * @return Filter.Expression
     */
    public static Filter.Expression buildMultiDocumentFilter(List<Long> documentIds, Long userId) {
        if (documentIds == null || documentIds.isEmpty()) {
            return buildUserIdFilter(userId);
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op documentIdFilter = null;

        for (Long docId : documentIds) {
            FilterExpressionBuilder.Op docIdOp = builder.eq("documentId", Base62.encode(String.valueOf(docId)));
            documentIdFilter = (documentIdFilter == null) ? docIdOp : builder.or(documentIdFilter, docIdOp);
        }

        // 组合文档ID过滤和用户ID过滤
        return builder.and(
                builder.eq("userId", Base62.encode(String.valueOf(userId))),
                documentIdFilter
        ).build();
    }

    // ==================== Keyword Extraction ====================

    /**
     * 从查询文本中提取关键词（用于高亮显示）
     *
     * @param query 查询文本
     * @return 关键词列表
     */
    public static List<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return Arrays.stream(query.trim().split("\\s+"))
                .filter(kw -> !kw.isBlank())
                .distinct()
                .toList();
    }

    // ==================== Metadata Building ====================

    /**
     * 构建文档元数据
     *
     * @param documentId 文档ID
     * @param userId     用户ID
     * @param chunkIndex 块索引
     * @param title      标题
     * @param type       类型
     * @return 元数据Map
     */
    public static Map<String, Object> buildDocumentMetadata(
            Long documentId,
            Long userId,
            Integer chunkIndex,
            String title,
            String type) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", documentId);
        metadata.put("userId", userId);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("title", title != null ? title : "未知文档");
        metadata.put("type", type != null ? type : "unknown");
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }

    /**
     * 构建文档元数据（扩展版）
     *
     * @param documentId 文档ID
     * @param userId     用户ID
     * @param chunkIndex 块索引
     * @param title      标题
     * @param type       类型
     * @param tokenCount Token数量
     * @return 元数据Map
     */
    public static Map<String, Object> buildDocumentMetadata(
            Long documentId,
            Long userId,
            Integer chunkIndex,
            String title,
            String type,
            Integer tokenCount) {

        Map<String, Object> metadata = buildDocumentMetadata(documentId, userId, chunkIndex, title, type);
        if (tokenCount != null) {
            metadata.put("tokenCount", tokenCount);
        }
        return metadata;
    }
}
