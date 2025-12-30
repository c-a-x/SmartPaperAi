package com.GeekPaperAssistant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 * 
 * @author 席崇援
 */
@RestController
public class IndexController {
    
    /**
     * 首页欢迎信息
     */
    @GetMapping("/")
    public Map<String, Object> index() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "SmartPaperAI");
        response.put("description", "基于 RAG 的论文智能问答系统");
        response.put("version", "0.0.1-SNAPSHOT");
        response.put("status", "running");
        response.put("message", "服务正常运行中");
        response.put("apiDocs", "/swagger-ui.html");
        return response;
    }
}
