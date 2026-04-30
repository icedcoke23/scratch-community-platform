/**
 * 开发环境日志工具
 * 
 * 生产环境自动禁用，开发环境输出带上下文的日志
 */

const isDev = import.meta.env.DEV

/** 创建带上下文的 logger */
export function createLogger(context: string) {
  return {
    log: (...args: unknown[]) => {
      if (isDev) console.log(`[${context}]`, ...args)
    },
    warn: (...args: unknown[]) => {
      if (isDev) console.warn(`[${context}]`, ...args)
    },
    error: (...args: unknown[]) => {
      // 错误始终记录（可接入错误上报）
      console.error(`[${context}]`, ...args)
    },
    info: (...args: unknown[]) => {
      if (isDev) console.info(`[${context}]`, ...args)
    },
    debug: (...args: unknown[]) => {
      if (isDev) console.debug(`[${context}]`, ...args)
    }
  }
}
