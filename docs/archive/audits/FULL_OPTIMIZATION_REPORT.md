# 全面优化分析报告

**日期**: 2026-04-28
**项目**: Scratch Community Platform
**分析范围**: 后端架构 / 前端架构 / 数据库设计 / 安全 / 测试 / 文档

---

## 1. 后端架构分析

### 1.1 CrossModuleQueryRepository / WriteRepository — God Object 风险

**现状**: `CrossModuleQueryRepository` 包含约 25 个查询方法，涵盖项目、用户、作业、积分、评论、Feed 等多个模块的跨表查询。`CrossModuleWriteRepository` 包含约 12 个写操作方法。

**评估**: ⚠️ **中等风险**
- 目前规模尚可接受（约 400 行），但持续增长将成为 God Object
- 所有方法都使用 `JdbcTemplate` 原生 SQL，缺少类型安全
- 优点是集中管理跨模块 SQL，便于审计和优化

**建议**: 按模块拆分为 `ProjectQueryRepository`、`UserQueryRepository`、`HomeworkQueryRepository` 等，保持读写分离模式。

### 1.2 事件驱动架构可靠性

**现状**:
- 使用 Spring `@TransactionalEventListener(AFTER_COMMIT)` 处理点赞/评论/浏览事件
- `EventPublisherHelper` 提供了降级机制（事件发布失败时直接操作）
- `PointEventListener` 使用 switch-case 分发积分事件

**评估**: ✅ **基本可靠**
- 使用 `AFTER_COMMIT` 避免读取未提交数据
- 降级机制设计合理
- **缺失**: 没有死信队列机制，事件处理失败仅记录日志
- **缺失**: 积分事件缺少幂等性保障（重复消费会导致积分重复发放）

**建议**: 
- 为积分事件添加幂等性检查（基于 `refType + refId` 去重）
- 考虑引入事件持久化表作为死信队列

### 1.3 异步判题线程池和重试逻辑

**现状**:
- `AsyncConfig` 配置了独立的 `judgeExecutor` 线程池（core=4, max=16, queue=100）
- `JudgeService.judgeAsync()` 使用 `@Async("judgeExecutor")` 异步执行
- 重试逻辑使用 `Thread.sleep(1000L * attempt)` 阻塞等待

**评估**: ⚠️ **需要改进**
- `Thread.sleep` 阻塞线程池线程，浪费资源
- 重试间隔固定递增（1s, 2s, 3s），建议使用指数退避
- 线程池拒绝策略直接抛异常，可能影响用户体验
- `@Async` + `@Transactional` 组合在 Spring 中需要注意代理问题

**建议**: 使用 `CompletableFuture.delayedExecutor()` 或 `ScheduledExecutorService` 替代 `Thread.sleep`

### 1.4 Token Refresh 后端接口

**现状**: ✅ **已完整实现**
- `UserController.refresh()` 端点存在（`POST /api/v1/user/refresh`）
- 验证 Refresh Token 有效性 + 检查是否与存储一致（防重放）
- Refresh Token 快过期时自动刷新（< 1 天）
- `JwtUtils` 提供完整的 Refresh Token 生成和验证

### 1.5 Rate Limiter 实现质量

**现状**:
- **Redis 版本**: `RedisRateLimiter` 使用 Lua 脚本实现真正的滑动窗口（Sorted Set）
- **内存版本**: `RateLimitConfig` 使用滑动窗口计数器（10 个子窗口）
- `RedisRateLimitInterceptor` 在有 Redis 时自动启用
- `RateLimitConfig` 在无 Redis 时作为 fallback（`@ConditionalOnMissingBean`）

**评估**: ✅ **实现质量高**
- Redis 版本使用 Lua 脚本保证原子性，真正的滑动窗口
- 内存版本使用子窗口计数器，有效解决固定窗口 2x 突发问题
- 降级策略完善
- 响应头标准（X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After）

### 1.6 SseTokenService 内存降级清理机制

**现状**:
- `cleaner` 是 `ScheduledExecutorService`，每分钟清理过期 Token
- **关键问题**: `cleaner.scheduleAtFixedRate()` 只在 `!redisAvailable` 时启动
- 如果 Redis 运行时可用但后来断开连接，降级到内存但清理器未启动

**评估**: ⚠️ **边界情况有风险**
- 正常场景下工作正常
- Redis 断连降级场景下，内存 Token 可能无法清理（如果构造时 Redis 可用）

**建议**: 在 `generateToken()` 降级到内存时确保清理器已启动

