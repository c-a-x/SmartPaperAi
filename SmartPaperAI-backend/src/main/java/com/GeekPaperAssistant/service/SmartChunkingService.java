package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.DocumentMetadataVO;

import java.util.List;

/**
 * 智能文档分块服务接口
 * 
 * <p>支持多种分块策略:</p>
 * <ul>
 *   <li><b>语义分块</b>: 基于句子相似度,保持语义完整性</li>
 *   <li><b>章节分块</b>: 基于标题层级,保留文档结构</li>
 *   <li><b>混合分块</b>: 章节 + 重叠(overlap),兼顾结构和上下文</li>
 * </ul>
 * 
 * @author 席崇援
 */
public interface SmartChunkingService {
    
    /**
     * 智能分块 - 自动选择最佳策略
     * 
     * <p>决策逻辑:</p>
     * <ul>
     *   <li>有章节结构 → 章节分块 + overlap</li>
     *   <li>无章节结构 → 语义分块</li>
     * </ul>
     * 
     * @param content 文档内容
     * @param metadata 文档元数据(可选,用于章节分块)
     * @param chunkSize 目标块大小(token数)
     * @param overlapSize 重叠大小(token数)
     * @return 分块结果列表
     */
    List<ChunkResult> smartChunk(String content, DocumentMetadataVO metadata, int chunkSize, int overlapSize);
    
    /**
     * 章节分块 - 基于标题层级
     * 
     * <p>优势:</p>
     * <ul>
     *   <li>保留文档结构(标题-内容关系)</li>
     *   <li>每个块都是语义完整的章节</li>
     *   <li>添加 overlap 确保跨章节检索</li>
     * </ul>
     * 
     * @param metadata 文档元数据(必须包含章节信息)
     * @param overlapSize 章节间重叠大小(token数)
     * @return 分块结果列表
     */
    List<ChunkResult> chunkBySections(DocumentMetadataVO metadata, int overlapSize);
    
    /**
     * 语义分块 - 基于句子相似度
     * 
     * <p>算法:</p>
     * <ol>
     *   <li>将文本分割为句子</li>
     *   <li>计算句子间的语义相似度</li>
     *   <li>在相似度低的地方分块(语义断裂点)</li>
     * </ol>
     * 
     * @param content 文档内容
     * @param chunkSize 目标块大小(token数)
     * @param overlapSize 重叠大小(token数)
     * @return 分块结果列表
     */
    List<ChunkResult> semanticChunk(String content, int chunkSize, int overlapSize);
    
    /**
     * 分块结果
     */
    class ChunkResult {
        /**
         * 分块内容
         */
        private String content;
        
        /**
         * 分块类型: "section"=章节分块, "semantic"=语义分块
         */
        private String type;
        
        /**
         * 章节标题(章节分块时有值)
         */
        private String sectionTitle;
        
        /**
         * 章节层级(章节分块时有值)
         */
        private Integer sectionLevel;
        
        /**
         * 在原文档中的起始位置
         */
        private int startPosition;
        
        /**
         * 在原文档中的结束位置
         */
        private int endPosition;
        
        /**
         * Token 数量(估算)
         */
        private int tokenCount;
        
        // Constructors
        public ChunkResult() {}
        
        public ChunkResult(String content, String type, int startPosition, int endPosition) {
            this.content = content;
            this.type = type;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.tokenCount = estimateTokenCount(content);
        }
        
        // Getters & Setters
        public String getContent() { return content; }
        public void setContent(String content) { 
            this.content = content;
            this.tokenCount = estimateTokenCount(content);
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getSectionTitle() { return sectionTitle; }
        public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
        
        public Integer getSectionLevel() { return sectionLevel; }
        public void setSectionLevel(Integer sectionLevel) { this.sectionLevel = sectionLevel; }
        
        public int getStartPosition() { return startPosition; }
        public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
        
        public int getEndPosition() { return endPosition; }
        public void setEndPosition(int endPosition) { this.endPosition = endPosition; }
        
        public int getTokenCount() { return tokenCount; }
        public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
        
        /**
         * 估算 Token 数量(简化版: 字符数 / 4)
         * 中文: ~1.5字符/token
         * 英文: ~4字符/token
         */
        private int estimateTokenCount(String text) {
            if (text == null) return 0;
            // 检测是否包含中文
            boolean hasChinese = text.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FA5);
            return hasChinese ? (int)(text.length() / 1.5) : (int)(text.length() / 4.0);
        }
    }
}
