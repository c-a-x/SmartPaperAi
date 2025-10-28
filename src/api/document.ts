// 文档管理相关 API
import { request } from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  DocumentListVO,
  DocumentDetailVO,
  DocumentSearchRequest,
  DocumentMetadata
} from '@/types'

// 引用格式数据类型
export interface CitationFormats {
  apa?: string
  bibtex?: string
  mla?: string
  gbt7714?: string
}

// 文档上传响应
export interface UploadDocumentResponse {
  documentId: string
  taskId: number
  title: string
  taskStatusUrl: string
}

// 文档任务状态
export interface DocumentTaskVO {
  id: number
  documentId: number
  taskType: string
  status: string  // processing, completed, failed
  retryCount: number
  maxRetries: number
  errorMessage?: string
  progress: number  // 0-100
  startTime?: string
  endTime?: string
  duration?: number
}

// 任务重试响应
export interface TaskRetryVO {
  message: string
  taskId: number
  success: boolean
}

// 上传文档
export function uploadDocument(file: File, onProgress?: (percent: number) => void) {
  return request.upload<ApiResponse<UploadDocumentResponse>>('/api/documents/upload', file, onProgress)
}

// 获取文档列表（分页）
export function getDocumentList(params?: DocumentSearchRequest) {
  return request.get<ApiResponse<PageResult<DocumentListVO>>>('/api/documents/list',  params)
}

// 搜索文档
export function searchDocuments(params: DocumentSearchRequest) {
  return request.post<ApiResponse<DocumentListVO[]>>('/api/documents/search', params)
}

// 获取文档详情
export function getDocumentDetail(documentId: number) {
  return request.get<ApiResponse<DocumentDetailVO>>(`/api/documents/${documentId}`)
}

// 获取文档元数据
export function getDocumentMetadata(documentId: number) {
  return request.get<ApiResponse<DocumentMetadata>>(`/api/documents/${documentId}/metadata`)
}

// 获取文档内容
export function getDocumentContent(documentId: number) {
  return request.get<ApiResponse<string>>(`/api/documents/${documentId}/content`)
}

// 预览文档（获取二进制流）
export function previewDocument(documentId: string) {
  return request.get<Blob>(`/api/documents/${documentId}/preview`, {
    responseType: 'blob'  // 设置响应类型为二进制流
  }) as Promise<any>  // 返回整个axios response对象
}

// 生成文档摘要
export function summarizeDocument(documentId: number) {
  return request.post<ApiResponse<string>>(`/api/documents/${documentId}/summarize`)
}

// 获取文档引用信息
export function getDocumentCitation(documentId: string) {
  return request.get<ApiResponse<CitationFormats>>(`/api/documents/${documentId}/citation`)
}

// 删除文档
export function deleteDocument(documentId: string | number) {
  return request.delete<ApiResponse<void>>(`/api/documents/${documentId}`)
}

// 批量获取引用信息
export function batchGetCitations(documentIds: number[]) {
  return request.post<ApiResponse<Record<number, string>>>('/api/documents/citations/batch', {
    documentIds
  })
}

// 文档对比
export function compareDocuments(documentIds: number[]) {
  return request.post<ApiResponse<any>>('/api/documents/compare', { documentIds })
}

// 获取文档任务状态
export function getDocumentTaskStatus(taskId: number) {
  return request.get<ApiResponse<DocumentTaskVO>>(`/api/documents/tasks/${taskId}`)
}

// 重试文档处理
export function retryDocumentTask(taskId: number) {
  return request.post<ApiResponse<TaskRetryVO>>(`/api/documents/tasks/${taskId}/retry`)
}
