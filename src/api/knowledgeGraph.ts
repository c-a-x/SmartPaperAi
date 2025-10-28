// 知识图谱相关 API
import { request } from '@/utils/request'
import type { ApiResponse, KnowledgeGraphVO } from '@/types'

// 构建全局知识图谱
export function getGlobalKnowledgeGraph(params?: {
  query?: string
  limit?: number
}) {
  return request.get<ApiResponse<KnowledgeGraphVO>>(
    '/api/rag/global-knowledge-graph',
    params
  )
}

// 构建文档知识图谱
export function getDocumentKnowledgeGraph(params: {
  documentId: number
  query: string
}) {
  return request.get<ApiResponse<KnowledgeGraphVO>>(
    '/api/rag/document-knowledge-graph',
    params
  )
}

// 构建知识库知识图谱
export function getKnowledgeBaseKnowledgeGraph(params: {
  knowledgeBaseId: number
  query?: string
  limit?: number
}) {
  return request.get<ApiResponse<KnowledgeGraphVO>>(
    '/api/rag/knowledge-base-knowledge-graph',
    params
  )
}
