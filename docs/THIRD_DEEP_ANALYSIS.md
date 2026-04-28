# 🔍 Scratch Community Platform — 第三轮深度审计报告

> **审计日期**: 2026-04-29
> **审计人**: AI Assistant (自动化审计)
> **审计范围**: 全栈架构 + 编译部署 + CI/CD + 代码质量 + 安全 + 数据库 + API
> **项目版本**: v3.5.0 (Phase 12)

---

## 📋 审计摘要

| 维度 | 评级 | 关键发现 |
|------|------|---------|
| 编译构建 | ✅ 优秀 | 13 模块全部编译成功，无 warning |
| 单元测试 | ✅ 良好 | 后端 7 测试 + 前端 166 测试全部通过 |
| API 功能 | ✅ 良好 | 健康检查/Feed/注册/登录均正常 |
| 架构设计 | ⭐⭐⭐⭐ | 模块化清晰，事件驱动解耦 |
| 代码质量 | ⭐⭐⭐⭐ | 代码规范，注释详尽 |
| 安全性 | ⭐⭐⭐⭐ | 多层防护，JWT/BCrypt/限流/敏感词 |
| 数据库 | ⭐⭐⭐⭐ | 24 表 + 19 Flyway 迁移 + 全文索引 |
| 前端架构 | ⭐⭐⭐⭐ | Vue 3 + TS + Vite + 166 测试 |
| 文档 | ⭐⭐⭐⭐⭐ | 极其丰富，20+ 文档 |
| **综合** | **⭐⭐⭐⭐** | **优秀的全栈项目** |

---

## 1. 编译与部署验证

### 1.1 环境搭建

| 组件 | 版本 | 状态 |
|------|------|------|
| JDK | OpenJDK 17.0.18 | ✅ |
| Maven | 3.8.7 | ✅ |
| Node.js | 22.22.1 | ✅ |
| MySQL | 8.0.45 | ✅ |
| Redis | 7.0.15 | ✅ |

### 1.2 后端编译

```
Reactor Summary:
scratch-common-core .................... SUCCESS [02:26 min]
scratch-common-redis ................... SUCCESS [26.133 s]
scratch-common-security ................ SUCCESS [ 4.977 s]
scratch-common-audit ................... SUCCESS [ 0.378 s]
scratch-sb3 ............................ SUCCESS [ 0.697 s]
scratch-judge-core ..................... SUCCESS [ 0.362 s]
scratch-user ........................... SUCCESS [ 1.361 s]
scratch-editor ......................... SUCCESS [ 0.785 s]
scratch-social ......................... SUCCESS [ 1.244 s]
scratch-judge .......................... SUCCESS [ 1.054 s]
scratch-classroom ...................... SUCCESS [ 0.824 s]
scratch-system ......................... SUCCESS [ 0.626 s]
scratch-app ............................ SUCCESS [ 9.305 s]
BUILD SUCCESS (3:43 min)
```

