/**
 * 通用工具函数
 */

// ==================== 服务端时间同步 ====================

/**
 * 服务端时间偏移量（ms）
 * <p>通过 API 响应中的 timestamp 字段计算，用于校准客户端时间
 * <p>serverTime = Date.now() + serverTimeOffset
 */
let serverTimeOffset = 0

/**
 * 更新服务端时间偏移量
 * <p>在 API 响应拦截器中调用，每次响应都更新
 */
export function updateServerTimeOffset(serverTimestamp: number) {
  serverTimeOffset = serverTimestamp - Date.now()
}

/**
 * 获取校准后的服务端时间
 */
export function getServerTime(): Date {
  return new Date(Date.now() + serverTimeOffset)
}

/**
 * 获取服务端当前时间戳（ms）
 */
export function getServerTimestamp(): number {
  return Date.now() + serverTimeOffset
}

// ==================== 时间格式化 ====================

export function timeAgo(date: string | Date | null | undefined): string {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  const minutes = Math.floor((getServerTimestamp() - d.getTime()) / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return d.toLocaleDateString()
}

export function formatDate(date: string | Date | null | undefined): string {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleString()
}

// ==================== 枚举映射 ====================

export function difficultyType(d: string): string {
  return ({ easy: 'success', medium: 'warning', hard: 'danger' } as Record<string, string>)[d] || 'info'
}

export function typeLabel(t: string): string {
  return ({ choice: '选择题', true_false: '判断题', scratch_algo: '编程题' } as Record<string, string>)[t] || t
}
