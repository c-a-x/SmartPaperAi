package com.GeekPaperAssistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.GeekPaperAssistant.model.dto.CreateKBDTO;
import com.GeekPaperAssistant.model.dto.KnowledgeBaseQueryDTO;
import com.GeekPaperAssistant.model.dto.TagQueryDTO;
import com.GeekPaperAssistant.model.entity.KnowledgeBaseDO;
import com.GeekPaperAssistant.model.vo.TagVO;

import java.util.List;

/**
 * 知识库服务接口
 * 
 * @author 席崇援
 */
public interface KnowledgeBaseService {
    
    /**
     * 创建知识库
     * 
     * @param createKBDTO 创建知识库DTO
     * @return 知识库ID
     */
    Long createKnowledgeBase(CreateKBDTO createKBDTO);
    
    /**
     * 分页查询用户的知识库列表
     * 
     * @param userId 用户ID
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<KnowledgeBaseDO> pageUserKnowledgeBases(Long userId, KnowledgeBaseQueryDTO queryDTO);
    
    /**
     * 获取知识库详情
     * 
     * @param kbId 知识库ID
     * @return 知识库详情
     */
    KnowledgeBaseDO getKnowledgeBase(Long kbId);
    
    /**
     * 更新知识库
     * 
     * @param kbId 知识库ID
     * @param createKBDTO 更新内容
     */
    void updateKnowledgeBase(Long kbId, CreateKBDTO createKBDTO);
    
    /**
     * 删除知识库
     * 
     * @param kbId 知识库ID
     */
    void deleteKnowledgeBase(Long kbId);
    
    /**
     * 批量将文档归档到知识库
     * 
     * @param documentIds 文档ID列表
     * @param kbId 知识库ID
     */
    void batchArchiveDocuments(List<Long> documentIds, Long kbId);

    /**
     * 移除知识库中的文档
     * 
     * @param documentIds 文档ID列表
     * @param kbId 知识库ID
     */
    void removeDocumentsFromKnowledgeBase(List<Long> documentIds, Long kbId);
    
    /**
     * 创建标签
     * 
     * @param name 标签名称
     * @param color 标签颜色
     * @return 标签ID
     */
    Long createTag(String name, String color);
    
    /**
     * 分页查询用户的标签列表
     * 
     * @param userId 用户ID
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<TagVO> pageUserTags(Long userId, TagQueryDTO queryDTO);
    
    /**
     * 为文档添加标签
     * 
     * @param documentId 文档ID
     * @param tagIds 标签ID列表
     */
    void tagDocument(Long documentId, List<Long> tagIds);
    
    /**
     * 根据标签获取文档列表
     * 
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 文档ID列表
     */
    List<Long> getDocumentsByTag(Long tagId, Long userId);
}
