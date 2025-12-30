package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.GeekPaperAssistant.model.vo.RagAnswerVO;
import com.GeekPaperAssistant.model.vo.TeachingPlanListVO;
import com.GeekPaperAssistant.model.vo.TeachingPlanVO;
import com.GeekPaperAssistant.service.TeachingPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学设计控制器
 *
 * @author 席崇援
 * @since 2025-10-07
 */
@Tag(name = "教学设计管理", description = "教学设计的增删改查功能")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching-plans")
@SaCheckLogin
public class TeachingPlanController {
    
    private final TeachingPlanService teachingPlanService;
    
    /**
     * 生成教学设计 - AI 生成（带引用）
     */
    @Operation(summary = "生成教学设计", description = "结合主题/年级/学科与可选文档，使用AI生成结构化教学方案（目标/步骤/作业/资源），附引用片段")
    @PostMapping("/generate")
    public RagAnswerVO<TeachingPlanVO> generateTeachingPlan(
            @Parameter(description = "课题") @RequestParam String topic,
            @Parameter(description = "学段") @RequestParam String grade,
            @Parameter(description = "学科") @RequestParam String subject,
            @Parameter(description = "参考文档ID列表") @RequestParam(required = false) List<Long> documentIds) {
        
        return teachingPlanService.generateTeachingPlan(topic, grade, subject, documentIds);
    }
    
    /**
     * 保存教学设计
     * 
     * @param teachingPlanVO 教学设计内容
     * @return 教案ID
     */
    @Operation(summary = "保存教学设计", description = "保存AI生成的教学设计")
    @PostMapping
    public Long saveTeachingPlan(
            @Parameter(description = "教学设计内容") @RequestBody @Validated TeachingPlanVO teachingPlanVO) {
        return teachingPlanService.saveTeachingPlan(teachingPlanVO);
    }
    
    /**
     * 获取教学设计列表
     * 
     * @return 教学设计列表
     */
    @Operation(summary = "获取教学设计列表", description = "获取当前用户的所有教学设计")
    @GetMapping
    public List<TeachingPlanListVO> getUserTeachingPlans() {
        Long userId = StpUtil.getLoginIdAsLong();
        return teachingPlanService.getUserTeachingPlans(userId);
    }
    
    /**
     * 获取教学设计详情
     * 
     * @param planId 教案ID
     * @return 教学设计详情
     */
    @Operation(summary = "获取教学设计详情", description = "查看教学设计的完整内容")
    @GetMapping("/{planId}")
    public TeachingPlanVO getTeachingPlan(
            @Parameter(description = "教案ID") @PathVariable Long planId) {
        return teachingPlanService.getTeachingPlan(planId);
    }
    
    /**
     * 删除教学设计
     * 
     * @param planId 教案ID
     */
    @Operation(summary = "删除教学设计", description = "删除指定的教学设计")
    @DeleteMapping("/{planId}")
    public void deleteTeachingPlan(
            @Parameter(description = "教案ID") @PathVariable Long planId) {
        teachingPlanService.deleteTeachingPlan(planId);
    }
}
