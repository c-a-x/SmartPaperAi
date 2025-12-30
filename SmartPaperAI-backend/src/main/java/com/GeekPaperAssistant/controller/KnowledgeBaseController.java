package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.GeekPaperAssistant.model.dto.CreateKBDTO;
import com.GeekPaperAssistant.model.dto.KnowledgeBaseQueryDTO;
import com.GeekPaperAssistant.model.dto.TagQueryDTO;
import com.GeekPaperAssistant.model.entity.KnowledgeBaseDO;
import com.GeekPaperAssistant.model.vo.TagVO;
import com.GeekPaperAssistant.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 * 
 * @author ican
 */
@Tag(name = "知识库管理")
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    private final KnowledgeBaseService knowledgeBaseService;
    
    @Operation(summary = "创建知识库")
    @PostMapping
    public Long createKnowledgeBase(@RequestBody CreateKBDTO createKBDTO) {
        return knowledgeBaseService.createKnowledgeBase(createKBDTO);
    }
    
    @Operation(summary = "分页查询我的知识库列表", description = "支持按名称、描述搜索，支持分页和排序")
    @GetMapping("/list")
    public IPage<KnowledgeBaseDO> getMyKnowledgeBases(KnowledgeBaseQueryDTO queryDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return knowledgeBaseService.pageUserKnowledgeBases(userId, queryDTO);
    }
    
    @Operation(summary = "获取知识库详情")
    @GetMapping("/{kbId}")
    public KnowledgeBaseDO getKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId) {
        return knowledgeBaseService.getKnowledgeBase(kbId);
    }
    
    @Operation(summary = "更新知识库")
    @PutMapping("/{kbId}")
    public void updateKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @RequestBody CreateKBDTO createKBDTO) {
        knowledgeBaseService.updateKnowledgeBase(kbId, createKBDTO);
    }
    
    @Operation(summary = "删除知识库")
    @DeleteMapping("/{kbId}")
    public void deleteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId) {
        knowledgeBaseService.deleteKnowledgeBase(kbId);
    }
    
    @Operation(summary = "批量将文档归档到知识库")
    @PostMapping("/{kbId}/documents/batch")
    public void batchArchiveDocuments(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @Parameter(description = "文档ID列表") @RequestBody List<Long> documentIds) {
        knowledgeBaseService.batchArchiveDocuments(documentIds, kbId);
    }

    @Operation(summary = "移除知识库中的文档")
    @PostMapping("/{kbId}/documents/remove")
    public void removeDocumentsFromKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @Parameter(description = "文档ID列表") @RequestBody List<Long> documentIds) {
        knowledgeBaseService.removeDocumentsFromKnowledgeBase(documentIds, kbId);
    }
    
    @Operation(summary = "创建标签")
    @PostMapping("/tags")
    public Long createTag(
            @Parameter(description = "标签名称") @RequestParam String name,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color) {
        return knowledgeBaseService.createTag(name, color);
    }
    
    @Operation(summary = "分页查询我的标签列表", description = "支持按名称、颜色搜索，支持分页和排序")
    @GetMapping("/tags/my")
    public IPage<TagVO> getMyTags(TagQueryDTO queryDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return knowledgeBaseService.pageUserTags(userId, queryDTO);
    }
    
    @Operation(summary = "为文档添加标签")
    @PostMapping("/documents/{documentId}/tags")
    public void tagDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Parameter(description = "标签ID列表") @RequestBody List<Long> tagIds) {
        knowledgeBaseService.tagDocument(documentId, tagIds);
    }
    
    @Operation(summary = "根据标签获取文档列表")
    @GetMapping("/tags/{tagId}/documents")
    public List<Long> getDocumentsByTag(
            @Parameter(description = "标签ID") @PathVariable Long tagId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return knowledgeBaseService.getDocumentsByTag(tagId, userId);
    }
}
