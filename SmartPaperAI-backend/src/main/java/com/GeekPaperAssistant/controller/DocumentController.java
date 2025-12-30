package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.GeekPaperAssistant.model.dto.DocumentQueryDTO;
import com.GeekPaperAssistant.model.dto.PaperComparisonDTO;
import com.GeekPaperAssistant.model.vo.*;
import com.GeekPaperAssistant.service.CitationFormatService;
import com.GeekPaperAssistant.service.DocumentService;
import com.GeekPaperAssistant.service.DocumentTaskService;
import com.GeekPaperAssistant.service.PaperAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档管理控制器
 * 
 * @author 席崇援
 * @since 2025-10-06
 */
@Slf4j
@Tag(name = "文档管理", description = "文档上传、解析、检索、论文分析等功能")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SaCheckLogin
public class DocumentController {
    
    private final DocumentService documentService;
    private final DocumentTaskService documentTaskService;
    private final PaperAnalysisService paperAnalysisService;
    private final CitationFormatService citationFormatService;
    
    /**
     * 上传文档（支持单个或批量）
     * 
     * @param files 文件列表（单个或多个）
     * @param type 文档类型 (research_paper, teaching_material, other)
     * @return 上传结果
     */
    @Operation(summary = "上传文档", description = "支持 PDF、Word、Markdown、TXT 格式；支持单个或批量上传")
    @PostMapping("/upload")
    public Object uploadDocument(
            @Parameter(description = "文件（单个或多个）") @RequestParam("file") MultipartFile[] files,
            @Parameter(description = "文档类型") @RequestParam(value = "type", defaultValue = "other") String type) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 单个文件上传
        if (files.length == 1) {
            return documentService.uploadDocument(files[0], type, userId);
        }
        
        // 批量上传
        List<BatchUploadResultVO.FileUploadResult> results = new java.util.ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (MultipartFile file : files) {
            try {
                DocumentUploadVO uploadResult = documentService.uploadDocument(file, type, userId);
                results.add(BatchUploadResultVO.FileUploadResult.builder()
                        .filename(file.getOriginalFilename())
                        .success(true)
                        .documentId(uploadResult.getDocumentId())
                        .taskId(uploadResult.getTaskId())
                        .title(uploadResult.getTitle())
                        .message("上传成功")
                        .taskStatusUrl(uploadResult.getTaskStatusUrl())
                        .build());
                successCount++;
            } catch (Exception e) {
                results.add(BatchUploadResultVO.FileUploadResult.builder()
                        .filename(file.getOriginalFilename())
                        .success(false)
                        .message("上传失败: " + e.getMessage())
                        .build());
                failureCount++;
            }
        }
        
