package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.model.vo.DocumentMetadataVO;
import com.GeekPaperAssistant.service.SmartChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能文档分块服务实现
 *
 * <p>重构说明：</p>
 * <ul>
 *   <li>保留章节分块（利用GROBID提取的PDF结构）</li>
 *   <li>使用Spring AI TokenTextSplitter替代自定义语义分块（简化代码、提升性能）</li>
 *   <li>从353行减少到143行，减少60%代码</li>
 * </ul>
 *
 * @author 席崇援
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html#_textsplitter">Spring AI TokenTextSplitter</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartChunkingServiceImpl implements SmartChunkingService {

    @Override
    public List<ChunkResult> smartChunk(String content, DocumentMetadataVO metadata, int chunkSize, int overlapSize) {
        // 决策: 有章节信息 → 章节分块, 否则 → TokenTextSplitter
        if (metadata != null && metadata.getSections() != null && !metadata.getSections().isEmpty()) {
            log.info("检测到章节结构,使用章节分块策略: sections={}", metadata.getSections().size());
            return chunkBySections(metadata, overlapSize);
        } else {
            log.info("未检测到章节结构,使用TokenTextSplitter分块: chunkSize={}, overlap={}", chunkSize, overlapSize);
            return tokenBasedChunk(content, chunkSize, overlapSize);
        }
    }

    @Override
    public List<ChunkResult> chunkBySections(DocumentMetadataVO metadata, int overlapSize) {
        if (metadata == null || metadata.getSections() == null || metadata.getSections().isEmpty()) {
            log.warn("章节信息为空,无法进行章节分块");
            return new ArrayList<>();
        }

        List<ChunkResult> results = new ArrayList<>();
        List<DocumentMetadataVO.Section> sections = metadata.getSections();

        for (int i = 0; i < sections.size(); i++) {
            DocumentMetadataVO.Section section = sections.get(i);

            // 构建分块内容: 标题 + 内容
            StringBuilder chunkContent = new StringBuilder();

            // 添加标题(带层级标记)
            String titlePrefix = "#".repeat(section.getLevel()) + " ";
            chunkContent.append(titlePrefix).append(section.getTitle()).append("\n\n");
            chunkContent.append(section.getContent());

            // 添加 overlap: 包含下一章节的开头部分
            if (i < sections.size() - 1 && overlapSize > 0) {
                DocumentMetadataVO.Section nextSection = sections.get(i + 1);
                String nextContent = nextSection.getContent();

                // 计算 overlap 字符数(简化: overlapSize * 4)
                int overlapChars = overlapSize * 4;
                if (nextContent.length() > overlapChars) {
                    chunkContent.append("\n\n--- 下一章节预览 ---\n");
                    chunkContent.append(nextContent, 0, overlapChars);
                    chunkContent.append("...");
                } else {
                    chunkContent.append("\n\n--- 下一章节 ---\n");
                    chunkContent.append(nextContent);
                }
            }

            ChunkResult chunk = new ChunkResult(
                    chunkContent.toString(),
                    "section",
                    0, // startPosition不再使用
                    0  // endPosition不再使用
            );
            chunk.setSectionTitle(section.getTitle());
            chunk.setSectionLevel(section.getLevel());

            results.add(chunk);
        }

        log.info("章节分块完成: sections={}, chunks={}", sections.size(), results.size());
        return results;
    }

    /**
     * 基于TokenTextSplitter的固定大小分块
     * 替代之前复杂的语义分块（使用Spring AI官方实现）
     *
     * @param content 文档内容
     * @param chunkSize 块大小(token)
     * @param overlapSize 重叠大小(token)
     * @return 分块结果
     */
    private List<ChunkResult> tokenBasedChunk(String content, int chunkSize, int overlapSize) {
        // ✅ 使用Spring AI的TokenTextSplitter（官方推荐）
        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,           // defaultChunkSize
                overlapSize,         // minChunkSizeChars
                5,                   // minChunkLengthToEmbed
                10000,               // maxNumChunks
                true                 // keepSeparator
        );

        // 将文本包装为Document
        Document document = new Document(content);

        // 执行分块
        List<Document> chunks = splitter.split(document);

        // 转换为ChunkResult
        List<ChunkResult> results = new ArrayList<>();
        for (Document chunk : chunks) {
            ChunkResult result = new ChunkResult(
                    chunk.getText(),
                    "token",
                    0,
                    0
            );
            results.add(result);
        }

        log.info("TokenTextSplitter分块完成: chunks={}, avgLength={}",
                results.size(),
                results.isEmpty() ? 0 : results.stream().mapToInt(c -> c.getContent().length()).average().orElse(0));

        return results;
    }

    @Override
    public List<ChunkResult> semanticChunk(String content, int chunkSize, int overlapSize) {
        // ⚠️ 已废弃：使用tokenBasedChunk替代
        // 旧的语义分块使用embedding计算相似度，成本高且效果提升有限
        log.warn("semanticChunk已废弃，使用tokenBasedChunk替代");
        return tokenBasedChunk(content, chunkSize, overlapSize);
    }
}
