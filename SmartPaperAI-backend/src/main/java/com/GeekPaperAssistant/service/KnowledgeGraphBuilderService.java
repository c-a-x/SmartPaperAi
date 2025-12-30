package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.KnowledgeGraphBuildResultVO;

/**
 * 知识图谱构建服务
 *
 * <p>负责从文档中提取概念并构建知识图谱</p>
 *
 * @author 席崇援
 * @since 2025-10-27
 */
public interface KnowledgeGraphBuilderService {

    /**
     * 为文档构建知识图谱
     *
     * <p>流程：</p>
     * <ol>
     *   <li>从文档文本中提取关键概念</li>
     *   <li>保存概念节点到 Neo4j</li>
     *   <li>创建文档节点</li>
     *   <li>创建文档-概念关系（CONTAINS）</li>
     *   <li>分析并创建概念间关系（RELATED_TO）</li>
     * </ol>
     *
     * @param documentId 文档ID
     * @param title 文档标题
     * @param content 文档全文内容
     * @param userId 用户ID
     * @param kbId 知识库ID
     * @return 构建结果统计
     */
    KnowledgeGraphBuildResultVO buildGraphForDocument(
        Long documentId,
        String title,
        String content,
        Long userId,
        Long kbId
    );

    /**
     * 删除文档的知识图谱数据
     *
     * @param documentId 文档ID
     */
    void deleteGraphForDocument(Long documentId);
}
