package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文献综述 VO
 * 
 * @author 席崇援
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiteratureReviewVO {
    
    /**
     * 综述主题
     */
    private String topic;
    
    /**
     * 分析的文献数量
     */
    private Integer paperCount;
    
    /**
     * 文献列表（标题+年份）
     */
    private List<PaperInfo> papers;
    
    /**
     * 研究现状综述
     */
    private ResearchStatus researchStatus;
    
    /**
     * 研究方法对比
     */
    private MethodologyComparison methodologyComparison;
    
    /**
     * 发展趋势分析
     */
    private TrendAnalysis trendAnalysis;
    
    /**
     * 研究空白与挑战
     */
    private ResearchGaps researchGaps;
    
    /**
     * 未来研究方向
     */
    private FutureDirections futureDirections;
    
    /**
     * 关键词云（按频率排序）
     */
    private List<KeywordFrequency> keywordCloud;
    
    /**
     * 验证和填充默认值
     */
    public void validate() {
        if (papers == null) {
            papers = new ArrayList<>();
        }
        if (keywordCloud == null) {
            keywordCloud = new ArrayList<>();
        }
        if (researchStatus == null) {
            researchStatus = new ResearchStatus();
        }
        researchStatus.validate();
        
        if (methodologyComparison == null) {
            methodologyComparison = new MethodologyComparison();
        }
        methodologyComparison.validate();
        
        if (trendAnalysis == null) {
            trendAnalysis = new TrendAnalysis();
        }
        trendAnalysis.validate();
        
        if (researchGaps == null) {
            researchGaps = new ResearchGaps();
        }
        researchGaps.validate();
        
        if (futureDirections == null) {
            futureDirections = new FutureDirections();
        }
        futureDirections.validate();
    }
    
    /**
     * 文献信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaperInfo {
        /**
         * 文档ID
         */
        private Long documentId;
        
        /**
         * 论文标题
         */
        private String title;
        
        /**
         * 作者列表
         */
        private List<String> authors;
        
        /**
         * 发表年份
         */
        private Integer year;
    }
    
    /**
     * 研究现状
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResearchStatus {
        /**
         * 研究领域概述
         */
        private String overview;
        
        /**
         * 主要研究主题
         */
        private List<String> mainThemes;
        
        /**
         * 代表性工作（文献ID + 贡献描述）
         */
        private List<RepresentativeWork> representativeWorks;
        
        public void validate() {
            if (mainThemes == null) {
                mainThemes = new ArrayList<>();
            }
            if (representativeWorks == null) {
                representativeWorks = new ArrayList<>();
            }
            if (overview == null) {
                overview = "";
            }
        }
    }
    
    /**
     * 代表性工作
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepresentativeWork {
        /**
         * 文档ID
         */
        private Long documentId;
        
        /**
         * 论文标题
         */
        private String title;
        
        /**
         * 核心贡献
         */
        private String contribution;
        
        /**
         * 影响力评分（1-5）
         */
        private Double impactScore;
    }
    
    /**
     * 研究方法对比
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodologyComparison {
        /**
         * 方法分类总结
         */
        private String summary;
        
        /**
         * 各类方法详情
         */
        private List<MethodCategory> categories;
        
        /**
         * 方法演进趋势
         */
        private String evolution;
        
        public void validate() {
            if (categories == null) {
                categories = new ArrayList<>();
            }
            if (summary == null) {
                summary = "";
            }
            if (evolution == null) {
                evolution = "";
            }
        }
    }
    
    /**
     * 方法类别
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodCategory {
        /**
         * 方法类别名称
         */
        private String categoryName;
        
        /**
         * 使用该方法的论文
         */
        private List<Long> documentIds;
        
        /**
         * 方法优缺点
         */
        private String prosAndCons;
    }
    
    /**
     * 发展趋势分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendAnalysis {
        /**
         * 时间轴趋势描述
         */
        private String timeline;
        
        /**
         * 热点主题
         */
        private List<String> hotTopics;
        
        /**
         * 新兴技术
         */
        private List<String> emergingTechnologies;
        
        /**
         * 研究重心转移
         */
        private String focusShift;
        
        public void validate() {
            if (hotTopics == null) {
                hotTopics = new ArrayList<>();
            }
            if (emergingTechnologies == null) {
                emergingTechnologies = new ArrayList<>();
            }
            if (timeline == null) {
                timeline = "";
            }
            if (focusShift == null) {
                focusShift = "";
            }
        }
    }
    
    /**
     * 研究空白与挑战
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResearchGaps {
        /**
         * 尚未解决的问题
         */
        private List<String> unsolvedProblems;
        
        /**
         * 方法论上的局限
         */
        private List<String> methodologicalLimitations;
        
        /**
         * 数据/资源缺口
         */
        private List<String> dataGaps;
        
        public void validate() {
            if (unsolvedProblems == null) {
                unsolvedProblems = new ArrayList<>();
            }
            if (methodologicalLimitations == null) {
                methodologicalLimitations = new ArrayList<>();
            }
            if (dataGaps == null) {
                dataGaps = new ArrayList<>();
            }
        }
    }
    
    /**
     * 未来研究方向
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FutureDirections {
        /**
         * 潜在研究方向
         */
        private List<String> directions;
        
        /**
         * 跨学科融合机会
         */
        private List<String> interdisciplinaryOpportunities;
        
        /**
         * 实际应用前景
         */
        private String applicationProspects;
        
        public void validate() {
            if (directions == null) {
                directions = new ArrayList<>();
            }
            if (interdisciplinaryOpportunities == null) {
                interdisciplinaryOpportunities = new ArrayList<>();
            }
            if (applicationProspects == null) {
                applicationProspects = "";
            }
        }
    }
    
    /**
     * 关键词频率
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordFrequency {
        /**
         * 关键词
         */
        private String keyword;
        
        /**
         * 出现频率
         */
        private Integer frequency;
    }
}
