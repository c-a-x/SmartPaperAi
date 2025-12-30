package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档元数据 VO - GROBID 解析结果
 * 
 * <p>包含从学术论文PDF中提取的结构化元数据</p>
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataVO {
    
    /**
     * 文档标题
     */
    private String title;
    
    /**
     * 作者列表
     */
    private List<Author> authors;
    
    /**
     * 摘要
     */
    private String abstractText;
    
    /**
     * 关键词
     */
    private List<String> keywords;
    
    /**
     * 章节结构
     */
    private List<Section> sections;
    
    /**
     * 引用文献数量
     */
    private Integer referenceCount;
    
    /**
     * 出版年份
     */
    private String publicationYear;
    
    /**
     * DOI
     */
    private String doi;
    
    /**
     * 会议/期刊名称
     */
    private String venue;
    
    /**
     * 作者信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        /**
         * 姓名
         */
        private String name;
        
        /**
         * 机构
         */
        private String affiliation;
        
        /**
         * 邮箱
         */
        private String email;
    }
    
    /**
     * 章节信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        /**
         * 章节标题
         */
        private String title;
        
        /**
         * 章节内容
         */
        private String content;
        
        /**
         * 层级 (1=一级标题, 2=二级标题...)
         */
        private Integer level;
        
        /**
         * 在文档中的起始位置
         */
        private Integer startPosition;
        
        /**
         * 在文档中的结束位置
         */
        private Integer endPosition;
    }
}
