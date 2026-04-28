/**
 * 安全错误处理工具
 * 
 * 统一的错误消息提取和日志记录
 */

import { createLogger } from './logger'

const logger = createLogger('Error')

/** 开发环境判断 */
const isDev = import.meta.env.DEV

/**
 * 从未知错误中安全提取消息
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  if (error && typeof error === 'object' && 'message' in error) {
    return String((error as { message: unknown }).message)
  }
  return '未知错误'
}

/**
 * 安全的 catch 处理 — 提取消息并可选记录
 */
export function handleError(error: unknown, context?: string): string {
  const message = getErrorMessage(error)
  if (context) {
    logger.error(context, error)
  }
  return message
}

/**
 * 创建类型安全的 catch 处理函数
 */
export function createErrorHandler(context: string) {
  return (error: unknown) => {
    const message = getErrorMessage(error)
    logger.error(context, error)
    return message
  }
}

/**
 * API 错误响应类型
 */
export interface ApiErrorResponse {
  code: number
  msg: string
  data?: unknown
}

/**
 * 从 API 错误响应中提取消息
 */
export function getApiErrorMessage(error: unknown): string {
  if (error && typeof error === 'object') {
    const err = error as Record<string, unknown>
    if (err.response && typeof err.response === 'object') {
      const response = err.response as Record<string, unknown>
      if (response.data && typeof response.data === 'object') {
        const data = response.data as ApiErrorResponse
        if (data.msg) return data.msg
      }
    }
    if (err.message) return String(err.message)
  }
  return getErrorMessage(error)
}
