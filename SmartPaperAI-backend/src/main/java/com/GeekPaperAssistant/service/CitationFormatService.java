package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.PaperMetadataVO;

/**
 * 引用格式服务
 * 
 * @author ican
 */
public interface CitationFormatService {
    
    /**
     * 生成 BibTeX 格式引用
     * 
     * @param metadata 论文元数据
     * @return BibTeX 格式字符串
     */
    String generateBibTeX(PaperMetadataVO metadata);
    
    /**
     * 生成 APA 格式引用
     * 
     * @param metadata 论文元数据
     * @return APA 格式字符串
     */
    String generateAPA(PaperMetadataVO metadata);
    
    /**
     * 生成 MLA 格式引用
     * 
     * @param metadata 论文元数据
     * @return MLA 格式字符串
     */
    String generateMLA(PaperMetadataVO metadata);
    
    /**
     * 生成 GB/T 7714 格式引用（中国国家标准）
     * 
     * @param metadata 论文元数据
     * @return GB/T 7714 格式字符串
     */
    String generateGBT7714(PaperMetadataVO metadata);
    
    /**
     * 批量导出引用
     * 
     * @param documentIds 文档ID列表
     * @param format 格式（bibtex, apa, mla, gbt7714）
     * @return 引用字符串列表
     */
    java.util.List<String> batchExportCitations(java.util.List<Long> documentIds, String format);
}
