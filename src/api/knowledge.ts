// 知识库管理相关 API
import { request } from '@/utils/request'
import type { ApiResponse, PageResult, KnowledgeBaseDO, CreateKBDTO } from '@/types'

// 获取知识库列表（分页）
export function getKnowledgeBaseList(params?: { current?: number; size?: number; name?: string }) {
  return request.get<ApiResponse<PageResult<KnowledgeBaseDO>>>('/api/kb/list',  params )
}

// 获取知识库详情
export function getKnowledgeBaseDetail(kbId: any) {
  return request.get<ApiResponse<KnowledgeBaseDO>>(`/api/kb/${kbId}`)
}

// 创建知识库
export function createKnowledgeBase(data: CreateKBDTO) {
  return request.post<ApiResponse<number>>('/api/kb', data)
}

// 更新知识库
export function updateKnowledgeBase(kbId: number, data: CreateKBDTO) {
  return request.put<ApiResponse<void>>(`/api/kb/${kbId}`, data)
}

// 删除知识库
export function deleteKnowledgeBase(kbId: number) {
  return request.delete<ApiResponse<void>>(`/api/kb/${kbId}`)
}

// 添加文档到知识库
export function addDocumentToKB(kbId: number, documentId: number) {
  return request.post<ApiResponse<void>>(`/api/kb/${kbId}/documents/${documentId}`)
}

// 从知识库移除文档（取消归档）
export function removeDocumentFromKB(kbId: any, documentIds: any) {
  return request.post<ApiResponse<void>>(`/api/kb/${kbId}/documents/remove`, documentIds)
}

// 批量添加文档到知识库
export function batchAddDocumentsToKB(kbId: any, documentIds: any) {
  return request.post<ApiResponse<void>>(`/api/kb/${kbId}/documents/batch`, documentIds )
}
