import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProjectStore } from '@/stores/project'
import { useNotificationStore } from '@/stores/notification'

// Mock API
vi.mock('@/api', () => ({
  projectApi: {
    getDetail: vi.fn()
  },
  notificationApi: {
    getUnreadCount: vi.fn(),
    getMyNotifications: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn()
  }
}))

import { projectApi, notificationApi } from '@/api'

describe('useProjectStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态应为空', () => {
    const store = useProjectStore()
    expect(store.currentProject).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.hasCurrentProject).toBe(false)
  })

  it('loadProject 应加载项目详情', async () => {
    const mockProject = {
      id: 1, title: '测试项目', status: 'published',
      userId: 1, username: 'test', likeCount: 0,
      commentCount: 0, viewCount: 0, createdAt: '2026-04-24'
    }
    vi.mocked(projectApi.getDetail).mockResolvedValue({
      code: 0, msg: 'ok', data: mockProject as any, timestamp: Date.now()
    })

    const store = useProjectStore()
    await store.loadProject(1)

    expect(store.currentProject).toEqual(mockProject)
    expect(store.hasCurrentProject).toBe(true)
    expect(store.loading).toBe(false)
  })

  it('loadProject 失败时不应崩溃', async () => {
    vi.mocked(projectApi.getDetail).mockRejectedValue(new Error('网络错误'))

    const store = useProjectStore()
    await store.loadProject(999)

    expect(store.currentProject).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('clearCurrent 应清除当前项目', async () => {
    const store = useProjectStore()
    store.currentProject = { id: 1 } as any
    store.clearCurrent()
    expect(store.currentProject).toBeNull()
    expect(store.hasCurrentProject).toBe(false)
  })

  it('缓存管理应正常工作', () => {
    const store = useProjectStore()
    const projects = [{ id: 1 }, { id: 2 }] as any[]

    store.setCache(1, projects)
    expect(store.getCache(1)).toEqual(projects)
    expect(store.getCache(2)).toBeUndefined()

    store.clearCache()
    expect(store.getCache(1)).toBeUndefined()
  })
})

describe('useNotificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('初始状态应为空', () => {
    const store = useNotificationStore()
    expect(store.unreadCount).toBe(0)
    expect(store.notifications).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('fetchUnreadCount 应更新未读数', async () => {
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue({
      code: 0, msg: 'ok', data: 5, timestamp: Date.now()
    })

    const store = useNotificationStore()
    await store.fetchUnreadCount()

    expect(store.unreadCount).toBe(5)
  })

  it('fetchUnreadCount 应限制最大值为 99', async () => {
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue({
      code: 0, msg: 'ok', data: 150, timestamp: Date.now()
    })

    const store = useNotificationStore()
    await store.fetchUnreadCount()

    expect(store.unreadCount).toBe(99)
  })

  it('fetchNotifications 应加载通知列表', async () => {
    const mockNotifications = [
      { id: 1, userId: 1, type: 'LIKE', title: '有人点赞', read: false, createdAt: '2026-04-24' },
      { id: 2, userId: 1, type: 'COMMENT', title: '有人评论', read: true, createdAt: '2026-04-24' }
    ]
    vi.mocked(notificationApi.getMyNotifications).mockResolvedValue({
      code: 0, msg: 'ok',
      data: { records: mockNotifications, total: 2, size: 20, current: 1, pages: 1 },
      timestamp: Date.now()
    })

    const store = useNotificationStore()
    await store.fetchNotifications()

    expect(store.notifications).toEqual(mockNotifications)
    expect(store.total).toBe(2)
    expect(store.loading).toBe(false)
  })

  it('fetchNotifications 翻页应追加数据', async () => {
    const page1 = [{ id: 1 }] as any[]
    const page2 = [{ id: 2 }] as any[]

    vi.mocked(notificationApi.getMyNotifications)
      .mockResolvedValueOnce({
        code: 0, msg: 'ok',
        data: { records: page1, total: 2, size: 1, current: 1, pages: 2 },
        timestamp: Date.now()
      })
      .mockResolvedValueOnce({
        code: 0, msg: 'ok',
        data: { records: page2, total: 2, size: 1, current: 2, pages: 2 },
        timestamp: Date.now()
      })

    const store = useNotificationStore()
    await store.fetchNotifications(1)
    await store.fetchNotifications(2)

    expect(store.notifications).toHaveLength(2)
    expect(store.page).toBe(2)
  })

  it('markRead 应标记单条已读', async () => {
    vi.mocked(notificationApi.markRead).mockResolvedValue({
      code: 0, msg: 'ok', data: undefined as any, timestamp: Date.now()
    })

    const store = useNotificationStore()
    store.notifications = [
      { id: 1, read: false } as any,
      { id: 2, read: false } as any
    ]
    store.unreadCount = 2

    await store.markRead(1)

    expect(store.notifications[0].read).toBe(true)
    expect(store.unreadCount).toBe(1)
  })

  it('markAllRead 应全部标记已读', async () => {
    vi.mocked(notificationApi.markAllRead).mockResolvedValue({
      code: 0, msg: 'ok', data: undefined as any, timestamp: Date.now()
    })

    const store = useNotificationStore()
    store.notifications = [
      { id: 1, read: false } as any,
      { id: 2, read: false } as any
    ]
    store.unreadCount = 2

    await store.markAllRead()

    expect(store.notifications.every(n => n.read)).toBe(true)
    expect(store.unreadCount).toBe(0)
  })

  it('startPolling/stopPolling 应管理轮询', () => {
    vi.mocked(notificationApi.getUnreadCount).mockResolvedValue({
      code: 0, msg: 'ok', data: 0, timestamp: Date.now()
    })

    const store = useNotificationStore()
    store.startPolling(1000)

    // 前进 3 秒，应该调用了初始加载 + 3 次轮询
    vi.advanceTimersByTime(3000)
    expect(notificationApi.getUnreadCount).toHaveBeenCalledTimes(4) // 1 initial + 3 polls

    store.stopPolling()
    const callCount = vi.mocked(notificationApi.getUnreadCount).mock.calls.length

    // 再前进 2 秒，不应该再调用
    vi.advanceTimersByTime(2000)
    expect(notificationApi.getUnreadCount).toHaveBeenCalledTimes(callCount)
  })

  it('reset 应清除所有状态并停止轮询', () => {
    const store = useNotificationStore()
    store.unreadCount = 5
    store.notifications = [{ id: 1 }] as any[]
    store.startPolling(1000)

    store.reset()

    expect(store.unreadCount).toBe(0)
    expect(store.notifications).toEqual([])
  })
})