---

## 2. 前端架构分析

### 2.1 Token 刷新竞态条件

**现状**:
- `request.ts` 使用 `refreshPromise` 去重机制
- `pendingRequests` 队列等待 Token 刷新后重试
- **关键问题**: `refreshPromise.finally(() => { refreshPromise = null })` 在 Promise 完成时立即清除引用

**评估**: ⚠️ **存在竞态条件**
- `finally` 在 `then/catch` 之后立即执行
- 如果多个请求同时 401，第一个触发刷新，其余加入 `pendingRequests`
- 刷新成功后，`retryPendingRequests(newToken)` 重试队列中的请求
- 但在 `finally` 中清除 `refreshPromise` 后，如果 `retryPendingRequests` 中的请求再次 401（极快场景），可能触发新的刷新

**建议**: 在 `retryPendingRequests` 完成后再清除 `refreshPromise`

### 2.2 API 层类型安全

**现状**:
- `request.ts` 的 `get<T>`, `post<T>`, `put<T>`, `del<T>` 使用泛型
- 但参数类型是 `Record<string, unknown>`，不够精确
- 各 API 模块（user.ts, project.ts 等）有具体的类型定义

**评估**: ⚠️ **类型安全可改进**
- `post<T>(url, data?: Record<string, unknown>)` 应该接受更具体的类型
- 部分 API 返回值类型不够精确（如 `get<unknown[]>`）

### 2.3 路由加载进度条

**现状**:
- 使用原生 DOM 操作 `document.getElementById('route-loading-bar')`
- 在 `router.beforeEach` 和 `router.afterEach` 中控制 className
- 需要 HTML 中存在 `#route-loading-bar` 元素

**评估**: ⚠️ **实现方式不够 Vue 化**
- 应该使用 Vue 组件 + CSS transition
- 当前依赖 HTML 中的特定元素，耦合度高

### 2.4 组件拆分和代码复用

**现状**: 
- 组件目录结构清晰（components/, views/, composables/）
- 使用 composables 抽取可复用逻辑（useTheme, useI18n, useNavigation 等）
- `App.vue` 约 300 行，包含全局样式

**评估**: ✅ **结构合理**
- 全局样式可以抽取到独立 CSS 文件

### 2.5 PWA Service Worker

**现状**: `manifest.json` 存在，但未发现 Service Worker 注册代码

**评估**: ⚠️ **PWA 不完整**
- 缺少 `sw.js` 或 Workbox 配置
- 离线支持不可用

---

## 3. 数据库设计分析

### 3.1 缺失的索引

**现状**: V18 迁移已添加了多个索引，但仍有缺失：

| 表 | 缺失索引 | 用途 |
|---|---|---|
| `point_log` | `(user_id, type, created_at)` | 今日积分查询、积分历史 |
| `point_log` | `(user_id, created_at)` | 用户积分时间线 |
| `notification` | `(user_id, created_at DESC)` | 通知列表按时间排序 |
| `project` | `(user_id, status, created_at DESC)` | 用户项目列表 |

### 3.2 JSON 字段使用

**现状**:
- `project.parse_result` — JSON 存储 sb3 解析结果
- `problem.options` — JSON 存储选择题选项
- `problem.expected_output` — JSON 存储预期输出
- `homework.problem_ids` — JSON 存储关联题目 ID
- `notification.data` — JSON 存储附加数据
- `homework_submission.answers` — JSON 存储答案

**评估**: ✅ **使用合理**
- MySQL 8.0 支持 JSON 类型和索引
- `problem.options` 和 `expected_output` 查询频率低，JSON 合适
- `homework.problem_ids` 使用 JSON 存储关联 ID 不如关系表，但数据量小可接受

### 3.3 计数字段一致性

**现状**:
- `project.like_count`, `comment_count`, `view_count` 通过事件驱动更新
- `CountCalibrationScheduler` 定时校准（每日凌晨 2 点）
- `CrossModuleWriteRepository` 使用原子 `+1`/`-1` 操作

**评估**: ✅ **设计合理**
- 原子操作 + 定时校准是标准方案
- 事件使用 `AFTER_COMMIT` 避免脏读

### 3.4 notification 表增长策略

**现状**: 无归档或清理机制

**评估**: ⚠️ **需要改进**
- 通知表会无限增长
- 建议添加定期清理策略（如保留 90 天已读通知）

---

## 4. 安全分析

### 4.1 CORS 生产配置

