import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { config } from '@/config'
import router from '@/router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: config.timeout,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (reqConfig) => {
    // 添加 token
    const token = localStorage.getItem(config.tokenKey)
    if (token && reqConfig.headers) {
      reqConfig.headers['Authorization'] = `Bearer ${token}`
    }
    return reqConfig
  },
  (error: any) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 如果是文件流，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    // 检查业务状态码
    if (res.code !== '0' && res.code !== '200') {
      ElMessage.error(res.msg || '请求失败')

      // 401 未授权，跳转登录页
      if (res.code === '401') {
        localStorage.removeItem(config.tokenKey)
        localStorage.removeItem(config.userInfoKey)
        router.push('/login')
      }

      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    return res
  },
  (error: any) => {
    console.error('响应错误:', error)

    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          localStorage.removeItem(config.tokenKey)
          localStorage.removeItem(config.userInfoKey)
          router.push('/login')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.msg || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络请求失败，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

// 导出请求方法
export default service

// 封装常用请求方法
export const request = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, { params, ...config })
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, { params, ...config })
  },

  upload<T = any>(url: string, file: File, onProgress?: (percent: number) => void): Promise<T> {
    const formData = new FormData()
    formData.append('file', file)

    return service.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent: any) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
  }
}