### 1.3 后端测试

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (1:00 min)
```

### 1.4 前端构建

```
vite v8.0.10 building client environment for production...
✓ 1922 modules transformed.
✓ built in 1.66s
dist/assets/js/index-fUL63v4O.js  842.92 kB │ gzip: 273.20 kB
```

### 1.5 前端测试

```
Test Files: 14 passed (14)
Tests: 166 passed (166)
Duration: 7.68s
```

### 1.6 API 验证

| 端点 | 状态 | 响应 |
|------|------|------|
| GET /api/health | ✅ 200 | `{"status":"UP","version":"3.0.0"}` |
| GET /swagger-ui.html | ✅ 302 | 重定向到 Swagger UI |
| GET /api/v1/social/feed | ✅ 200 | `{"records":[],"total":0}` |
| POST /api/v1/user/register | ✅ 200 | JWT Token 返回 |

---

## 2. 后端架构分析

### 2.1 模块化设计 ⭐⭐⭐⭐

**优点**:
- 13 个 Maven 模块，职责分明
- `common-*` 共享层避免循环依赖
- 模块化单体架构适合当前团队规模

**问题**:
- `CrossModuleQueryRepository`（~400 行）和 `CrossModuleWriteRepository`（~200 行）是跨模块的"万能类"
- 部分方法已标记 `@Deprecated` 但仍被使用
- `CrossModuleQueryRepository` 中的 `getProjectFeed()` 与 `FeedService` 逻辑重复

**建议**:
- 按模块拆分: `ProjectQueryRepository`, `UserQueryRepository`, `HomeworkQueryRepository`
- 清理 `@Deprecated` 方法的调用方

### 2.2 事件驱动架构 ⭐⭐⭐⭐

**优点**:
- `EventPublisherHelper` 统一封装事件发布 + 降级逻辑
- `@TransactionalEventListener(AFTER_COMMIT)` 避免读取未提交数据
- 点赞/评论/浏览/积分全部通过事件解耦

**问题**:
- `ProjectService.getDetail()` 中的事件发布没有使用 `EventPublisherHelper`，而是手动 try-catch
- 积分事件缺少幂等性保障（重复消费会导致积分重复发放）
- 没有死信队列机制

**建议**:
- 统一使用 `EventPublisherHelper`
- 为积分事件添加幂等性检查（基于 `refType + refId` 去重）

### 2.3 异步判题 ⭐⭐⭐

**优点**:
- `@Async("judgeExecutor")` 独立线程池
- 指数退避重试（1s, 2s, 4s）
- 只传 ID 避免 detached entity 问题

**问题**:
- `Thread.sleep()` 仍在线程中（虽有 `CompletableFuture.delayedExecutor` 但未实际使用）
- 判题队列依赖线程池队列，高并发时可能耗尽

**建议**:
- 使用 `CompletableFuture.delayedExecutor` 替代 `Thread.sleep`
- 考虑引入 Redis Stream 实现判题排队

### 2.4 认证与安全 ⭐⭐⭐⭐

**优点**:
- JWT 双令牌（Access + Refresh）
- BCrypt 密码加密（strength=10）
- 生产环境强制校验默认密钥
- Token 黑名单 + 用户级黑名单
- `@RateLimit` 注解 + 滑动窗口限流
- DFA 敏感词过滤

**问题**:
- CORS 配置包含 `http://localhost:*`，生产环境应移除
- `RateLimitConfig` 使用内存存储，多实例部署时限流不共享

**建议**:
- 生产环境 CORS 白名单化
- 限流器升级为 Redis 实现（已有 `RedisRateLimiter` 条件 Bean）

---

## 3. 前端架构分析

### 3.1 技术选型 ⭐⭐⭐⭐

- Vue 3.5 + TypeScript 6.0 + Vite 8.0 + Element Plus 2.13
- Pinia 状态管理 + Vue Router 4.6
- 自动导入 (unplugin-auto-import + unplugin-vue-components)
- Vitest 4.1 测试框架

### 3.2 Token 刷新机制 ⭐⭐⭐⭐

**优点**:
- `doRefreshToken()` 实现了 Promise 去重，防止并发刷新
- `isRetryingPending` 标记防止 refreshPromise 过早清除
- 请求队列 (`pendingRequests`) 等待 Token 刷新后重试

**问题**:
- 竞态窗口: `setTimeout(() => { refreshPromise = null }, 100)` 的 100ms 延迟可能不够
- `_isRetry` 标记在 Axios 内部 config 上，类型不安全

**建议**:
- 改为更可靠的 Promise 链式调用
- 使用 Map 管理重试状态而非扩展 config 对象

### 3.3 路由架构 ⭐⭐⭐⭐

- 20+ 路由，含权限守卫 (auth/teacher/admin)
- 首次加载时验证 Token 有效性
- 路由级 `keepAlive` 优化

### 3.4 测试覆盖 ⭐⭐⭐⭐

- 14 个测试文件，166 个测试
- 覆盖: API / 组件 / 路由 / Store / Composable / SSE / Utils / 集成

---

## 4. 数据库设计分析

### 4.1 表结构 ⭐⭐⭐⭐

24 张表，覆盖:
- 用户: `user`, `user_follow`, `user_oauth`, `class`, `class_student`
- 创作: `project`, `project_like`, `project_comment`
- 判题: `problem`, `submission`
- 教学: `homework`, `homework_submission`, `homework_problem`
- 竞赛: `competition`, `competition_registration`, `competition_ranking`, `competition_problem`
- 社区: `ai_review`, `collab_session`, `collab_participant`
- 系统: `notification`, `content_audit_log`, `system_config`, `point_log`

### 4.2 索引策略 ⭐⭐⭐⭐

- V1-V19 共 19 个 Flyway 迁移脚本
- 全文索引: `project.ft_search`, `problem.ft_search` (ngram parser)
- 复合索引覆盖主要查询路径
- 唯一约束防止重复数据

### 4.3 问题

- `init.sql` 中 `CREATE INDEX IF NOT EXISTS` 语法在 MySQL 8.0 中不支持
- 部分索引在 `init.sql` 和 Flyway 迁移中重复定义
- JSON 字段 (`parse_result`, `options`, `judge_detail`) 缺少索引

---

