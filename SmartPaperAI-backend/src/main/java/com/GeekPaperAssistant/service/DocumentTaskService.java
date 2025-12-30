package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.DocumentTaskVO;

import java.util.List;

/**
 * 文档任务服务接口
 * 
 * @author 席崇援
 */
public interface DocumentTaskService {
    
    /**
     * 创建文档处理任务
     * 
     * @param documentId 文档ID
     * @param taskType 任务类型
     * @return 任务ID
     */
    Long createTask(Long documentId, String taskType);
    
    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID
     * @param status 状态
     * @param progress 进度
     * @param errorMessage 错误信息
     */
    void updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage);
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    DocumentTaskVO getTaskStatus(Long taskId);
    
    /**
     * 获取文档的所有任务
     * 
     * @param documentId 文档ID
     * @return 任务列表
     */
    List<DocumentTaskVO> getDocumentTasks(Long documentId);
    
    /**
     * 重试失败的任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean retryTask(Long taskId);
    
    /**
     * 获取待重试的任务列表
     * 
     * @return 任务列表
     */
    List<DocumentTaskVO> getPendingRetryTasks();
}
