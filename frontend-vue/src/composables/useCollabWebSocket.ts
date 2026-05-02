import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from '@/stores/user'
import { createLogger } from '@/utils/logger'
import type { CollabEvent, CollabParticipant, EditOperation } from '@/api/collab'

const log = createLogger('Collab')

/**
 * 协作编辑冲突数据
 */
interface ConflictData {
  type: 'update' | 'delete' | 'version_mismatch'
  field?: string
  localValue?: unknown
  remoteValue?: unknown
  expectedVersion?: number
  actualVersion?: number
  message?: string
}

/**
 * 协作编辑 WebSocket 组合式函数
 *
 * 使用 STOMP over SockJS 连接后端 WebSocket 端点
 * 提供会话管理、编辑同步、光标同步、聊天等功能
 *
 * 特性：
 * - 自动重连（指数退避，最大 30 秒）
 * - 心跳检测（10 秒间隔）
 * - 连接状态追踪
 */
export function useCollabWebSocket(sessionId: () => number | null) {
  const userStore = useUserStore()

  // 状态
  const connected = ref(false)
  const participants = ref<CollabParticipant[]>([])
  const currentVersion = ref(0)
  const chatMessages = ref<Array<{ userId: number; nickname: string; message: string; timestamp: number }>>([])
  const conflictData = ref<ConflictData | null>(null)
  const reconnectAttempt = ref(0)

  // 重连配置
  const RECONNECT_BASE_DELAY = 1000   // 初始重连延迟 1 秒
  const RECONNECT_MAX_DELAY = 30000   // 最大重连延迟 30 秒
  const RECONNECT_MAX_ATTEMPTS = 10   // 最大重连次数

  // STOMP 客户端
  let client: Client | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let manualDisconnect = false

  /** 计算指数退避延迟 */
  function getReconnectDelay(): number {
    const delay = Math.min(
      RECONNECT_BASE_DELAY * Math.pow(2, reconnectAttempt.value),
      RECONNECT_MAX_DELAY
    )
    // 添加 ±20% 随机抖动，避免重连风暴
    const jitter = delay * 0.2 * (Math.random() - 0.5)
    return Math.round(delay + jitter)
  }

  /** 连接 WebSocket */
  function connect() {
    const sid = sessionId()
    if (!sid) return

    // 清理旧连接
    if (client) {
      client.deactivate()
      client = null
    }

    manualDisconnect = false

    client = new Client({
      webSocketFactory: () => new SockJS(`/ws-collab?token=${userStore.token}`),
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // 禁用 STOMP 内置重连（我们自己管理重连逻辑）
      reconnectDelay: 0,

      onConnect: () => {
        connected.value = true
        reconnectAttempt.value = 0 // 重置重连计数
        log.log(`[Collab] 已连接到会话 ${sid}`)

        // 订阅会话广播频道
        client!.subscribe(`/topic/collab/${sid}`, (message) => {
          const event: CollabEvent = JSON.parse(message.body)
          handleEvent(event)
        })

        // 订阅个人冲突通知频道
        client!.subscribe('/user/queue/collab/conflict', (message) => {
          conflictData.value = JSON.parse(message.body)
        })

        // 发送加入消息
        sendMessage('join', { role: 'editor' })
      },

      onDisconnect: () => {
        connected.value = false
        log.log('[Collab] 已断开连接')
        // 非主动断开时尝试重连
        if (!manualDisconnect) {
          scheduleReconnect()
        }
      },

      onStompError: (frame) => {
        log.error('[Collab] STOMP 错误:', frame.headers['message'])
        connected.value = false
        if (!manualDisconnect) {
          scheduleReconnect()
        }
      },
    })

    client.activate()
  }

  /** 调度重连（指数退避） */
  function scheduleReconnect() {
    if (manualDisconnect) return
    if (reconnectAttempt.value >= RECONNECT_MAX_ATTEMPTS) {
      log.warn(`[Collab] 已达最大重连次数 (${RECONNECT_MAX_ATTEMPTS})，停止重连`)
      return
    }

    const delay = getReconnectDelay()
    reconnectAttempt.value++
    log.log(`[Collab] 将在 ${delay}ms 后重连 (第 ${reconnectAttempt.value} 次)`)

    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      if (!manualDisconnect && sessionId()) {
        connect()
      }
    }, delay)
  }

  /** 断开连接 */
  function disconnect() {
    manualDisconnect = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (client) {
      const sid = sessionId()
      if (sid) {
        sendMessage('leave', null)
      }
      client.deactivate()
      client = null
      connected.value = false
      participants.value = []
      chatMessages.value = []
      reconnectAttempt.value = 0
    }
  }

  /** 处理服务端事件 */
  function handleEvent(event: CollabEvent) {
    switch (event.type) {
      case 'user_joined':
        // 添加参与者
        if (!participants.value.find(p => p.userId === event.userId)) {
          participants.value.push({
            userId: event.userId,
            nickname: event.nickname,
            role: event.payload?.role || 'viewer',
            cursorX: 0,
            cursorY: 0,
          })
        }
        break

      case 'user_left':
        // 移除参与者
        participants.value = participants.value.filter(p => p.userId !== event.userId)
        break

      case 'edit_applied':
        // 编辑操作已应用，更新版本号
        if (event.payload?.version) {
          currentVersion.value = event.payload.version
        }
        // 触发编辑回调（由组件处理具体逻辑）
        onEditApplied?.(event.payload as unknown as EditOperation)
        break

      case 'cursor_update':
        // 更新光标位置
        const p = participants.value.find(p => p.userId === event.userId)
        if (p && event.payload) {
          p.cursorX = event.payload.x
          p.cursorY = event.payload.y
        }
        break

      case 'chat':
        // 接收聊天消息
        if (event.payload) {
          chatMessages.value.push({
            userId: event.userId,
            nickname: event.nickname,
            message: event.payload.message,
            timestamp: event.timestamp,
          })
          // 限制历史消息数
          if (chatMessages.value.length > 200) {
            chatMessages.value = chatMessages.value.slice(-100)
          }
        }
        break

      case 'session_closed':
        connected.value = false
        break
    }
  }

  // 编辑回调（外部可覆盖）
  let onEditApplied: ((op: EditOperation) => void) | null = null

  /** 设置编辑回调 */
  function setEditCallback(cb: (op: EditOperation) => void) {
    onEditApplied = cb
  }

  /** 发送消息到服务端 */
  function sendMessage(type: string, payload: Record<string, unknown> | null) {
    const sid = sessionId()
    if (!client || !connected.value || !sid) return

    client.publish({
      destination: `/app/collab/${sid}/${type}`,
      body: JSON.stringify({ type, sessionId: sid, payload }),
    })
  }

  /** 提交编辑操作 */
  function sendEdit(op: Omit<EditOperation, 'version' | 'timestamp'>) {
    sendMessage('edit', { ...op, version: currentVersion.value })
  }

  /** 更新光标位置 */
  function sendCursor(x: number, y: number) {
    sendMessage('cursor', { x, y })
  }

  /** 发送聊天消息 */
  function sendChat(message: string) {
    sendMessage('chat', { message })
  }

  // 当 sessionId 变化时自动连接
  watch(() => sessionId(), (newId, oldId) => {
    if (newId && newId !== oldId) {
      if (client) disconnect()
      connect()
    }
  })

  return {
    connected,
    participants,
    currentVersion,
    chatMessages,
    conflictData,
    reconnectAttempt,
    connect,
    disconnect,
    sendEdit,
    sendCursor,
    sendChat,
    setEditCallback,
  }
}
