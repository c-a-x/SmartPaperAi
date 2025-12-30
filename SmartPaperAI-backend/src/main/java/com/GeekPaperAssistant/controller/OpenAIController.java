package com.GeekPaperAssistant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.GeekPaperAssistant.model.dto.ChatRequestDTO;
import com.GeekPaperAssistant.model.vo.ChatMessageVO;
import com.GeekPaperAssistant.model.vo.ChatResponseVO;
import com.GeekPaperAssistant.model.vo.ChatSessionVO;
import com.GeekPaperAssistant.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.continew.starter.log.annotation.Log;

import java.util.List;

/**
 * DeepSeek AI 聊天控制器
 * 提供与 DeepSeek AI 模型交互的接口,支持上下文对话和历史记录管理
 */
@Tag(name = "DeepSeek AI 聊天接口")
@RestController
@Log(ignore = true)
@RequestMapping("/ai")
@RequiredArgsConstructor
@SaCheckLogin
public class OpenAIController {

    private final ChatService chatService;

    /**
     * 发送聊天消息 - 支持上下文对话
     * POST /deepseek/chat
     * 
     * @param request 聊天请求
     * @return AI 回复
     */
    @Operation(summary = "发送聊天消息", description = "支持上下文对话,如果不传conversationId则创建新会话")
    @PostMapping("/chat")
    public ChatResponseVO chat(@Validated @RequestBody ChatRequestDTO request) {
        return chatService.chat(request);
    }

    /**
     * 流式发送聊天消息 - SSE 流式传输
     * POST /deepseek/chat/stream
     * 
     * @param request 聊天请求
     * @return AI 流式回复
     */
    @Operation(summary = "流式发送聊天消息", description = "使用SSE流式传输AI回复,逐字显示")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Validated @RequestBody ChatRequestDTO request) {
        return chatService.chatStream(request);
    }

    /**
     * 获取用户的所有会话列表
     * GET /deepseek/sessions
     * 
     * @param userId 用户ID(可选)
     * @return 会话列表
     */
    @Operation(summary = "获取会话列表", description = "获取用户的所有会话列表")
    @GetMapping("/sessions")
    public List<ChatSessionVO> getSessions(@Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        return chatService.getUserSessions(userId);
    }

    /**
     * 获取会话的历史消息
     * GET /deepseek/sessions/{conversationId}/history
     * 
     * @param conversationId 会话ID
     * @return 历史消息列表
     */
    @Operation(summary = "获取会话历史", description = "获取指定会话的所有历史消息")
    @GetMapping("/sessions/{conversationId}/history")
    public List<ChatMessageVO> getSessionHistory(@Parameter(description = "会话ID", required = true) @PathVariable String conversationId) {
        return chatService.getSessionHistory(conversationId);
    }

    /**
     * 创建新会话
     * POST /deepseek/sessions
     * 
     * @param userId 用户ID(可选)
     * @param title  会话标题(可选)
     * @return 会话ID
     */
    @Operation(summary = "创建新会话", description = "创建一个新的聊天会话")
    @PostMapping("/sessions")
    public String createSession(@Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
                                @Parameter(description = "会话标题") @RequestParam(required = false) String title) {
        return chatService.createSession(userId, title);
    }

    /**
     * 删除会话
     * DELETE /deepseek/sessions/{conversationId}
     * 
     * @param conversationId 会话ID
     */
    @Operation(summary = "删除会话", description = "删除指定会话及其所有历史消息")
    @DeleteMapping("/sessions/{conversationId}")
    public void deleteSession(@Parameter(description = "会话ID", required = true) @PathVariable String conversationId) {
        chatService.deleteSession(conversationId);
    }

    /**
     * 清空会话历史记录
     * DELETE /deepseek/sessions/{conversationId}/history
     * 
     * @param conversationId 会话ID
     */
    @Operation(summary = "清空会话历史", description = "清空指定会话的所有历史消息")
    @DeleteMapping("/sessions/{conversationId}/history")
    public void clearSessionHistory(@Parameter(description = "会话ID", required = true) @PathVariable String conversationId) {
        chatService.clearSessionHistory(conversationId);
    }
}
