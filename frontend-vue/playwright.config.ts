import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E 测试配置
 * 文档: https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  // 测试文件目录
  testDir: './e2e',

  // 测试结果输出目录
  outputDir: './e2e/test-results',

  // 每个测试的超时时间 (30 秒)
  timeout: 30_000,

  // 全局设置：所有测试运行前的超时
  globalTimeout: 600_000,

  // expect 断言超时
  expect: {
    timeout: 5_000,
  },

  // 完全并行运行测试
  fullyParallel: true,

  // CI 环境下禁止 test.only
  forbidOnly: !!process.env.CI,

  // 失败重试次数（CI 环境重试 2 次，本地不重试）
  retries: process.env.CI ? 2 : 0,

  // 并行 worker 数量
  workers: process.env.CI ? 1 : undefined,

  // 测试报告配置
  reporter: [
    ['html', { outputFolder: './e2e/playwright-report', open: 'never' }],
    ['list'],
  ],

  // 全局共享配置
  use: {
    // 基础 URL，所有相对 URL 都会基于此
    baseURL: 'http://localhost:3000',

    // 测试失败时自动截图
    screenshot: 'only-on-failure',

    // 录制失败测试的视频
    video: 'retain-on-failure',

    // 收集 trace（调试用）
    trace: 'on-first-retry',

    // 默认导航超时
    navigationTimeout: 15_000,

    // 默认操作超时
    actionTimeout: 10_000,

    // 视口尺寸
    viewport: { width: 1280, height: 720 },

    // 忽略 HTTPS 错误
    ignoreHTTPSErrors: true,
  },

  // 浏览器项目配置
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    // 可选：移动端测试
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],

  // 开发服务器配置（运行测试前自动启动）
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