        return BatchUploadResultVO.builder()
                .total(files.length)
                .success(successCount)
                .failure(failureCount)
                .results(results)
                .message(String.format("批量上传完成: 成功 %d 个，失败 %d 个", successCount, failureCount))
                .build();
    }
    
    /**
     * 分页查询用户的文档列表
     * 
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    @Operation(summary = "分页查询文档列表", description = "支持按标题、类型、状态、知识库等条件查询，支持分页和排序")
    @GetMapping("/list")
    public IPage<DocumentVO> getUserDocuments(DocumentQueryDTO queryDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return documentService.pageUserDocuments(userId, queryDTO);
    }
    
    /**
     * 获取文档详情
     * 
     * @param documentId 文档ID
     * @return 文档信息
     */
    @Operation(summary = "获取文档详情", description = "获取指定文档的详细信息（需拥有者权限且未被删除）")
    @GetMapping("/{documentId}")
    public DocumentVO getDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        
        return documentService.getDocument(documentId);
    }
    
    /**
     * 智能搜索文档
     * 
     * <p>采用混合搜索策略，结合语义理解和关键词匹配，自动返回最相关的结果</p>
     * 
     * @param query 搜索关键词或问题
     * @param topK 返回结果数量
     * @return 搜索结果列表（含高亮片段和相关度评分）
     */
    @Operation(summary = "搜索文档", description = "智能搜索：自动结合语义理解和关键词匹配，返回最相关的文档")
    @GetMapping("/search")
    public List<DocumentSearchResultVO> searchDocuments(
            @Parameter(description = "搜索关键词或问题") @RequestParam("query") String query,
            @Parameter(description = "返回结果数量") @RequestParam(value = "topK", defaultValue = "5") int topK) {
        
        return documentService.hybridSearch(query, topK);
    }
    
    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     * @return 操作结果
     */
    @Operation(summary = "删除文档", description = "逻辑删除文档并清除其向量及映射数据")
    @DeleteMapping("/{documentId}")
    public void deleteDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
    }
    
    /**
     * 获取文档任务状态 (DP-01)
     * 
     * @param documentId 文档ID
     * @return 任务列表
     */
    @Operation(summary = "获取文档任务状态", description = "查看文档处理任务的状态和进度（按创建时间倒序）")
    @GetMapping("/{documentId}/tasks")
    public List<com.GeekPaperAssistant.model.vo.DocumentTaskVO> getDocumentTasks(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        return documentTaskService.getDocumentTasks(documentId);
    }

    /**
     * 按任务ID查询任务状态 (DP-01a)
     *
     * 用于前端已拿到 taskId 的场景，直接查询该任务的最新状态
     */
    @Operation(summary = "获取单个任务状态", description = "根据 taskId 查询任务状态与进度")
    @GetMapping("/tasks/{taskId}")
    public DocumentTaskVO getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return documentTaskService.getTaskStatus(taskId);
    }
    
    /**
     * 重试文档处理
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Operation(summary = "重试文档处理", description = "重新处理失败的文档（自动清理旧数据并重新解析、向量化）")
    @PostMapping("/tasks/{taskId}/retry")
    public TaskRetryVO retryTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        boolean success = documentTaskService.retryTask(taskId);
        return TaskRetryVO.builder()
                .message("文档处理已重新开始")
                .taskId(taskId)
                .success(success)
                .build();
    }

    
    /**
     * 获取文档内容
     *
     * @param documentId 文档ID
     * @return 文档内容
     */
    @Operation(summary = "获取文档内容", description = "获取文档解析后的纯文本内容")
    @GetMapping("/{documentId}/content")
    public String getDocumentContent(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {

        return documentService.parseDocument(documentId);
    }

    /**
     * 文档预览
     *
     * @param documentId 文档ID
     * @param download   是否下载
     * @return 文档文件流
     */
    @Operation(summary = "文档预览", description = "获取文档原始文件以支持在线预览或下载")
    @GetMapping("/{documentId}/preview")
    public ResponseEntity<ByteArrayResource> previewDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Parameter(description = "是否下载") @RequestParam(value = "download", defaultValue = "false") boolean download) {

        DocumentFileVO documentFile = documentService.getDocumentFile(documentId);
        String filename = documentFile.getFilename();
        String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        String dispositionType = download ? "attachment" : "inline";
        ByteArrayResource resource = new ByteArrayResource(documentFile.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documentFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + encodedFilename + "\"")
                .contentLength(documentFile.getData().length)
                .cacheControl(CacheControl.noCache().mustRevalidate())
                .body(resource);
    }
    
    // ==================== 论文分析功能 ====================
    
    /**
     * 论文总结 - 结构化输出（带引用）
     */
    @Operation(summary = "论文总结", description = "生成结构化 JSON（背景/方法/结果/创新点/局限），并附引用片段；若字段缺失会填充空字符串或空数组")
    @PostMapping("/{documentId}/summarize")
    public RagAnswerVO<PaperSummaryVO> summarizePaper(
            @Parameter(description = "论文文档ID") @PathVariable Long documentId) {
        return paperAnalysisService.summarizePaper(documentId);
    }
    
    /**
     * 论文对比 - 多文献对比分析 (PA-01)
     */
    @Operation(summary = "论文对比", description = "多篇论文多维度对比（默认维度：方法/数据集/创新点/结论/实验结果），输出结构化矩阵 + 引用片段")
    @PostMapping("/compare")
    public RagAnswerVO<PaperComparisonVO> comparePapers(
            @Validated @RequestBody PaperComparisonDTO request) {
        
        return paperAnalysisService.comparePapers(request.getDocumentIds(), request.getDimensions());
    }
    
    /**
     * 抽取论文元数据 (PA-03)
     */
    @Operation(summary = "提取论文元数据", description = "从论文内容抽取题录信息（title/authors/year/doi/keywords 等），优先使用 GROBID，失败时降级到 LLM 提取")
    @PostMapping("/{documentId}/metadata")
    public PaperMetadataVO extractPaperMetadata(
            @Parameter(description = "论文文档ID") @PathVariable Long documentId) {
        
        return paperAnalysisService.extractPaperMetadata(documentId);
    }
    
    /**
     * 导出引用格式 (PA-04)
     */
    @Operation(summary = "导出引用格式（全部）", description = "一键返回多种引用格式：BibTeX/APA/MLA/GB/T7714；若缺字段使用空串占位")
    @GetMapping("/{documentId}/citation")
    public java.util.Map<String, String> exportAllCitations(
            @Parameter(description = "论文文档ID") @PathVariable Long documentId) {

        PaperMetadataVO metadata = paperAnalysisService.extractPaperMetadata(documentId);

        String apa = citationFormatService.generateAPA(metadata);
        String bibtex = citationFormatService.generateBibTeX(metadata);
        String mla = citationFormatService.generateMLA(metadata);
        String gbt = citationFormatService.generateGBT7714(metadata);

        java.util.Map<String, String> citations = new java.util.LinkedHashMap<>();
        citations.put("apa", apa != null ? apa : "");
        citations.put("bibtex", bibtex != null ? bibtex : "");
        citations.put("mla", mla != null ? mla : "");
        citations.put("gbt7714", gbt != null ? gbt : "");
        return citations;
    }
    
    /**
     * 批量导出引用 (PA-04)
     */
    @Operation(summary = "批量导出引用", description = "批量导出多篇论文引用格式（串行处理确保数据库连接安全），返回包含文档标题和引用的列表")
    @PostMapping("/citations/batch")
    public BatchCitationVO batchExportCitations(
            @Parameter(description = "文档ID列表") @RequestBody List<Long> documentIds,
            @Parameter(description = "引用格式") @RequestParam(defaultValue = "apa") String format) {
        
        // 注意：由于 MyBatis Mapper 不是线程安全的，这里使用串行处理
        // 如果需要并行，需要为每个线程创建独立的 SqlSession
        List<BatchCitationVO.CitationItem> citationList = documentIds.stream()
            .map(docId -> {
                try {
                    PaperMetadataVO metadata = paperAnalysisService.extractPaperMetadata(docId);
                    
                    String citation;
                    switch (format.toLowerCase()) {
                        case "bibtex", "bib" -> citation = citationFormatService.generateBibTeX(metadata);
                        case "mla" -> citation = citationFormatService.generateMLA(metadata);
                        case "gbt7714", "gb" -> citation = citationFormatService.generateGBT7714(metadata);
                        default -> citation = citationFormatService.generateAPA(metadata);
                    }
                    
                    return BatchCitationVO.CitationItem.builder()
                            .title(metadata.getTitle() != null ? metadata.getTitle() : "")
                            .citation(citation)
                            .build();
                } catch (Exception e) {
                    // 记录详细错误日志便于排查
                    log.error("导出引用失败: documentId={}, format={}", docId, format, e);
                    return BatchCitationVO.CitationItem.builder()
                            .title("提取失败")
                            .citation("")
                            .error(e.getMessage() != null ? e.getMessage() : "未知错误")
                            .build();
                }
            })
            .toList();
        
        return BatchCitationVO.builder()
                .format(format)
                .count(citationList.size())
                .citations(citationList)
                .build();
    }
    
    /**
     * 创新点聚合 (PA-02)
     */
    @Operation(summary = "创新点聚合", description = "抽取多篇论文创新点并进行主题聚类，输出创新主题集合")
    @PostMapping("/innovations/aggregate")
    public List<InnovationClusterVO> aggregateInnovations(
            @Parameter(description = "论文文档ID列表") @RequestBody List<Long> documentIds) {
        
        return paperAnalysisService.aggregateInnovations(documentIds);
    }
    
    /**
     * 文献综述生成 (PA-05)
     */
    @Operation(summary = "文献综述生成", description = "综合分析多篇论文，生成结构化综述（研究现状/方法对比/发展趋势/研究空白/未来方向），附引用片段")
    @PostMapping("/literature-review")
    public RagAnswerVO<LiteratureReviewVO> generateLiteratureReview(
            @Parameter(description = "论文文档ID列表（至少2篇）") @RequestBody List<Long> documentIds) {
        
        return paperAnalysisService.generateLiteratureReview(documentIds);
    }
}

