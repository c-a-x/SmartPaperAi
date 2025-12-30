package com.GeekPaperAssistant.config;

/**
 * 向量与检索元数据字段常量
 * 统一管理，避免不同地方使用 score/similarity 混乱
 */
public final class MetadataKeys {
    private MetadataKeys() {}

    public static final String SIMILARITY = "similarity"; // 相似度分值（越大越相关）
    public static final String SCORE = "score";            // 某些 VectorStore 可能返回的得分键
    public static final String DOCUMENT_ID = "documentId";
    public static final String USER_ID = "userId";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String TITLE = "title";
}
