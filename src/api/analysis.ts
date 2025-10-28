// 论文分析相关 API
import { request } from '@/utils/request'
import type {
  ApiResponse,
  RagAnswerVO,
  PaperSummaryVO,
  PaperComparisonVO,
  PaperComparisonDTO,
  InnovationClusterVO,
  LiteratureReviewVO
} from '@/types'

// 论文总结
export function summarizeDocument(documentId: number) {
  return request.post<ApiResponse<RagAnswerVO<PaperSummaryVO>>>(
    `/api/documents/${documentId}/summarize`
  )
}

// 论文对比
export function compareDocuments(data: PaperComparisonDTO) {
  return request.post<ApiResponse<RagAnswerVO<PaperComparisonVO>>>(
    '/api/documents/compare',
    data
  )
}

// 创新点聚合
export function aggregateInnovations(documentIds: number[]) {
  return request.post<ApiResponse<InnovationClusterVO[]>>(
    '/api/documents/innovations/aggregate',
    documentIds
  )
}

// 文献综述生成
export function generateLiteratureReview(documentIds: number[]) {
  return request.post<ApiResponse<RagAnswerVO<LiteratureReviewVO>>>(
    '/api/documents/literature-review',
    documentIds
  )
}
