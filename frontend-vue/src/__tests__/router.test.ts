import { describe, it, expect } from 'vitest'

describe('路由配置', () => {
  it('应包含所有核心路由', async () => {
    const routerModule = await import('@/router')
    const router = routerModule.default
    const routes = router.getRoutes()

    const routeNames = routes.map(r => r.name).filter(Boolean)

    expect(routeNames).toContain('Feed')
    expect(routeNames).toContain('ProjectDetail')
    expect(routeNames).toContain('NewEditor')
    expect(routeNames).toContain('Editor')
    expect(routeNames).toContain('Rank')
    expect(routeNames).toContain('Problems')
    expect(routeNames).toContain('Competition')
    expect(routeNames).toContain('CompetitionDetail')
    expect(routeNames).toContain('Homework')
    expect(routeNames).toContain('HomeworkDetail')
    expect(routeNames).toContain('Analytics')
    expect(routeNames).toContain('Points')
    expect(routeNames).toContain('Admin')
    expect(routeNames).toContain('Notifications')
  })

  it('应有 404 兜底路由', async () => {
    const routerModule = await import('@/router')
    const router = routerModule.default
    const routes = router.getRoutes()

    // 检查有 catch-all 路由
    const catchAll = routes.find(r => r.path.includes(':pathMatch'))
    expect(catchAll).toBeDefined()
  })

  it('需要认证的路由应有 requiresAuth meta', async () => {
    const routerModule = await import('@/router')
    const router = routerModule.default
    const routes = router.getRoutes()

    const editorRoute = routes.find(r => r.name === 'Editor')
    expect(editorRoute?.meta?.requiresAuth).toBe(true)

    const pointsRoute = routes.find(r => r.name === 'Points')
    expect(pointsRoute?.meta?.requiresAuth).toBe(true)

    const adminRoute = routes.find(r => r.name === 'Admin')
    expect(adminRoute?.meta?.requiresAuth).toBe(true)
    expect(adminRoute?.meta?.requiresAdmin).toBe(true)
  })

  it('无需认证的路由应可公开访问', async () => {
    const routerModule = await import('@/router')
    const router = routerModule.default
    const routes = router.getRoutes()

    const feedRoute = routes.find(r => r.name === 'Feed')
    expect(feedRoute?.meta?.requiresAuth).toBeFalsy()

    const problemsRoute = routes.find(r => r.name === 'Problems')
    expect(problemsRoute?.meta?.requiresAuth).toBeFalsy()
  })
})
