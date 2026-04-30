import axios from 'axios'
import type { ApiResponse } from '@/types'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { addError } from '@/utils/errorHandler'
import { updateServerTimeOffset } from '@/utils'

// 从环境变量读取 API 基础路径
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// ==================== 请求取消管理 ====================
const abortControllers = new Map<string, AbortController>()

export function getAbortController(key: string): AbortController {
  const existing = abortControllers.get(key)
  if (existing) existing.abort()
  const controller = new AbortController()
  abortControllers.set(key, controller)
  return controller
}

export function clearAbortController(key: string) {
  abortControllers.delete(key)
}

export function cancelAllRequests() {
  for (const [, controller] of abortControllers) {
    controller.abort()
  }
  abortControllers.clear()
}

// ==================== Token 刷新（防并发） ====================
let refreshPromise: Promise<string | null> | null = null
/** 标记是否正在重试队列中的请求，防止 finally 过早清除 refreshPromise */
let isRetryingPending = false

/**
 * 使用 Refresh Token 刷新 Access Token
 * 防并发：多个请求同时 401 时，只发一次刷新请求（refreshPromise 去重）
 *
 * 修复竞态条件：在 pendingRequests 队列重试完成前，不清除 refreshPromise，
 * 避免重试中的请求再次 401 时触发新的刷新请求。
 */
function doRefreshToken(): Promise<string | null> {
  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    const userStore = useUserStore()
    const rt = userStore.refreshToken
    if (!rt) return null

    try {
      // 后端期望：refreshToken 放在请求体
      const res = await axios.post(`${API_BASE}/user/refresh`, { refreshToken: rt }, {
        timeout: 10000
      })
      const data = res.data
      if (data.code === 0 && data.data) {
        const { token: newToken, refreshToken: newRefreshToken, userInfo } = data.data
        userStore.setAuth(newToken, userInfo || userStore.user!, newRefreshToken)

        // 重试队列中的请求（在清除 refreshPromise 之前完成）
        isRetryingPending = true
        retryPendingRequests(newToken)
        isRetryingPending = false

        return newToken
      }
    } catch {
      // 刷新失败
    }
    return null
  })()

  // 仅在非重试状态下清除 refreshPromise，避免竞态
  refreshPromise.finally(() => {
    if (!isRetryingPending) {
      refreshPromise = null
    } else {
      // 延迟清除：给重试中的请求一点时间完成
      setTimeout(() => { refreshPromise = null }, 100)
    }
  })

  return refreshPromise
}

function handleAuthExpired() {
  const userStore = useUserStore()
  userStore.logout()
  // 清空等待 Token 刷新的请求队列，避免请求永远挂起
  pendingRequests = []
  addError('auth', '登录已过期，请重新登录')
  ElMessage.warning('登录已过期，请重新登录')
  window.dispatchEvent(new CustomEvent('auth:expired'))
}

// ==================== 请求队列（等待 Token 刷新） ====================
let pendingRequests: Array<(token: string) => void> = []

function addPendingRequest(callback: (token: string) => void) {
  pendingRequests.push(callback)
}

function retryPendingRequests(newToken: string) {
  pendingRequests.forEach(cb => cb(newToken))
  pendingRequests = []
}

// ==================== 请求拦截 ====================
api.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// ==================== 响应拦截 ====================
api.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse
    if (data.timestamp) {
      updateServerTimeOffset(data.timestamp)
    }
    // 9997 = Token 过期（后端自定义错误码）
    if (data.code === 9997) {
      const originalRequest = response.config
      if (!originalRequest._isRetry) {
        originalRequest._isRetry = true
        return doRefreshToken().then((newToken) => {
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return api.request(originalRequest)
          }
          handleAuthExpired()
          return response
        })
      }
      handleAuthExpired()
      return response
    }
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const originalRequest = error.config

      if (status === 429) {
        addError('rateLimit', '请求过于频繁，请稍后再试')
        ElMessage.warning('请求过于频繁，请稍后再试')
      } else if (status === 401 && !originalRequest._isRetry) {
        originalRequest._isRetry = true
        // 优先尝试 Refresh Token
        return doRefreshToken().then((newToken) => {
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return api.request(originalRequest)
          }
          handleAuthExpired()
          return Promise.reject(error)
        })
      } else if (status === 401) {
        handleAuthExpired()
      } else if (status >= 500) {
        addError('server', '服务器错误，请稍后重试')
        ElMessage.error('服务器错误，请稍后重试')
      }
    } else if (error.code === 'ECONNABORTED') {
      addError('timeout', '请求超时，请检查网络')
      ElMessage.error('请求超时，请检查网络')
    } else if (axios.isCancel(error)) {
      return Promise.reject(error)
    } else {
      addError('network', '网络错误，请检查连接')
      ElMessage.error('网络错误，请检查连接')
    }
    return Promise.reject(error)
  }
)

// ==================== 辅助函数 ====================
// 改进泛型类型安全：data 参数支持任意类型（DTO 对象、FormData 等）

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  const res = await api.get(url, { params })
  return res.data as ApiResponse<T>
}

export async function post<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  const res = await api.post(url, data)
  return res.data as ApiResponse<T>
}

export async function put<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  const res = await api.put(url, data)
  return res.data as ApiResponse<T>
}

export async function del<T>(url: string): Promise<ApiResponse<T>> {
  const res = await api.delete(url)
  return res.data as ApiResponse<T>
}

export default api
