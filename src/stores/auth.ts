// 用户认证状态管理
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo, LoginRequest, RegisterRequest } from '@/types'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import { tokenStorage, userInfoStorage, clearAllStorage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(tokenStorage.get())
  const userInfo = ref<UserInfo | null>(userInfoStorage.get())
  const isLoggedIn = ref<boolean>(!!token.value)

  // 登录
  async function login(loginData: LoginRequest) {
    try {
      const response = await loginApi(loginData)
      const { token: newToken, userInfo: newUserInfo } = response.data

      // 保存到状态
      token.value = newToken
      userInfo.value = newUserInfo
      isLoggedIn.value = true

      // 保存到本地存储
      tokenStorage.set(newToken)
      userInfoStorage.set(newUserInfo)

      return response
    } catch (error) {
      throw error
    }
  }

  // 注册
  async function register(registerData: RegisterRequest) {
    try {
      const response = await registerApi(registerData)
      return response
    } catch (error) {
      throw error
    }
  }

  // 退出登录
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      // 清空状态
      token.value = null
      userInfo.value = null
      isLoggedIn.value = false

      // 清空本地存储
      clearAllStorage()
    }
  }

  // 更新用户信息
  function updateUserInfo(newUserInfo: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...newUserInfo }
      userInfoStorage.set(userInfo.value)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    logout,
    updateUserInfo
  }
})
