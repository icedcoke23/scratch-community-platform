export interface ErrorInfo {
  id: number
  type: 'network' | 'auth' | 'rateLimit' | 'server' | 'timeout' | 'validation'
  message: string
  timestamp: number
  code?: number
  details?: string
}

export interface ApiError {
  code: number
  msg: string
  data?: unknown
}

export interface ErrorContext {
  url?: string
  method?: string
  params?: Record<string, unknown>
  data?: unknown
}

export const ErrorCode = {
  SUCCESS: 0,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,

  VALIDATION_ERROR: 1001,
  INVALID_CREDENTIALS: 1002,
  TOKEN_EXPIRED: 1003,
  TOKEN_INVALID: 1004,
  USER_NOT_FOUND: 1005,
  USER_ALREADY_EXISTS: 1006,
  PROJECT_NOT_FOUND: 2001,
  PROJECT_FORBIDDEN: 2002,
  FILE_TOO_LARGE: 3001,
  INVALID_FILE_TYPE: 3002,
  UPLOAD_FAILED: 3003,
  SB3_NOT_FOUND: 3004,
} as const

export type ErrorCodeType = typeof ErrorCode[keyof typeof ErrorCode]

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    'msg' in error &&
    typeof (error as ApiError).code === 'number' &&
    typeof (error as ApiError).msg === 'string'
  )
}

export function getErrorTypeFromCode(code: number): ErrorInfo['type'] {
  if (code === ErrorCode.TOKEN_EXPIRED || code === ErrorCode.TOKEN_INVALID) {
    return 'auth'
  }
  if (code >= 400 && code < 500) {
    return 'validation'
  }
  if (code >= 500) {
    return 'server'
  }
  return 'server'
}

export function formatErrorMessage(msg: string, code?: number): string {
  if (msg) return msg

  const defaultMessages: Partial<Record<number, string>> = {
    [ErrorCode.VALIDATION_ERROR]: '数据验证失败',
    [ErrorCode.INVALID_CREDENTIALS]: '用户名或密码错误',
    [ErrorCode.TOKEN_EXPIRED]: '登录已过期，请重新登录',
    [ErrorCode.TOKEN_INVALID]: '登录信息无效，请重新登录',
    [ErrorCode.USER_NOT_FOUND]: '用户不存在',
    [ErrorCode.USER_ALREADY_EXISTS]: '用户名已存在',
    [ErrorCode.PROJECT_NOT_FOUND]: '项目不存在',
    [ErrorCode.PROJECT_FORBIDDEN]: '无权访问该项目',
    [ErrorCode.FILE_TOO_LARGE]: '文件过大',
    [ErrorCode.INVALID_FILE_TYPE]: '不支持的文件类型',
    [ErrorCode.UPLOAD_FAILED]: '上传失败',
    [ErrorCode.SB3_NOT_FOUND]: '项目文件不存在',
  }

  return defaultMessages[code ?? 0] || '操作失败，请稍后重试'
}
