/**
 * 全局错误状态管理
 *
 * 替代直接弹窗的方式，通过全局状态管理错误，
 * 组件可以按需展示错误信息（弹窗、行内提示、Toast 等）。
 */
import { ref } from 'vue'

export interface ErrorInfo {
  id: number
  type: 'network' | 'auth' | 'rateLimit' | 'server' | 'timeout'
  message: string
  timestamp: number
}

const errors = ref<ErrorInfo[]>([])
let errorIdCounter = 0

/** 最大保留错误数 */
const MAX_ERRORS = 10

/** 重复错误抑制时间（ms） */
const DEDUP_INTERVAL = 3000

/** 最近的错误消息（用于去重） */
const recentMessages = new Map<string, number>()

/**
 * 添加错误
 */
export function addError(type: ErrorInfo['type'], message: string): ErrorInfo {
  const now = Date.now()

  // 去重：相同消息在 DEDUP_INTERVAL 内不重复添加
  const lastTime = recentMessages.get(message)
  if (lastTime && now - lastTime < DEDUP_INTERVAL) {
    return errors.value[errors.value.length - 1]
  }
  recentMessages.set(message, now)

  const error: ErrorInfo = {
    id: ++errorIdCounter,
    type,
    message,
    timestamp: now
  }

  errors.value.push(error)

  // 超过最大数量，移除最旧的
  if (errors.value.length > MAX_ERRORS) {
    errors.value.shift()
  }

  return error
}

/**
 * 清除指定错误
 */
export function removeError(id: number) {
  const idx = errors.value.findIndex(e => e.id === id)
  if (idx !== -1) errors.value.splice(idx, 1)
}

/**
 * 清除所有错误
 */
export function clearErrors() {
  errors.value = []
  recentMessages.clear()
}

/**
 * 获取错误列表
 */
export function useErrors() {
  return {
    errors,
    addError,
    removeError,
    clearErrors
  }
}
