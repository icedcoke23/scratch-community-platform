import { test, expect } from '@playwright/test'

test.describe('首页', () => {
  test('访问首页应重定向到 /feed', async ({ page }) => {
    // 访问根路径，验证自动跳转到 /feed
    await page.goto('/')
    await expect(page).toHaveURL(/\/feed/)
  })

  test('导航栏应存在', async ({ page }) => {
    await page.goto('/feed')

    // 验证顶部导航栏存在
    const header = page.locator('header, .app-header, nav')
    await expect(header.first()).toBeVisible()
  })

  test('页面标题应包含 Scratch 社区', async ({ page }) => {
    await page.goto('/feed')

    // 路由守卫会设置 document.title = "社区 - Scratch 社区"
    await expect(page).toHaveTitle(/Scratch 社区/)
  })

  test('Feed 页面内容应正常渲染', async ({ page }) => {
    await page.goto('/feed')

    // 页面应包含社区相关的内容区域
    const mainContent = page.locator('main, .feed-view, .app-main, [class*="feed"]')
    await expect(mainContent.first()).toBeVisible({ timeout: 10_000 })
  })
})
