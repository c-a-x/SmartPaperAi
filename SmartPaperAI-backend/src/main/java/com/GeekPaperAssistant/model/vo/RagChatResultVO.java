package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 聊天结果 VO（简化版，用于对话场景）
 *
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG聊天结果")
public class RagChatResultVO {

    @Schema(description = "答案内容")
    private String answer;

    @Schema(description = "引用来源列表")
    private List<CitationVO> citations;
}
