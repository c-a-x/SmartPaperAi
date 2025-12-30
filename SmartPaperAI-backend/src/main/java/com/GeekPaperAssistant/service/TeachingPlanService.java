package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.vo.RagAnswerVO;
import com.GeekPaperAssistant.model.vo.TeachingPlanListVO;
import com.GeekPaperAssistant.model.vo.TeachingPlanVO;

import java.util.List;

/**
 * 教学设计服务接口
 * 
 * @author 席崇援
 */
public interface TeachingPlanService {
    
    /**
     * 生成教学设计
     * 
     * @param topic 主题
     * @param grade 年级
     * @param subject 学科
     * @param documentIds 参考文档ID列表（可选）
     * @return 教学设计结果（包含引用）
     */
    RagAnswerVO<TeachingPlanVO> generateTeachingPlan(String topic, String grade, String subject, List<Long> documentIds);
    
    /**
     * 保存教学设计
     * 
     * @param teachingPlanVO 教学设计VO
     * @return 保存后的教学设计ID
     */
    Long saveTeachingPlan(TeachingPlanVO teachingPlanVO);
    
    /**
     * 获取用户的教学设计列表
     * 
     * @param userId 用户ID
     * @return 教学设计列表
     */
    List<TeachingPlanListVO> getUserTeachingPlans(Long userId);
    
    /**
     * 获取教学设计详情
     * 
     * @param planId 教学设计ID
     * @return 教学设计详情
     */
    TeachingPlanVO getTeachingPlan(Long planId);
    
    /**
     * 删除教学设计
     * 
     * @param planId 教学设计ID
     */
    void deleteTeachingPlan(Long planId);
}
