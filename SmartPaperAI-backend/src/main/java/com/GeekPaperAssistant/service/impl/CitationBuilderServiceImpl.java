package com.GeekPaperAssistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.GeekPaperAssistant.mapper.DocumentChunkMapper;
import com.GeekPaperAssistant.model.entity.DocumentChunkDO;
import com.GeekPaperAssistant.model.vo.CitationVO;
import com.GeekPaperAssistant.service.CitationBuilderService;
import com.GeekPaperAssistant.utils.NumberConversionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 引用构建服务实现
 *
 * <p>统一处理引用信息的构建逻辑，避免代码重复</p>
 *
 * @author ican
 * @since 2025-10-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitationBuilderServiceImpl implements CitationBuilderService {

    private final DocumentChunkMapper documentChunkMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<CitationVO> buildCitations(List<Document> docs, String query, int maxSnippetLength) {
        if (docs == null || docs.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 提取关键词列表(前端用于高亮)
        List<String> keywords = query == null || query.isBlank()
                ? List.of()
                : Arrays.stream(query.trim().split("\\s+"))
                .filter(kw -> !kw.isBlank())
                .toList();

        return docs.stream().map(d -> {
            // 尝试多种方式获取 documentId
            Long documentId = NumberConversionUtils.toLong(String.valueOf(d.getMetadata().get("documentId")));
            Integer chunkIndex = NumberConversionUtils.toInteger(String.valueOf(d.getMetadata().get("chunkIndex")));
            Double score = d.getScore(); // 使用Document自带的score
            String title = d.getMetadata().getOrDefault("title", "未知文档").toString();
            String text = d.getText();
            if (text != null && text.length() > maxSnippetLength) {
                text = text.substring(0, maxSnippetLength) + "...";
            }

            // 查询 chunkId（可选，不影响主流程）
            Long chunkId = null;
            String position = null;
            String metadataJson = null;

            try {
                if (documentId != null && chunkIndex != null) {
                    DocumentChunkDO chunk = documentChunkMapper.selectOne(
                            new LambdaQueryWrapper<DocumentChunkDO>()
                                    .eq(DocumentChunkDO::getDocumentId, documentId)
                                    .eq(DocumentChunkDO::getChunkIndex, chunkIndex)
                                    .last("limit 1"));
                    if (chunk != null) {
                        chunkId = chunk.getId();
                        position = "第 " + (chunkIndex + 1) + " 段";
                    }
                } else {
                    log.trace("无法查询chunkId: documentId={}, chunkIndex={}", documentId, chunkIndex);
                }

                // 序列化元数据为JSON（可选，用于调试）
                if (!d.getMetadata().isEmpty()) {
                    try {
                        metadataJson = objectMapper.writeValueAsString(d.getMetadata());
                    } catch (Exception ignored) {
                        metadataJson = d.getMetadata().toString();
                    }
                }
            } catch (Exception e) {
                log.trace("查询chunkId失败 docId={}, chunkIndex={}", documentId, chunkIndex, e);
            }

            return CitationVO.builder()
                    .documentId(documentId)
                    .title(title)
                    .chunkId(chunkId)
                    .chunkIndex(chunkIndex)
                    .score(score)
                    .snippet(text)
                    .keywords(keywords)  // 提供给前端用于高亮
                    .position(position)
                    .metadata(metadataJson)
                    .build();
        }).toList();
    }
}
