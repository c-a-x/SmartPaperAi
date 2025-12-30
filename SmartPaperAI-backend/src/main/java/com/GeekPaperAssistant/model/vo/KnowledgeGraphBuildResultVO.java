package com.GeekPaperAssistant.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识图谱构建结果 VO
 *
 * @author 席崇援
 * @since 2025-10-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphBuildResultVO {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 提取的概念数量
     */
    private Integer conceptCount;

    /**
     * 新增的概念数量
     */
    private Integer newConceptCount;

    /**
     * 创建的概念关系数量
     */
    private Integer relationshipCount;

    /**
     * 是否构建成功
     */
    private Boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
}
