/**
 * k6 性能压测脚本 - Scratch 社区平台
 *
 * 使用方法:
 *   k6 run scripts/performance-test.js
 *   k6 run --env BASE_URL=http://your-server scripts/performance-test.js
 *
 * 覆盖场景:
 *   1. 健康检查压测 (GET /api/v1/health)
 *   2. Feed 浏览压测 (GET /api/v1/social/feed)
 *   3. 排行榜压测   (GET /api/v1/social/rank)
 *   4. 用户登录压测 (POST /api/v1/user/login)
 */

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Rate, Trend } from 'k6/metrics'

// ==================== 自定义指标 ====================

// 错误率指标
const errorRate = new Rate('errors')

// 各接口响应时间指标
const healthDuration = new Trend('health_duration', true)
const feedDuration = new Trend('feed_duration', true)
const rankDuration = new Trend('rank_duration', true)
const loginDuration = new Trend('login_duration', true)

// ==================== 测试配置 ====================

// 基础 URL，可通过环境变量覆盖
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  // 阶梯式加压策略
  stages: [
    // 阶段1: 预热 - 10秒内逐步增加到 10 个虚拟用户
    { duration: '10s', target: 10 },

    // 阶段2: 稳定负载 - 保持 10 个用户运行 30 秒
    { duration: '30s', target: 10 },

    // 阶段3: 压力递增 - 30秒内增加到 50 个用户
    { duration: '30s', target: 50 },

    // 阶段4: 高负载 - 保持 50 个用户运行 30 秒
    { duration: '30s', target: 50 },

    // 阶段5: 峰值压力 - 20秒内增加到 100 个用户
    { duration: '20s', target: 100 },

    // 阶段6: 峰值保持 - 保持 100 个用户运行 30 秒
    { duration: '30s', target: 100 },

    // 阶段7: 逐步降压 - 30秒内降到 0
    { duration: '30s', target: 0 },
  ],

  // 性能阈值（不满足则测试失败）
  thresholds: {
    // 全局错误率 < 5%
    errors: ['rate<0.05'],

    // 健康检查 P95 < 200ms（简单接口要求更严格）
    health_duration: ['p(95)<200'],

    // Feed 接口 P95 < 500ms
    feed_duration: ['p(95)<500'],

    // 排行榜接口 P95 < 500ms
    rank_duration: ['p(95)<500'],

    // 登录接口 P95 < 500ms（涉及数据库查询）
    login_duration: ['p(95)<500'],

    // 全局 HTTP 请求 P95 < 500ms
    http_req_duration: ['p(95)<500'],

    // HTTP 请求失败率 < 1%
    http_req_failed: ['rate<0.01'],
  },
}

// ==================== 测试主函数 ====================

export default function () {
  // 每次迭代之间随机等待 1-3 秒，模拟真实用户行为
  const thinkTime = Math.random() * 2 + 1

  // ----- 场景1: 健康检查压测 -----
  group('健康检查', function () {
    const healthRes = http.get(`${BASE_URL}/api/v1/health`, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'health' },
    })

    // 记录响应时间
    healthDuration.add(healthRes.timings.duration)

    // 验证响应
    const healthOk = check(healthRes, {
      '健康检查状态码为 200': (r) => r.status === 200,
      '健康检查返回 status=UP': (r) => {
        try {
          const body = JSON.parse(r.body)
          return body.data && body.data.status === 'UP'
        } catch {
          return false
        }
      },
      '健康检查响应时间 < 200ms': (r) => r.timings.duration < 200,
    })

    errorRate.add(!healthOk)
  })

  sleep(thinkTime)

  // ----- 场景2: Feed 浏览压测 -----
  group('Feed 浏览', function () {
    const feedRes = http.get(`${BASE_URL}/api/v1/social/feed`, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'feed' },
    })

    feedDuration.add(feedRes.timings.duration)

    const feedOk = check(feedRes, {
      'Feed 状态码为 200': (r) => r.status === 200,
      'Feed 返回数据结构正确': (r) => {
        try {
          const body = JSON.parse(r.body)
          return body.code !== undefined
        } catch {
          return false
        }
      },
      'Feed 响应时间 < 500ms': (r) => r.timings.duration < 500,
    })

    errorRate.add(!feedOk)
  })

  sleep(thinkTime)

  // ----- 场景3: 排行榜压测 -----
  group('排行榜', function () {
    const rankRes = http.get(`${BASE_URL}/api/v1/social/rank`, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'rank' },
    })

    rankDuration.add(rankRes.timings.duration)

    const rankOk = check(rankRes, {
      '排行榜状态码为 200': (r) => r.status === 200,
      '排行榜响应时间 < 500ms': (r) => r.timings.duration < 500,
    })

    errorRate.add(!rankOk)
  })

  sleep(thinkTime)

  // ----- 场景4: 用户登录压测 -----
  group('用户登录', function () {
    const loginPayload = JSON.stringify({
      username: `testuser_${__VU}`,  // 使用虚拟用户编号避免重复
      password: 'test123456',
    })

    const loginRes = http.post(`${BASE_URL}/api/v1/user/login`, loginPayload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'login' },
    })

    loginDuration.add(loginRes.timings.duration)

    const loginOk = check(loginRes, {
      '登录接口返回有效状态码': (r) => r.status === 200 || r.status === 401 || r.status === 400,
      '登录接口响应时间 < 500ms': (r) => r.timings.duration < 500,
    })

    // 登录失败不一定是服务端错误（可能是账号不存在），所以只标记 5xx
    errorRate.add(loginRes.status >= 500)
  })

  sleep(thinkTime)
}

// ==================== 测试生命周期钩子 ====================

// 测试开始前执行
export function setup() {
  console.log('🚀 开始性能压测...')
  console.log(`📍 目标服务器: ${BASE_URL}`)

  // 预检：验证服务是否可达
  const res = http.get(`${BASE_URL}/api/v1/health`)
  if (res.status !== 200) {
    console.error(`❌ 服务不可达 (status=${res.status})，请确认服务已启动`)
  } else {
    console.log('✅ 服务健康检查通过，开始压测')
  }
}

// 测试结束后执行
export function teardown() {
  console.log('✅ 性能压测完成')
}
