package com.GeekPaperAssistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.GeekPaperAssistant.mapper.DocumentTaskMapper;
import com.GeekPaperAssistant.mapper.DocumentMapper;
import com.GeekPaperAssistant.mapper.DocumentVectorMapper;
import com.GeekPaperAssistant.mapper.DocumentChunkMapper;
import com.GeekPaperAssistant.model.entity.DocumentTaskDO;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import com.GeekPaperAssistant.model.entity.DocumentVectorDO;
import com.GeekPaperAssistant.model.entity.DocumentChunkDO;
import com.GeekPaperAssistant.mq.DocumentProcessingProducer;
import com.GeekPaperAssistant.model.vo.DocumentTaskVO;
import com.GeekPaperAssistant.service.DocumentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BusinessException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文档任务服务实现（整合了缓存逻辑）
 * 
 * 功能说明：
 * 1. 任务生命周期管理（创建、更新、查询、重试）
 * 2. Redis 缓存加速（进度查询、状态同步）
 * 3. 任务清理和补偿机制
 * 
 * 整合历史：
 * - 合并了 DocumentTaskCacheService 的缓存逻辑
 * - Redis 作为可选依赖，未配置时自动降级为纯 MySQL
 * 
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTaskServiceImpl implements DocumentTaskService {
    
    private final DocumentTaskMapper documentTaskMapper;
    private final DocumentMapper documentMapper;
    private final DocumentProcessingProducer documentProcessingProducer;
    private final DocumentVectorMapper documentVectorMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final VectorStore vectorStore;
    
    // Redis 可选依赖（未配置时为 null）
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final String TASK_PROGRESS_PREFIX = "task:progress:";
    private static final String TASK_STATUS_PREFIX = "task:status:";
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(Long documentId, String taskType) {
        log.info("创建文档任务: documentId={}, taskType={}", documentId, taskType);
        
        DocumentTaskDO task = DocumentTaskDO.builder()
            .documentId(documentId)
            .taskType(taskType)
            .status("pending")
            .retryCount(0)
            .maxRetries(DEFAULT_MAX_RETRIES)
            .progress(0)
            .createTime(LocalDateTime.now())
            .updateTime(LocalDateTime.now())
            .build();
        
        documentTaskMapper.insert(task);
        
        return task.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage) {
        DocumentTaskDO task = documentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        task.setStatus(status);
        task.setUpdateTime(LocalDateTime.now());
        
        if (progress != null) {
            task.setProgress(progress);
        }
        
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        
        if ("processing".equals(status) && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        
        if ("completed".equals(status) || "failed".equals(status)) {
            task.setEndTime(LocalDateTime.now());
            task.setProgress(100);
        }
        
        // 更新 MySQL
        documentTaskMapper.updateById(task);
        
        // 更新或清理 Redis 缓存（如果可用）
        if ("completed".equals(status) || "failed".equals(status)) {
            // 任务结束，清理缓存（避免占用 Redis 内存）
            removeTaskCache(taskId);
        } else {
            // 任务进行中，更新缓存加速查询
            updateCache(taskId, status, progress);
        }
        
        log.info("任务状态更新: taskId={}, status={}, progress={}", taskId, status, progress);
    }
    
    @Override
    public DocumentTaskVO getTaskStatus(Long taskId) {
        // 尝试从 Redis 读取进度（热数据加速）
        Integer cachedProgress = getProgressFromCache(taskId);
        
        DocumentTaskDO task = documentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        // 如果缓存有更新的进度，使用缓存值
        if (cachedProgress != null && cachedProgress > task.getProgress()) {
            task.setProgress(cachedProgress);
        }
        
        return convertToVO(task);
    }
    
    @Override
    public List<DocumentTaskVO> getDocumentTasks(Long documentId) {
        List<DocumentTaskDO> tasks = documentTaskMapper.selectList(
            new LambdaQueryWrapper<DocumentTaskDO>()
                .eq(DocumentTaskDO::getDocumentId, documentId)
                .orderByDesc(DocumentTaskDO::getCreateTime)
        );
        
        return tasks.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryTask(Long taskId) {
        DocumentTaskDO task = documentTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        if (!"failed".equals(task.getStatus())) {
            throw new BusinessException("只能重试失败的任务");
        }
        
        if (task.getRetryCount() >= task.getMaxRetries()) {
            throw new BusinessException("已达到最大重试次数");
        }
        
        // 校验文档未被逻辑删除
        DocumentDO document = documentMapper.selectById(task.getDocumentId());
        if (document == null || (document.getIsDeleted()!=null && document.getIsDeleted()==1)) {
            throw new BusinessException("文档已删除，无法重试");
        }

        // 清理旧的向量数据（重新处理前必须清空）
        cleanupDocumentVectors(task.getDocumentId());
        
        // 重置任务状态
        task.setStatus("pending");
        task.setRetryCount(task.getRetryCount() + 1);
        task.setProgress(0);
        task.setErrorMessage(null);
        task.setStartTime(null);
        task.setEndTime(null);
        task.setUpdateTime(LocalDateTime.now());
        
        documentTaskMapper.updateById(task);
        
        // 投递到队列重新处理
        try {
            documentProcessingProducer.sendDocumentProcessingMessage(task.getDocumentId(), document.getUserId(), task.getId());
            log.info("任务重试已重新投递: taskId={}, retryCount={}, documentId={}", 
                    taskId, task.getRetryCount(), task.getDocumentId());
        } catch (Exception e) {
            log.error("任务重试重新投递失败: taskId={}", taskId, e);
            throw new BusinessException("重试任务队列投递失败");
        }
        
        return true;
    }
    
    /**
     * 清理文档的向量数据（用于重新处理）
     */
    private void cleanupDocumentVectors(Long documentId) {
        try {
            // 1. 从映射表查询向量ID
            List<DocumentVectorDO> vectorMappings = documentVectorMapper.selectList(
                new LambdaQueryWrapper<DocumentVectorDO>()
                    .eq(DocumentVectorDO::getDocumentId, documentId)
            );
            
            if (!vectorMappings.isEmpty()) {
                // 2. 提取向量ID列表并从向量库删除
                List<String> vectorIds = vectorMappings.stream()
                    .map(DocumentVectorDO::getVectorId)
                    .toList();
                
                vectorStore.delete(vectorIds);
                log.info("清理文档向量: documentId={}, count={}", documentId, vectorIds.size());
                
                // 3. 删除映射记录
                documentVectorMapper.delete(
                    new LambdaQueryWrapper<DocumentVectorDO>()
                        .eq(DocumentVectorDO::getDocumentId, documentId)
                );
                
                // 4. 删除分块记录
                documentChunkMapper.delete(
                    new LambdaQueryWrapper<DocumentChunkDO>()
                        .eq(DocumentChunkDO::getDocumentId, documentId)
                );
            } else {
                log.info("文档无向量数据，跳过清理: documentId={}", documentId);
            }
        } catch (Exception e) {
            log.warn("清理文档向量失败（将继续重试）: documentId={}", documentId, e);
            // 不抛出异常，允许继续重试处理
        }
    }
    
    @Override
    public List<DocumentTaskVO> getPendingRetryTasks() {
        // 查询失败且未达到最大重试次数的任务
        List<DocumentTaskDO> tasks = documentTaskMapper.selectList(
            new LambdaQueryWrapper<DocumentTaskDO>()
                .eq(DocumentTaskDO::getStatus, "failed")
                .apply("retry_count < max_retries")
                .orderByAsc(DocumentTaskDO::getUpdateTime)
                .last("LIMIT 100") // 限制返回数量
        );
        
        return tasks.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换为 VO
     */
    private DocumentTaskVO convertToVO(DocumentTaskDO task) {
        Long duration = null;
        if (task.getStartTime() != null && task.getEndTime() != null) {
            duration = Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
        }
        
        return DocumentTaskVO.builder()
            .id(task.getId())
            .documentId(task.getDocumentId())
            .taskType(task.getTaskType())
            .status(task.getStatus())
            .retryCount(task.getRetryCount())
            .maxRetries(task.getMaxRetries())
            .errorMessage(task.getErrorMessage())
            .progress(task.getProgress())
            .startTime(task.getStartTime())
            .endTime(task.getEndTime())
            .duration(duration)
            .build();
    }
    
    // ==================== Redis 缓存方法（内部实现） ====================
    
    /**
     * 更新 Redis 缓存
     */
    private void updateCache(Long taskId, String status, Integer progress) {
        if (redisTemplate == null) {
            return; // Redis 未配置，跳过
        }
        
        try {
            if (progress != null) {
                String progressKey = TASK_PROGRESS_PREFIX + taskId;
                redisTemplate.opsForValue().set(progressKey, progress.toString(), CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            if (status != null) {
                String statusKey = TASK_STATUS_PREFIX + taskId;
                redisTemplate.opsForValue().set(statusKey, status, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
            
            log.debug("Redis 缓存已更新: taskId={}, status={}, progress={}", taskId, status, progress);
        } catch (Exception e) {
            log.warn("Redis 缓存更新失败（不影响主流程）: taskId={}", taskId, e);
        }
    }
    
    /**
     * 从 Redis 读取进度
     */
    private Integer getProgressFromCache(Long taskId) {
        if (redisTemplate == null) {
            return null; // Redis 未配置，返回 null
        }
        
        try {
            String key = TASK_PROGRESS_PREFIX + taskId;
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                return Integer.valueOf(value.toString());
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败（降级为 MySQL）: taskId={}", taskId, e);
        }
        
        return null;
    }
    
    /**
     * 清理任务缓存（任务完成或失败时调用）
     */
    private void removeTaskCache(Long taskId) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            redisTemplate.delete(TASK_PROGRESS_PREFIX + taskId);
            redisTemplate.delete(TASK_STATUS_PREFIX + taskId);
            log.debug("Redis 缓存已清理: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("Redis 缓存清理失败: taskId={}", taskId, e);
        }
    }
}
