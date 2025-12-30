package com.GeekPaperAssistant.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 答案统一返回 VO（泛型封装）
 * 
 * @author ican
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG答案统一返回")
public class RagAnswerVO<T> {

    @Schema(description = "答案内容")
    private T answer;

    @Schema(description = "引用来源列表")
    private List<CitationVO> citations;

    @Schema(description = "扩展元数据")
    private Object metadata;
}