## 5. CI/CD 分析

### 5.1 CI 流水线 ⭐⭐⭐

```
5 个 Job:
├── backend     (JDK 17, compile → test → package)
├── frontend    (Node 20, ci → typecheck → test → build)
├── sandbox     (Node 18, install → test)
├── docker      (Docker 构建验证)
└── lint        (Checkstyle + 编译验证)
```

### 5.2 问题

- `docker` Job 依赖 `backend` + `sandbox`，但 `deploy` Job 条件为 `github.ref == 'refs/heads/main'`，永远不会在 PR 中触发
- `lint` Job 的 Checkstyle 使用 `|| true`，即使失败也不阻断
- `frontend` Job 使用 Node 20，但 `package.json` 中依赖 Vite 8 需要 Node 22+

### 5.3 CD 流水线 ⭐⭐⭐⭐

- 手动触发 + CI 通过后自动触发
- Docker 镜像构建 + 推送到 GHCR
- SSH 部署 + 健康检查 + 自动回滚
- 部署后冒烟测试 + GitHub Release

---

## 6. 安全审计

### 6.1 安全措施清单

| 措施 | 实现 | 评级 |
|------|------|------|
| 认证 | JWT 双令牌 + BCrypt | ✅ 优秀 |
| 授权 | 角色注解 + 拦截器 | ✅ 良好 |
| XSS | Jackson XSS + XSS Filter | ✅ 良好 |
| CSRF | 幂等性拦截器 | ✅ 良好 |
| SQL 注入 | 参数化查询 | ✅ 优秀 |
| 限流 | 滑动窗口 (IP 维度) | ✅ 良好 |
| 内容审核 | DFA 敏感词 | ✅ 良好 |
| 文件上传 | MinIO + 大小限制 | ✅ 良好 |
| 密钥管理 | 环境变量 + 生产强制校验 | ✅ 优秀 |
| Token 黑名单 | Redis 存储 | ✅ 良好 |

### 6.2 发现的安全问题

1. **init.sql 密码硬编码**: `admin123` 的 BCrypt hash 在 init.sql 中，但 Flyway V2 也有，优先使用 Flyway
2. **CORS localhost**: 开发环境配置包含 `localhost:*`，生产环境需收紧
3. **文件上传**: sb3 是 ZIP 格式，需检查 ZIP 路径遍历

---

## 7. 优化建议（按优先级）

### 🔴 高优先级

| # | 问题 | 建议 | 影响 |
|---|------|------|------|
| 1 | init.sql `CREATE INDEX IF NOT EXISTS` 语法错误 | 移除 IF NOT EXISTS | 部署阻断 |
| 2 | ProjectService 事件发布不一致 | 统一使用 EventPublisherHelper | 代码一致性 |
| 3 | JudgeService Thread.sleep | 改用 CompletableFuture.delayedExecutor | 性能 |
| 4 | CrossModuleQueryRepository God Object | 按模块拆分 | 可维护性 |

### 🟡 中优先级

| # | 问题 | 建议 | 影响 |
|---|------|------|------|
| 5 | 积分事件幂等性 | 添加 refType+refId 去重 | 数据一致性 |
| 6 | CORS 生产配置 | 白名单化 | 安全 |
| 7 | 前端 Token 刷新竞态 | 优化 Promise 链 | 稳定性 |
| 8 | CI Node 版本 | 统一 Node 22 | CI 兼容性 |

### 🟢 低优先级

| # | 问题 | 建议 | 影响 |
|---|------|------|------|
| 9 | @Deprecated 方法清理 | 移除或替换调用方 | 代码整洁 |
| 10 | JSON 字段索引 | MySQL 8.0 JSON 索引 | 查询性能 |
| 11 | 通知表分区 | 按时间分区 | 大数据量性能 |
| 12 | API 文档自动生成 | OpenAPI 代码生成 | 开发效率 |

---

## 8. 代码质量评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 模块化清晰，事件驱动解耦 |
| 代码质量 | ⭐⭐⭐⭐ | 代码规范，注释详尽 |
| 测试覆盖 | ⭐⭐⭐⭐ | 166 前端 + 7 后端测试 |
| 安全性 | ⭐⭐⭐⭐ | 多层防护 |
| 文档 | ⭐⭐⭐⭐⭐ | 极其丰富 |
| CI/CD | ⭐⭐⭐ | 流水线完整但有改进空间 |
| 数据库 | ⭐⭐⭐⭐ | 设计合理，索引完善 |
| **综合** | **⭐⭐⭐⭐** | **优秀的全栈项目** |

---

*报告结束。后续将实施具体优化。*
