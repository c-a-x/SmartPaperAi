package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.DocumentMetadataVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * GROBID 元数据解析服务接口
 * 
 * <p>GROBID (GeneRation Of BIbliographic Data) 是一个开源的机器学习库,
 * 专门用于从学术论文PDF中提取高精度的结构化元数据。</p>
 * 
 * <p>支持提取的元数据:</p>
 * <ul>
 *   <li>标题、作者、摘要</li>
 *   <li>关键词、机构</li>
 *   <li>章节结构</li>
 *   <li>引用文献</li>
 *   <li>表格、图表</li>
 * </ul>
 * 
 * @author 席崇援
 */
public interface GrobidMetadataService {
    
    /**
     * 从PDF文档中提取结构化元数据
     * 
     * @param pdfFile PDF文件
     * @return 文档元数据
     */
    DocumentMetadataVO extractMetadata(MultipartFile pdfFile);
    
    /**
     * 从PDF字节数组中提取结构化元数据
     * 
     * @param pdfData PDF文件字节数组
     * @param filename 文件名(用于日志)
     * @return 文档元数据
     */
    DocumentMetadataVO extractMetadata(byte[] pdfData, String filename);
    
    /**
     * 从PDF文档中提取章节结构
     * 
     * @param pdfFile PDF文件
     * @return TEI XML格式的章节结构
     */
    String extractStructure(MultipartFile pdfFile);
    
    /**
     * 检查GROBID服务是否可用
     * 
     * @return true=可用, false=不可用
     */
    boolean isAvailable();
}
