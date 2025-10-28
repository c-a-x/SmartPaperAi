// 认证相关 API
import { request } from '@/utils/request'
import type { ApiResponse, LoginRequest, LoginVO, RegisterRequest } from '@/types'

// 获取验证码
export function getCaptcha() {
  return request.get<ApiResponse<{ captchaKey: string; captchaImage: string }>>('/auth/captcha')
}

// 用户登录
export function login(data: LoginRequest) {
  return request.post<ApiResponse<LoginVO>>('/auth/login', data)
}

// 用户注册
export function register(data: RegisterRequest) {
  return request.post<ApiResponse<void>>('/auth/register', data)
}

// 退出登录
export function logout() {
  return request.post<ApiResponse<void>>('/auth/logout')
}

// 检查登录状态
export function checkAuth() {
  return request.get<ApiResponse<boolean>>('/auth/check')
}

// 获取当前用户ID
export function getCurrentUserId() {
  return request.get<ApiResponse<number>>('/auth/current-user-id')
}
