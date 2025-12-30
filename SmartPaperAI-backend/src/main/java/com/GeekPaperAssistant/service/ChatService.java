package com.GeekPaperAssistant.service;

import com.GeekPaperAssistant.model.dto.ChatRequestDTO;
import com.GeekPaperAssistant.model.vo.ChatMessageVO;
import com.GeekPaperAssistant.model.vo.ChatResponseVO;
import com.GeekPaperAssistant.model.vo.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 聊天服务
 */
public interface ChatService {

    /**
     * 发送聊天消息
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponseVO chat(ChatRequestDTO request);

    /**
     * 流式发送聊天消息 (SSE)
     * 
     * @param request 聊天请求
     * @return 流式响应
     */
    SseEmitter chatStream(ChatRequestDTO request);

    /**
     * 获取用户的所有会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSessionVO> getUserSessions(Long userId);

    /**
     * 获取会话的历史消息
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ChatMessageVO> getSessionHistory(String conversationId);

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 会话ID
     */
    String createSession(Long userId, String title);

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     */
    void deleteSession(String conversationId);

    /**
     * 清空会话历史记录
     *
     * @param conversationId 会话ID
     */
    void clearSessionHistory(String conversationId);
}
