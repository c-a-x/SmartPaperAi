package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档搜索结果 VO - 支持前端高亮
 * 
 * <p>设计说明:</p>
 * <ul>
 *   <li><b>snippet</b>: 原始文本片段(无HTML标签)</li>
 *   <li><b>keywords</b>: 高亮关键词列表,前端使用 Mark.js 渲染</li>
 *   <li><b>score</b>: ES 的 BM25 评分</li>
 * </ul>
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResultVO {
    
    /**
     * 文档ID
     */
    private Long documentId;
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 文档类型 (pdf, docx, txt...)
     */
    private String type;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 相关文本片段(原始文本,无HTML)
     */
    private String snippet;
    
    /**
     * 需要高亮的关键词列表
     * 前端使用 Mark.js 渲染高亮效果
     */
    private List<String> keywords;
    
    /**
     * 搜索相关性评分
     * - BM25 评分(全文搜索)
     * - 余弦相似度(向量搜索)
     * - RRF 融合评分(混合搜索)
     */
    private Double score;
    
    /**
     * 搜索来源类型
     * - "fulltext": ES 全文搜索
     * - "vector": 向量相似度搜索
     * - "hybrid": RRF 混合搜索
     */
    private String source;
}
