package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.InnovationClusterVO;
import com.GeekPaperAssistant.model.vo.LiteratureReviewVO;
import com.GeekPaperAssistant.model.vo.PaperComparisonVO;
import com.GeekPaperAssistant.model.vo.PaperMetadataVO;
import com.GeekPaperAssistant.model.vo.PaperSummaryVO;
import com.GeekPaperAssistant.model.vo.RagAnswerVO;

import java.util.List;

/**
 * 论文分析服务接口
 * 提供论文总结、对比、元数据提取、创新点聚合等功能
 * 
 * @author 席崇援
 */
public interface PaperAnalysisService {
    
    /**
     * 论文总结
     * 
     * @param documentId 文档ID
     * @return 论文总结结果（包含引用）
     */
    RagAnswerVO<PaperSummaryVO> summarizePaper(Long documentId);
    
    /**
     * 论文对比
     * 
     * @param documentIds 文档ID列表
     * @param dimensions 对比维度列表（可选）
     * @return 论文对比结果（包含引用）
     */
    RagAnswerVO<PaperComparisonVO> comparePapers(List<Long> documentIds, List<String> dimensions);
    
    /**
     * 提取论文元数据
     * 
     * @param documentId 文档ID
     * @return 论文元数据
     */
    PaperMetadataVO extractPaperMetadata(Long documentId);
    
    /**
     * 清除论文元数据缓存
     * 
     * @param documentId 文档ID
     */
    void clearPaperMetadataCache(Long documentId);
    
    /**
     * 创新点聚合
     * 
     * @param documentIds 文档ID列表
     * @return 创新点聚类结果
     */
    List<InnovationClusterVO> aggregateInnovations(List<Long> documentIds);
    
    /**
     * 生成文献综述
     * 
     * @param documentIds 文档ID列表
     * @return 文献综述结果（包含引用）
     */
    RagAnswerVO<LiteratureReviewVO> generateLiteratureReview(List<Long> documentIds);
}
