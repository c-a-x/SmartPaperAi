package com.GeekPaperAssistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.GeekPaperAssistant.model.dto.DocumentQueryDTO;
import com.GeekPaperAssistant.model.vo.DocumentFileVO;
import com.GeekPaperAssistant.model.vo.DocumentSearchResultVO;
import com.GeekPaperAssistant.model.vo.DocumentUploadVO;
import com.GeekPaperAssistant.model.vo.DocumentVO;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 * 
 * @author 席崇援
 */
public interface DocumentService {
    
    /**
     * 上传并处理文档
     * 
     * @param file 文件
     * @param type 文档类型
     * @param userId 用户ID
     * @return 文档上传响应（包含 documentId 和 taskId）
     */
    DocumentUploadVO uploadDocument(MultipartFile file, String type, Long userId);
    
    /**
     * 分页查询用户的文档列表
     * 
     * @param userId 用户ID
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<DocumentVO> pageUserDocuments(Long userId, DocumentQueryDTO queryDTO);
    
    /**
     * 获取文档详情
     * 
     * @param documentId 文档ID
     * @return 文档信息
     */
    DocumentVO getDocument(Long documentId);
    
    /**
     * 解析文档内容
     * 
     * @param documentId 文档ID
     * @return 文档内容
     */
    String parseDocument(Long documentId);
    
    /**
     * 将文档向量化并存储
     * 
     * @param documentId 文档ID
     * @param content 文档内容
     * @param userId 用户ID
     */
    void vectorizeAndStore(Long documentId, String content, Long userId);
    
    /**
     * 检索相关文档 (向量检索)
     * 
     * @param query 查询文本
     * @param topK 返回数量
     * @return 相关文档列表
     */
    List<Document> searchSimilarDocuments(String query, int topK);
    
    /**
     * 🆕 混合搜索 - 结合向量检索和全文检索
     * 
     * 使用 RRF (Reciprocal Rank Fusion) 算法融合:
     * - 向量检索结果(语义理解)
     * - ES 全文检索结果(关键词匹配)
     * 
     * @param query 搜索查询
     * @param topK 最终返回数量
     * @return 混合搜索结果(含高亮信息)
     */
    List<DocumentSearchResultVO> hybridSearch(String query, int topK);
    
    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     */
    void deleteDocument(Long documentId);
    
    /**
     * 重建文档向量索引 (RAG-05)
     * 
     * @param documentId 文档ID
     * @return 重建的向量数量
     */
    int reindexDocument(Long documentId);
    
    /**
     * 清除文档的所有向量 (RAG-05)
     * 
     * @param documentId 文档ID
     * @return 删除的向量数量
     */
    int purgeDocumentVectors(Long documentId);
    
    /**
     * 批量重建向量索引
     * 
     * @param documentIds 文档ID列表
     * @return 成功重建的文档数量
     */
    int batchReindexDocuments(List<Long> documentIds);
    
    /**
     * 获取文档文件用于预览或下载
     * 
     * @param documentId 文档ID
     * @return 文档文件VO
     */
    DocumentFileVO getDocumentFile(Long documentId);
}