**现状**:
- `WebMvcConfig` 从 `cors.allowed-origins` 配置读取允许的源
- 默认值 `http://localhost:*,http://localhost:3000`
- 支持通配符模式

**评估**: ⚠️ **生产环境需确认配置**
- 开发环境默认值安全（仅 localhost）
- 生产环境必须通过配置文件限制域名

### 4.2 API 版本重定向路径遍历

**现状**: `ApiVersionRedirectConfig` 将 `/api/xxx` 重定向到 `/api/v1/xxx`

**评估**: ⚠️ **低风险但存在隐患**
- `uri.substring(4)` 直接拼接，理论上可以构造 `/api/../` 路径
- 但 Spring MVC 的 URI 规范化会处理 `..` 路径
- 307 重定向保留原始请求方法和体

**建议**: 添加 URI 规范化检查，拒绝包含 `..` 的路径

### 4.3 sb3 上传 ZIP 路径遍历

**现状**: `SB3Unzipper.unzip()` 已检查：
- `name.contains("..")` — 拒绝相对路径遍历
- `name.startsWith("/")` — 拒绝绝对路径

**评估**: ✅ **防护已到位**
- Zip Slip 攻击的主要载体已覆盖
- 额外建议：规范化路径后检查是否仍在预期目录

### 4.4 文件大小校验

**现状**:
- `FileConstants` 统一定义大小限制
- `FileUploadUtils` 在上传时校验
- `SB3Unzipper` 在解压时校验总大小和单条目大小
- ZIP 炸弹防护：单条目 50MB 限制

**评估**: ✅ **校验完整**

---

## 5. 测试覆盖分析

### 5.1 后端测试

**现状**: 约 10 个测试类：
- `ApiIntegrationTest`, `HealthAndRateLimitTest`, `UserApiIntegrationTest`
- `UserServiceTest`, `PointServiceTest`
- `HomeworkServiceTest`, `CompetitionServiceTest`, `JudgeServiceTest`
- `SocialServiceTest`, `AiReviewServiceTest`
- `SensitiveWordFilterTest`, `SB3ParserTest`
- `AuthInterceptorTest`, `JwtUtilsTest`, `NotifyServiceTest`

**评估**: ⚠️ **覆盖不足**
- 缺少 `ProjectService`, `FeedService`, `RankService` 的测试
- 缺少 `CrossModuleQueryRepository` 的测试
- 集成测试没有使用 Testcontainers（依赖 H2 内存数据库）

### 5.2 前端测试

**现状**: 约 14 个测试文件：
- 组件测试、Store 测试、API 测试、路由测试
- E2E 测试：`auth.spec.ts`, `homepage.spec.ts`, `navigation.spec.ts`, `project.spec.ts`

**评估**: ✅ **覆盖较好**

---

## 6. 文档质量分析

### 6.1 文档分散

**现状**: `docs/` 目录包含 20+ 个文档文件，`docs/archive/` 包含大量历史审计报告

**评估**: ⚠️ **需要整理**
- 当前文档有价值但分散
- archive 目录过于庞大，应定期清理

### 6.2 踩坑记录

**现状**: `docs/PITFALLS.md` 存在，记录了一些常见问题

**评估**: ✅ **有基础文档**

### 6.3 README

**现状**: `README.md` 约 300 行，包含项目介绍、技术栈、快速开始等

**评估**: ✅ **内容完整**

---

## 优化优先级总结

| 优先级 | 项目 | 状态 |
|---|---|---|
| 🔴 高 | Token 刷新竞态条件修复 | 待实施 |
| 🔴 高 | point_log 复合索引 | 待实施 |
| 🔴 高 | API 版本重定向路径遍历防护 | 待实施 |
| 🟡 中 | CrossModuleQueryRepository 拆分 | 待实施 |
| 🟡 中 | 异步判题重试优化 | 待实施 |
| 🟡 中 | SseTokenService 清理机制完善 | 待实施 |
| 🟡 中 | 前端 API 类型安全改进 | 待实施 |
| 🟡 中 | 路由进度条 Vue 组件化 | 待实施 |
| 🟢 低 | notification 归档策略 | 待实施 |
| 🟢 低 | 文档整理 | 待实施 |
| ✅ 已有 | Token Refresh 接口 | 已完整 |
| ✅ 已有 | Rate Limiter 滑动窗口 | 已实现 |
| ✅ 已有 | sb3 ZIP 路径遍历防护 | 已实现 |
| ✅ 已有 | 文件大小校验 | 已完整 |
