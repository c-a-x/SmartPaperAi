// AI 聊天相关 API
import { request } from '@/utils/request'
import type {
  ApiResponse,
  ChatRequest,
  ChatResponse,
  RagChatRequest,
  RagChatResponse,
  ConversationVO,
  ChatMessage
} from '@/types'

// 普通 AI 聊天
export function chat(data: ChatRequest) {
  return request.post<ApiResponse<ChatResponse>>('/ai/chat', data)
}

// 流式 AI 聊天
export function chatStream(data: ChatRequest) {
  return request.post('/ai/chat/stream', data, {
    responseType: 'stream'
  })
}

// 文档对话
export function documentChat(documentId: string, query: string, useGraphEnhancement?: boolean) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/document-chat', null, {
    params: { documentId, query, useGraphEnhancement }
  })
}

// 知识库问答
export function knowledgeBaseChat(knowledgeBaseId: number, query: string, useGraphEnhancement?: boolean) {
  return request.post<ApiResponse<RagChatResponse>>('/api/rag/knowledge-base-chat', null, {
    params: { knowledgeBaseId, query, useGraphEnhancement }
  })
}

// 获取会话列表
export function getConversationList(userId?: number) {
  return request.get<ApiResponse<ConversationVO[]>>('/ai/sessions', {
    params: { userId }
  })
}

// 创建新会话
export function createConversation(title?: string, userId?: number) {
  return request.post<ApiResponse<string>>('/ai/sessions', null, {
    params: { title, userId }
  })
}

// 删除会话
export function deleteConversation(conversationId: string) {
  return request.delete<ApiResponse<void>>(`/ai/sessions/${conversationId}`)
}

// 获取会话历史
export function getConversationHistory(conversationId: string) {
  return request.get<ApiResponse<ChatMessage[]>>(`/ai/sessions/${conversationId}/history`)
}

// 清空会话历史
export function clearConversationHistory(conversationId: string) {
  return request.delete<ApiResponse<void>>(`/ai/sessions/${conversationId}/history`)
}
