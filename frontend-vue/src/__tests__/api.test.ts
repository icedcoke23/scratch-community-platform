import { describe, it, expect } from 'vitest'

// API 模块的基础测试（不需要实际网络请求）
describe('API 模块结构', () => {
  it('应导出所有 API 对象', async () => {
    const api = await import('@/api')
    expect(api.userApi).toBeDefined()
    expect(api.notificationApi).toBeDefined()
    expect(api.projectApi).toBeDefined()
    expect(api.socialApi).toBeDefined()
    expect(api.problemApi).toBeDefined()
    expect(api.homeworkApi).toBeDefined()
    expect(api.pointApi).toBeDefined()
    expect(api.competitionApi).toBeDefined()
    expect(api.aiReviewApi).toBeDefined()
    expect(api.adminApi).toBeDefined()
    expect(api.analyticsApi).toBeDefined()
  })

  it('userApi 应包含登录方法', async () => {
    const { userApi } = await import('@/api')
    expect(typeof userApi.login).toBe('function')
    expect(typeof userApi.register).toBe('function')
    expect(typeof userApi.logout).toBe('function')
    expect(typeof userApi.refreshToken).toBe('function')
    expect(typeof userApi.getMyInfo).toBe('function')
  })

  it('notificationApi 应包含完整的通知方法', async () => {
    const { notificationApi } = await import('@/api')
    expect(typeof notificationApi.getMyNotifications).toBe('function')
    expect(typeof notificationApi.getUnreadCount).toBe('function')
    expect(typeof notificationApi.markRead).toBe('function')
    expect(typeof notificationApi.markAllRead).toBe('function')
  })

  it('aiReviewApi 应包含流式方法', async () => {
    const { aiReviewApi } = await import('@/api')
    expect(typeof aiReviewApi.stream).toBe('function')
    expect(typeof aiReviewApi.getSseToken).toBe('function')
    expect(typeof aiReviewApi.generate).toBe('function')
    expect(typeof aiReviewApi.getLatest).toBe('function')
    expect(typeof aiReviewApi.getHistory).toBe('function')
  })

  it('competitionApi 应包含完整的方法', async () => {
    const { competitionApi } = await import('@/api')
    expect(typeof competitionApi.list).toBe('function')
    expect(typeof competitionApi.getDetail).toBe('function')
    expect(typeof competitionApi.register).toBe('function')
    expect(typeof competitionApi.getRanking).toBe('function')
    expect(typeof competitionApi.create).toBe('function')
  })

  it('adminApi 应包含管理方法', async () => {
    const { adminApi } = await import('@/api')
    expect(typeof adminApi.getDashboard).toBe('function')
    expect(typeof adminApi.listUsers).toBe('function')
    expect(typeof adminApi.disableUser).toBe('function')
    expect(typeof adminApi.enableUser).toBe('function')
  })
})

describe('类型定义完整性', () => {
  it('应导出所有类型', async () => {
    // TypeScript 编译时检查，运行时只需确认模块可加载
    const types = await import('@/types')
    expect(types).toBeDefined()
  })
})
