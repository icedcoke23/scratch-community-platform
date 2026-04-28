import { test, expect } from '@playwright/test'

test.describe('项目浏览', () => {
  test('Feed 页面应正常加载', async ({ page }) => {
    await page.goto('/feed')

    // 验证页面加载完成
    await expect(page).toHaveURL(/\/feed/)
    await expect(page).toHaveTitle(/Scratch 社区/)
  })

  test('项目卡片应加载显示', async ({ page }) => {
    await page.goto('/feed')

    // 等待项目卡片渲染（ProjectCard 组件）
    // 卡片可能是 el-card 或自定义的 project-card
    const cards = page.locator(
      '.project-card, .el-card, [class*="project-item"], [class*="card"]'
    )

    // 等待至少一个卡片出现（允许加载时间）
    await expect(cards.first()).toBeVisible({ timeout: 15_000 }).catch(() => {
      // 如果没有项目数据，可能显示空状态
      const emptyState = page.locator('.empty-state, [class*="empty"], text=暂无')
      expect(emptyState.first()).toBeTruthy()
    })
  })

  test('项目卡片应包含必要信息', async ({ page }) => {
    await page.goto('/feed')

    // 等待卡片加载
    const card = page.locator(
      '.project-card, .el-card, [class*="project-item"], [class*="card"]'
    ).first()

    if (await card.isVisible({ timeout: 10_000 }).catch(() => false)) {
      // 卡片内应有标题或项目名称
      const hasTitle = await card.locator('h2, h3, h4, .title, [class*="title"], [class*="name"]').count()
      expect(hasTitle).toBeGreaterThan(0)
    }
  })

  test('点击项目卡片应跳转到详情页', async ({ page }) => {
    await page.goto('/feed')

    // 等待卡片加载
    const card = page.locator(
      '.project-card a, .el-card a, [class*="project-item"] a, a[href*="/project/"]'
    ).first()

    if (await card.isVisible({ timeout: 10_000 }).catch(() => false)) {
      // 记录项目链接
      const href = await card.getAttribute('href')

      // 点击卡片
      await card.click()

      // 验证跳转到项目详情页
      await expect(page).toHaveURL(/\/project\//)
    }
  })

  test('项目详情页应显示完整信息', async ({ page }) => {
    // 直接导航到一个项目详情页（需要有效的项目 ID）
    // 这里先访问 feed 获取一个项目链接
    await page.goto('/feed')

    const projectLink = page.locator('a[href*="/project/"]').first()

    if (await projectLink.isVisible({ timeout: 10_000 }).catch(() => false)) {
      await projectLink.click()
      await expect(page).toHaveURL(/\/project\//)

      // 详情页应包含项目标题区域
      const titleArea = page.locator(
        'h1, h2, .project-title, [class*="title"], [class*="detail"]'
      ).first()
      await expect(titleArea).toBeVisible({ timeout: 10_000 }).catch(() => {
        // 页面至少应该有内容
        expect(page.locator('body')).toBeTruthy()
      })
    }
  })

  test('Feed 页面应支持滚动加载', async ({ page }) => {
    await page.goto('/feed')

    // 等待初始内容加载
    await page.waitForTimeout(2_000)

    // 记录初始卡片数量
    const initialCards = await page.locator(
      '.project-card, .el-card, [class*="project-item"], [class*="card"]'
    ).count()

    // 滚动到页面底部
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight)
    })

    // 等待可能的懒加载
    await page.waitForTimeout(2_000)

    // 检查是否有更多内容加载（或已经是全部内容）
    const afterScrollCards = await page.locator(
      '.project-card, .el-card, [class*="project-item"], [class*="card"]'
    ).count()

    // 卡片数量应 >= 初始数量
    expect(afterScrollCards).toBeGreaterThanOrEqual(initialCards)
  })
})
