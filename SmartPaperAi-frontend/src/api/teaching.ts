// 教学设计相关 API
import { request } from '@/utils/request'
import type {
  ApiResponse,
  TeachingPlanListVO,
  TeachingPlanVO,
  CreateTeachingPlanDTO,
  RagAnswerVO
} from '@/types'

// 获取教学设计列表
export function getTeachingPlanList() {
  return request.get<ApiResponse<TeachingPlanListVO[]>>('/api/teaching-plans')
}

// 获取教学设计详情
export function getTeachingPlanDetail(id:any) {
  return request.get<ApiResponse<TeachingPlanVO>>(`/api/teaching-plans/${id}`)
}

// 保存教学设计
export function createTeachingPlan(data: CreateTeachingPlanDTO) {
  return request.post<ApiResponse<number>>('/api/teaching-plans', data)
}

// 更新教学设计（暂无对应API，使用POST保存）
// export function updateTeachingPlan(id: number, data: CreateTeachingPlanDTO) {
//   return request.post<ApiResponse<number>>('/api/teaching-plans', data)
// }

// 删除教学设计
export function deleteTeachingPlan(id: any) {
  return request.delete<ApiResponse<void>>(`/api/teaching-plans/${id}`)
}

// 生成教学设计
export function generateTeachingPlan(params: {
  topic: string
  grade: string
  subject: string
  documentIds?: number[]
}) {
  return request.post<ApiResponse<RagAnswerVO<TeachingPlanVO>>>(
    '/api/teaching-plans/generate',
    null,
    { params }
  )
}
