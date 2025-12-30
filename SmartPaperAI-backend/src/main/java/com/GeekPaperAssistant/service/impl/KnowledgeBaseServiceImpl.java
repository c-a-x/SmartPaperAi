package com.GeekPaperAssistant.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.GeekPaperAssistant.mapper.*;
import com.GeekPaperAssistant.model.dto.CreateKBDTO;
import com.GeekPaperAssistant.model.dto.KnowledgeBaseQueryDTO;
import com.GeekPaperAssistant.model.dto.TagQueryDTO;
import com.GeekPaperAssistant.model.entity.*;
import com.GeekPaperAssistant.model.vo.TagVO;
import com.GeekPaperAssistant.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {
    
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TagMapper tagMapper;
    private final DocumentTagMapper documentTagMapper;
    private final DocumentMapper documentMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(CreateKBDTO createKBDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        KnowledgeBaseDO kb = KnowledgeBaseDO.builder()
            .name(createKBDTO.getName())
            .description(createKBDTO.getDescription())
            .userId(userId)
            .documentCount(0)
            .isDeleted(0)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();
        
        knowledgeBaseMapper.insert(kb);
        log.info("创建知识库成功: id={}, name={}, userId={}", kb.getId(), kb.getName(), userId);
        
        return kb.getId();
    }
    
    @Override
    public IPage<KnowledgeBaseDO> pageUserKnowledgeBases(Long userId, KnowledgeBaseQueryDTO queryDTO) {
        // 创建分页对象
        Page<KnowledgeBaseDO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBaseDO::getUserId, userId)
               .eq(KnowledgeBaseDO::getIsDeleted, 0);
        
        // 名称模糊查询
        if (StrUtil.isNotBlank(queryDTO.getName())) {
            wrapper.like(KnowledgeBaseDO::getName, queryDTO.getName());
        }
        
        // 描述模糊查询
        if (StrUtil.isNotBlank(queryDTO.getDescription())) {
            wrapper.like(KnowledgeBaseDO::getDescription, queryDTO.getDescription());
        }
        
        // 排序
        String sortField = queryDTO.getSortField();
        String sortOrder = queryDTO.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByDesc(getKBColumnByField(sortField));
            } else {
                wrapper.orderByAsc(getKBColumnByField(sortField));
            }
        } else {
            // 默认按更新时间降序
            wrapper.orderByDesc(KnowledgeBaseDO::getUpdateTime);
        }
        
        // 执行分页查询
        return knowledgeBaseMapper.selectPage(page, wrapper);
    }
    
    /**
     * 根据字段名获取知识库列函数（用于动态排序）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<KnowledgeBaseDO, ?> getKBColumnByField(String field) {
        return switch (field) {
            case "name" -> KnowledgeBaseDO::getName;
            case "documentCount" -> KnowledgeBaseDO::getDocumentCount;
            case "createTime" -> KnowledgeBaseDO::getCreateTime;
            default -> KnowledgeBaseDO::getUpdateTime;
        };
    }
    
    @Override
    public KnowledgeBaseDO getKnowledgeBase(Long kbId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }
        
        // 检查所有权
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该知识库");
        }
        
        return kb;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long kbId, CreateKBDTO createKBDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        
        // 检查所有权
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该知识库");
        }
        
        kb.setName(createKBDTO.getName());
        kb.setDescription(createKBDTO.getDescription());
        kb.setUpdateTime(LocalDateTime.now());
        
        knowledgeBaseMapper.updateById(kb);
        log.info("更新知识库成功: id={}", kbId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long kbId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }
        
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException("只有所有者可以删除知识库");
        }
        
        kb.setIsDeleted(1);
        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(kb);
        
        log.info("删除知识库成功: id={}", kbId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchArchiveDocuments(List<Long> documentIds, Long kbId) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BusinessException("文档ID列表不能为空");
        }
        
        // 验证知识库所有权
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException("无权向该知识库添加文档");
        }
        
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        // 批量处理文档
        for (Long documentId : documentIds) {
            try {
                // 验证文档所有权
                DocumentDO document = documentMapper.selectById(documentId);
                if (document == null) {
                    errors.add("文档 " + documentId + " 不存在");
                    continue;
                }
                if (document.getIsDeleted() != null && document.getIsDeleted() == 1) {
                    errors.add("文档 " + documentId + " 已被删除");
                    continue;
                }
                if (!document.getUserId().equals(userId)) {
                    errors.add("文档 " + documentId + " 无权归档");
                    continue;
                }
                
                // 关联文档到知识库
                document.setKbId(kbId);
                document.setUpdateTime(LocalDateTime.now());
                documentMapper.updateById(document);
                
                successCount++;
                log.info("文档归档成功: documentId={}, kbId={}", documentId, kbId);
                
            } catch (Exception e) {
                log.error("归档文档失败: documentId={}, kbId={}", documentId, kbId, e);
                errors.add("文档 " + documentId + ": " + e.getMessage());
            }
        }
        
        // 更新知识库文档数量
        if (successCount > 0) {
            kb.setDocumentCount(kb.getDocumentCount() + successCount);
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseMapper.updateById(kb);
        }
        
        log.info("批量归档完成: total={}, success={}, failed={}", 
            documentIds.size(), successCount, documentIds.size() - successCount);
        
        if (!errors.isEmpty() && successCount == 0) {
            throw new BusinessException("所有文档归档失败: " + String.join("; ", errors));
        } else if (!errors.isEmpty()) {
            log.warn("部分文档归档失败: {}", String.join("; ", errors));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDocumentsFromKnowledgeBase(List<Long> documentIds, Long kbId) {
        // Implementation similar to batchArchiveDocuments but removes the kbId association
        Long userId = StpUtil.getLoginIdAsLong();
        
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BusinessException("文档ID列表不能为空");
        }

        // 验证知识库所有权
        KnowledgeBaseDO kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || kb.getIsDeleted() == 1) {
            throw new BusinessException("知识库不存在");
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该知识库");
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // 批量处理文档
        for (Long documentId : documentIds) {
            try {
                // 验证文档所有权
                DocumentDO document = documentMapper.selectById(documentId);
                if (document == null) {
                    errors.add("文档 " + documentId + " 不存在");
                    continue;
                }
                if (document.getIsDeleted() != null && document.getIsDeleted() == 1) {
                    errors.add("文档 " + documentId + " 已被删除");
                    continue;
                }
                if (!document.getUserId().equals(userId)) {
                    errors.add("文档 " + documentId + " 无权操作");
                    continue;
                }

                // 移除文档与知识库的关联
                document.setKbId(-1L); // 设为不属于任何知识库
                document.setUpdateTime(LocalDateTime.now());
                documentMapper.updateById(document);
                knowledgeBaseMapper.updateById(kb);

                successCount++;
                log.info("文档移除成功: documentId={}, kbId={}", documentId, kbId);

            } catch (Exception e) {
                log.error("移除文档失败: documentId={}, kbId={}", documentId, kbId, e);
                errors.add("文档 " + documentId + ": " + e.getMessage());
            }
        }

        // 更新知识库文档数量
        if (successCount > 0) {
            kb.setDocumentCount(kb.getDocumentCount() - successCount);
            kb.setUpdateTime(LocalDateTime.now());
            knowledgeBaseMapper.updateById(kb);
        }

        log.info("批量移除完成: total={}, success={}, failed={}",
            documentIds.size(), successCount, documentIds.size() - successCount);

        if (!errors.isEmpty() && successCount == 0) {
            throw new BusinessException("所有文档移除失败: " + String.join("; ", errors));
        } else if (!errors.isEmpty()) {
            log.warn("部分文档移除失败: {}", String.join("; ", errors));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(String name, String color) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 检查标签是否已存在
        TagDO existingTag = tagMapper.selectOne(
            new LambdaQueryWrapper<TagDO>()
                .eq(TagDO::getName, name)
                .eq(TagDO::getUserId, userId)
        );
        
        if (existingTag != null) {
            return existingTag.getId();
        }
        
        TagDO tag = TagDO.builder()
            .name(name)
            .color(color)
            .userId(userId)
            .createTime(LocalDateTime.now())
            .build();
        
        tagMapper.insert(tag);
        log.info("创建标签成功: id={}, name={}", tag.getId(), name);
        
        return tag.getId();
    }
    
    @Override
    public IPage<TagVO> pageUserTags(Long userId, TagQueryDTO queryDTO) {
        // 创建分页对象
        Page<TagDO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        
        // 构建查询条件
        LambdaQueryWrapper<TagDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagDO::getUserId, userId);
        
        // 名称模糊查询
        if (StrUtil.isNotBlank(queryDTO.getName())) {
            wrapper.like(TagDO::getName, queryDTO.getName());
        }
        
        // 颜色过滤
        if (StrUtil.isNotBlank(queryDTO.getColor())) {
            wrapper.eq(TagDO::getColor, queryDTO.getColor());
        }
        
        // 排序
        String sortField = queryDTO.getSortField();
        String sortOrder = queryDTO.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            if ("desc".equalsIgnoreCase(sortOrder)) {
                wrapper.orderByDesc(getTagColumnByField(sortField));
            } else {
                wrapper.orderByAsc(getTagColumnByField(sortField));
            }
        } else {
            // 默认按创建时间降序
            wrapper.orderByDesc(TagDO::getCreateTime);
        }
        
        // 执行分页查询
        IPage<TagDO> tagPage = tagMapper.selectPage(page, wrapper);
        
        // 转换为 VO（包含使用次数统计）
        IPage<TagVO> voPage = new Page<>(tagPage.getCurrent(), tagPage.getSize(), tagPage.getTotal());
        voPage.setRecords(tagPage.getRecords().stream()
                .map(tag -> {
                    // 统计使用次数
                    int usageCount = documentTagMapper.selectCount(
                        new LambdaQueryWrapper<DocumentTagDO>()
                            .eq(DocumentTagDO::getTagId, tag.getId())
                    ).intValue();
                    
                    return TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .usageCount(usageCount)
                        .createTime(tag.getCreateTime())
                        .build();
                })
                .collect(Collectors.toList()));
        
        return voPage;
    }
    
    /**
     * 根据字段名获取标签列函数（用于动态排序）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<TagDO, ?> getTagColumnByField(String field) {
        return switch (field) {
            case "name" -> TagDO::getName;
            case "color" -> TagDO::getColor;
            default -> TagDO::getCreateTime;
        };
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tagDocument(Long documentId, List<Long> tagIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 验证文档权限
        DocumentDO document = documentMapper.selectById(documentId);
        if (document == null || !document.getUserId().equals(userId)) {
            throw new BusinessException("无权标记该文档");
        }
        
        // 删除旧标签关联
        documentTagMapper.delete(
            new LambdaQueryWrapper<DocumentTagDO>()
                .eq(DocumentTagDO::getDocumentId, documentId)
        );
        
        // 添加新标签关联
        for (Long tagId : tagIds) {
            DocumentTagDO documentTag = DocumentTagDO.builder()
                .documentId(documentId)
                .tagId(tagId)
                .createTime(LocalDateTime.now())
                .build();
            documentTagMapper.insert(documentTag);
        }
        
        log.info("文档标签更新成功: documentId={}, tagCount={}", documentId, tagIds.size());
    }
    
    @Override
    public List<Long> getDocumentsByTag(Long tagId, Long userId) {
        List<DocumentTagDO> documentTags = documentTagMapper.selectList(
            new LambdaQueryWrapper<DocumentTagDO>()
                .eq(DocumentTagDO::getTagId, tagId)
        );
        
        List<Long> documentIds = documentTags.stream()
            .map(DocumentTagDO::getDocumentId)
            .toList();
        
        if (documentIds.isEmpty()) {
            return List.of();
        }
        
        // 只返回用户有权访问的文档
        List<DocumentDO> documents = documentMapper.selectList(
            new LambdaQueryWrapper<DocumentDO>()
                .in(DocumentDO::getId, documentIds)
                .eq(DocumentDO::getUserId, userId)
        );
        
        return documents.stream()
            .map(DocumentDO::getId)
            .collect(Collectors.toList());
    }
}
