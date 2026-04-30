/**
 * API 错误处理工具
 *
 * 提供统一的 API 错误处理、消息提取和用户提示
 */
import axios, { AxiosError, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { addError, addErrorFromResponse } from '@/utils/errorHandler'
import type { ApiResponse } from '@/types'
import type { ErrorCodeType } from '@/types/errors'

export interface ApiErrorDetail {
  code: ErrorCodeType
  msg: string
  url?: string
  method?: string
}

export function extractErrorFromResponse(response: AxiosResponse): ApiErrorDetail | null {
  const data = response.data as ApiResponse | undefined

  if (data && typeof data.code === 'number' && typeof data.msg === 'string') {
    return {
      code: data.code as ErrorCodeType,
      msg: data.msg,
      url: response.config.url,
      method: response.config.method
    }
  }

  return null
}

export function extractErrorFromAxiosError(error: AxiosError): ApiErrorDetail | null {
  if (error.response) {
    const apiError = extractErrorFromResponse(error.response)
    if (apiError) return apiError

    return {
      code: (error.response.status || 500) as ErrorCodeType,
      msg: getHttpStatusMessage(error.response.status),
      url: error.config?.url,
      method: error.config?.method
    }
  }

  if (error.request) {
    return {
      code: 0,
      msg: '网络连接失败，请检查网络'
    }
  }

  return null
}

function getHttpStatusMessage(status: number): string {
  const messages: Record<number, string> = {
    400: '请求参数错误',
    401: '登录已过期，请重新登录',
    403: '没有权限访问该资源',
    404: '请求的资源不存在',
    405: '请求方法不支持',
    408: '请求超时',
    409: '请求冲突',
    413: '文件过大',
    422: '数据验证失败',
    429: '请求过于频繁，请稍后重试',
    500: '服务器内部错误',
    502: '网关错误',
    503: '服务暂时不可用',
    504: '网关超时'
  }

  return messages[status] || `请求失败 (${status})`
}

export function handleApiError(error: unknown, options?: { showMessage?: boolean; addToGlobal?: boolean }): ApiErrorDetail | null {
  const { showMessage = true, addToGlobal = true } = options || {}

  if (axios.isCancel(error)) {
    return null
  }

  const apiError = extractErrorFromAxiosError(error as AxiosError)

  if (apiError) {
    if (showMessage && apiError.msg) {
      ElMessage.error(apiError.msg)
    }

    if (addToGlobal) {
      addErrorFromResponse(apiError.code as number, apiError.msg)
    }
  } else {
    const genericMsg = '操作失败，请稍后重试'
    if (showMessage) {
      ElMessage.error(genericMsg)
    }
    if (addToGlobal) {
      addError('server', genericMsg)
    }
  }

  return apiError
}

export function showSuccessMessage(message: string = '操作成功') {
  ElMessage.success(message)
}

export function showWarningMessage(message: string) {
  ElMessage.warning(message)
}

export function showInfoMessage(message: string) {
  ElMessage.info(message)
}
