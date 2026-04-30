import { ref, onBeforeUnmount, type Ref } from 'vue'

export interface SseOptions {
  /** SSE 连接 URL */
  url: string
  /** 收到 token/消息片段时回调 */
  onToken?: (token: string) => void
  /** 连接完成时回调 */
  onComplete?: (data: string) => void
  /** 出错时回调 */
  onError?: (error: string) => void
  /** 自动重连（默认 false） */
  autoReconnect?: boolean
  /** 重连间隔 ms（默认 3000） */
  reconnectInterval?: number
  /** 最大重连次数（默认 3） */
  maxReconnects?: number
}

export interface UseSseReturn {
  /** 是否正在连接 */
  isConnected: Ref<boolean>
  /** 是否正在重连 */
  isReconnecting: Ref<boolean>
  /** 累计接收的内容 */
  content: Ref<string>
  /** 开始连接 */
  connect: () => void
  /** 断开连接 */
  disconnect: () => void
  /** 重置内容 */
  reset: () => void
}

/**
 * SSE (Server-Sent Events) composable
 * 封装 EventSource 的创建、事件监听、自动重连和生命周期管理
 */
export function useSseStream(options: SseOptions): UseSseReturn {
  const isConnected = ref(false)
  const isReconnecting = ref(false)
  const content = ref('')

  let eventSource: EventSource | null = null
  let reconnectCount = 0
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const {
    url,
    onToken,
    onComplete,
    onError,
    autoReconnect = false,
    reconnectInterval = 3000,
    maxReconnects = 3
  } = options

  function connect() {
    disconnect()

    try {
      eventSource = new EventSource(url)
      isConnected.value = true

      eventSource.addEventListener('token', (e) => {
        const data = (e as MessageEvent).data
        content.value += data
        onToken?.(data)
      })

      eventSource.addEventListener('complete', (e) => {
        const data = (e as MessageEvent).data
        onComplete?.(data)
        disconnect()
      })

      eventSource.addEventListener('error', (e) => {
        const data = (e as MessageEvent).data || '服务端错误'
        onError?.(data)
        handleReconnect()
      })

      eventSource.onerror = () => {
        if (eventSource?.readyState === EventSource.CLOSED) {
          onError?.('连接中断')
          handleReconnect()
        }
      }
    } catch (err) {
      isConnected.value = false
      onError?.('创建 SSE 连接失败')
    }
  }

  function handleReconnect() {
    disconnect()

    if (!autoReconnect || reconnectCount >= maxReconnects) {
      isConnected.value = false
      isReconnecting.value = false
      return
    }

    reconnectCount++
    isReconnecting.value = true

    reconnectTimer = setTimeout(() => {
      isReconnecting.value = false
      connect()
    }, reconnectInterval)
  }

  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    isConnected.value = false
  }

  function reset() {
    content.value = ''
    reconnectCount = 0
  }

  // 组件卸载时自动清理
  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    isConnected,
    isReconnecting,
    content,
    connect,
    disconnect,
    reset
  }
}
