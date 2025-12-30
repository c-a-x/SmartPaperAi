// 本地存储工具类
import { config } from '@/config'
import type { UserInfo } from '@/types'

// Token 操作
export const tokenStorage = {
  get(): string | null {
    return localStorage.getItem(config.tokenKey)
  },
  set(token: string): void {
    localStorage.setItem(config.tokenKey, token)
  },
  remove(): void {
    localStorage.removeItem(config.tokenKey)
  }
}

// 用户信息操作
export const userInfoStorage = {
  get(): UserInfo | null {
    const userInfoStr = localStorage.getItem(config.userInfoKey)
    return userInfoStr ? JSON.parse(userInfoStr) : null
  },
  set(userInfo: UserInfo): void {
    localStorage.setItem(config.userInfoKey, JSON.stringify(userInfo))
  },
  remove(): void {
    localStorage.removeItem(config.userInfoKey)
  }
}

// 清空所有存储
export function clearAllStorage(): void {
  tokenStorage.remove()
  userInfoStorage.remove()
}
