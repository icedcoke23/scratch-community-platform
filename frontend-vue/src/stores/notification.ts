import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Notification } from '@/types'
import { notificationApi } from '@/api'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref<Notification[]>([])
  const loading = ref(false)
  const page = ref(1)
  const total = ref(0)
  let pollTimer: ReturnType<typeof setInterval> | null = null

  /** 获取未读通知数 */
  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      if (res.code === 0 && res.data != null) {
        unreadCount.value = Math.min(res.data, 99)
      }
    } catch { /* 忽略 */ }
  }

  /** 获取通知列表 */
  async function fetchNotifications(pageNum = 1, size = 20) {
    loading.value = true
    try {
      const res = await notificationApi.getMyNotifications(pageNum, size)
      if (res.code === 0 && res.data) {
        if (pageNum === 1) {
          notifications.value = (res.data.records as unknown as Notification[]) || []
        } else {
          notifications.value.push(...((res.data.records as unknown as Notification[]) || []))
        }
        total.value = res.data.total || 0
        page.value = pageNum
      }
    } catch { /* 忽略 */ }
    finally { loading.value = false }
  }

  /** 标记单条已读 */
  async function markRead(id: number) {
    try {
      const res = await notificationApi.markRead(id)
      if (res.code === 0) {
        const item = notifications.value.find(n => n.id === id)
        if (item) item.read = true
        if (unreadCount.value > 0) unreadCount.value--
      }
    } catch { /* 忽略 */ }
  }

  /** 全部标记已读 */
  async function markAllRead() {
    try {
      const res = await notificationApi.markAllRead()
      if (res.code === 0) {
        notifications.value.forEach(n => { n.read = true })
        unreadCount.value = 0
      }
    } catch { /* 忽略 */ }
  }

  /** 开始轮询未读数 */
  function startPolling(intervalMs = 60000) {
    stopPolling()
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, intervalMs)
  }

  /** 停止轮询 */
  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  /** 重置状态 */
  function reset() {
    unreadCount.value = 0
    notifications.value = []
    stopPolling()
  }

  return {
    unreadCount,
    notifications,
    loading,
    page,
    total,
    fetchUnreadCount,
    fetchNotifications,
    markRead,
    markAllRead,
    startPolling,
    stopPolling,
    reset
  }
})
