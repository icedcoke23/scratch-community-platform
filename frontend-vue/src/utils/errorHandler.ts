/**
 * 全局错误状态管理
 *
 * 替代直接弹窗的方式，通过全局状态管理错误，
 * 组件可以按需展示错误信息（弹窗、行内提示、Toast 等）。
 */
import { ref, computed } from 'vue'
import type { ErrorInfo, ErrorCodeType } from '@/types/errors'
import { getErrorTypeFromCode, formatErrorMessage } from '@/types/errors'

const errors = ref<ErrorInfo[]>([])
let errorIdCounter = 0

const MAX_ERRORS = 10
const DEDUP_INTERVAL = 3000
const recentMessages = new Map<string, number>()

export function addError(
  type: ErrorInfo['type'],
  message: string,
  options?: { code?: number; details?: string }
): ErrorInfo {
  const now = Date.now()
  const lastTime = recentMessages.get(message)
  if (lastTime && now - lastTime < DEDUP_INTERVAL) {
    return errors.value[errors.value.length - 1]
  }
  recentMessages.set(message, now)

  const error: ErrorInfo = {
    id: ++errorIdCounter,
    type,
    message,
    timestamp: now,
    code: options?.code,
    details: options?.details
  }

  errors.value.push(error)

  if (errors.value.length > MAX_ERRORS) {
    errors.value.shift()
  }

  return error
}

export function addErrorFromResponse(code: number, msg: string): ErrorInfo {
  const type = getErrorTypeFromCode(code)
  const formattedMsg = formatErrorMessage(msg, code)
  return addError(type, formattedMsg, { code })
}

export function removeError(id: number) {
  const idx = errors.value.findIndex(e => e.id === id)
  if (idx !== -1) errors.value.splice(idx, 1)
}

export function clearErrors() {
  errors.value = []
  recentMessages.clear()
}

export const recentErrors = computed(() => {
  return errors.value.slice(-5).reverse()
})

export function useErrors() {
  return {
    errors,
    recentErrors,
    addError,
    addErrorFromResponse,
    removeError,
    clearErrors
  }
}
