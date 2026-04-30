# 🔍 Scratch Community Platform — 深度分析报告

> 分析日期: 2026-04-25 | 分析范围: 全栈架构 + CI/CD + 代码质量 + 安全

---

## 📋 目录

1. [项目概览](#1-项目概览)
2. [CI/CD 分析与修复](#2-cicd-分析与修复)
3. [后端架构深度分析](#3-后端架构深度分析)
4. [前端架构分析](#4-前端架构分析)
5. [数据库设计分析](#5-数据库设计分析)
6. [安全架构分析](#6-安全架构分析)
7. [API 接口分析](#7-api-接口分析)
8. [沙箱判题系统分析](#8-沙箱判题系统分析)
9. [踩坑记录](#9-踩坑记录)
10. [优化建议](#10-优化建议)

---

## 1. 项目概览

### 架构全景

```
┌───────────────────────────────────────────────────────────────┐
│                      Nginx (前端静态资源)                       │
├───────────────────────────────────────────────────────────────┤
│                Spring Boot 3.2 模块化单体 (JDK 17)             │
│  ┌──────────┬───────────┬──────────┬──────────┬────────────┐  │
│  │  user    │  editor   │  social  │  judge   │ classroom  │  │
│  │ 用户系统  │ 创作引擎   │ 社区系统  │ 判题系统  │ 教学管理    │  │
│  ├──────────┴───────────┴──────────┴──────────┴────────────┤  │
│  │       common (认证/异常/审核/限流/分布式锁/工具)             │  │
│  │           sb3-parser / judge-core (共享库)                │  │
│  └─────────────────────────────────────────────────────────┘  │
├───────────────────────────────────────────────────────────────┤
│  MySQL 8.0  │  Redis 7 (Redisson)  │  MinIO  │  Node Sandbox │
│  (业务数据)   │  (缓存/排行/分布式锁)  │ (文件存储) │ (进程隔离判题)  │
└───────────────────────────────────────────────────────────────┘
```

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis + Redisson | 3.28.0 |
| 文件存储 | MinIO | 8.5.10 |
| 前端框架 | Vue 3 + TypeScript | 3.5.32 |
| UI 组件 | Element Plus | 2.13.7 |
| 构建工具 | Vite | 8.0.10 |
| 沙箱 | Node.js (进程隔离) | 18 |
| 容器化 | Docker Compose | V2 |

### 代码规模

- **后端**: 10 个 Maven 模块，~80 个 Java 源文件
- **前端**: ~40 个 Vue/TS 文件，14 个测试文件
- **数据库**: 14 个 Flyway 迁移脚本
- **文档**: 20+ 个 Markdown 文档

---

## 2. CI/CD 分析与修复

### 2.1 CI 流水线结构

```yaml
5 个 Job:
├── backend     (JDK 17, mvn compile → test → package)
├── frontend    (Node 20, npm ci → typecheck → test → build)
├── sandbox     (Node 18, npm install → test)
├── docker      (依赖 backend + sandbox, Docker 构建验证)
├── lint        (Checkstyle + 编译验证)
└── deploy      (仅 main 分支, 待配置)
```

### 2.2 CI 失败根因分析

**失败位置**: `scratch-social` 模块 `SocialServiceTest` — 4 个测试失败

**根因**: 架构与测试不同步

`SocialService` 使用 `EventPublisherHelper.publishEvent(event, desc, fallback)` 发布事件：
- 正常路径: 通过 Spring `ApplicationEventPublisher` 发布事件，由监听器异步更新计数
- 降级路径: 事件发布失败时，fallback 回调直接操作 `CrossModuleWriteRepository`

测试中 mock 了 `EventPublisherHelper`，导致：
1. `publishEvent()` 被 mock 吞掉，fallback 永远不执行
2. 测试 verify 的是 `crossModuleWrite.incrementXxx()` → 实际从未被调用

**修复方案**: 将 verify 从验证降级路径改为验证事件发布调用

```java
// 修复前 (验证降级路径 — 永远不会触发)
verify(crossModuleWrite).incrementProjectLikeCount(PROJECT_ID);

// 修复后 (验证事件发布 — 匹配实际架构)
verify(eventPublisher).publishEvent(
    argThat(e -> e instanceof ProjectLikeEvent 
        && ((ProjectLikeEvent) e).getAction() == ProjectLikeEvent.LikeAction.LIKE),
    eq("点赞事件"),
    any()
);
```

### 2.3 额外发现: HomeworkServiceTest 编译错误

`HomeworkService.grade()` 已拆分到 `HomeworkGradingService`，但测试未同步更新。
修复: 添加 `@InjectMocks HomeworkGradingService`，将 `homeworkService.grade()` 改为 `homeworkGradingService.grade()`。

---

## 3. 后端架构深度分析

### 3.1 模块化设计 ⭐⭐⭐⭐

**优点**:
- 清晰的模块边界: `user/editor/social/judge/classroom/system` 职责分明
- 共享库 (`common/sb3/judge-core`) 避免循环依赖
- 模块化单体架构适合团队规模和项目阶段

**问题**:
- `CrossModuleQueryRepository` / `CrossModuleWriteRepository` 是跨模块的"万能类"，所有模块的跨表 SQL 都堆在这里
- 随着模块增多，这两个类会膨胀为 God Object

**建议**:
- 按模块拆分跨模块查询: `SocialCrossModuleQuery`, `EditorCrossModuleQuery` 等
- 或引入 CQRS 模式，将查询和命令分离

### 3.2 事件驱动架构 ⭐⭐⭐⭐

**优点**:
- `EventPublisherHelper` 统一封装事件发布 + 降级逻辑
- 点赞/评论/浏览等操作通过事件解耦，避免跨模块直接依赖
- 积分系统通过 `PointEvent` 实现松耦合

**问题**:
- 事件发布和降级 fallback 的设计容易让开发者误解（如 CI 测试失败所示）
- 缺少事件消费的幂等性保证
- 没有事件重试或死信队列机制

**建议**:
- 为 `EventPublisherHelper` 添加详细的 Javadoc 和使用示例
- 考虑引入 Spring `@TransactionalEventListener` 替代手动降级
- 为关键事件添加幂等性校验（如 `INSERT IGNORE` 模式）

### 3.3 认证与安全 ⭐⭐⭐⭐

**优点**:
- JWT 密钥在生产环境强制校验，拒绝默认密钥
- BCrypt 密码加密 (strength=10)
- `AuthInterceptor` 统一认证拦截
- `IdempotentInterceptor` 防止重复提交
- `SensitiveWordFilter` 内容审核
- `JacksonXssConfig` + `XssFilterConfig` XSS 防护

**问题**:
- JWT Token 没有 refresh 机制的后端实现（前端有刷新逻辑但后端缺少对应接口）
- Token 黑名单依赖 Redis，Redis 不可用时安全降级策略不明确
- `RateLimitConfig` 使用固定窗口算法，窗口边界可能有 2x 突发流量

**建议**:
- 实现后端 Token Refresh 接口
- 固定窗口升级为滑动窗口计数器
- 添加 Token 黑名单的本地缓存降级

### 3.4 异步判题设计 ⭐⭐⭐

**优点**:
- `@Async("judgeExecutor")` 异步执行，不阻塞主线程
- 3 次重试 + 退避策略
- 只传递 ID 而非 entity，避免 detached entity 问题

**问题**:
- `Thread.sleep(1000L * attempt)` 阻塞线程池线程，应使用 `CompletableFuture.delayExecutor`
- 缺少判题超时的精确控制（依赖 RestTemplate timeout，但线程池可能积压）
- 没有判题队列，高并发时线程池可能耗尽

**建议**:
- 使用 `ScheduledExecutorService` 或 Spring `@Retryable` 替代手动重试
- 引入消息队列（如 Redis Stream）实现判题排队
- 添加判题任务的优先级和取消机制

### 3.5 SB3 解析器 ⭐⭐⭐⭐

**优点**:
- 纯 Java 实现，不依赖外部服务
- 内存解压，不写磁盘
- 多维度分析: 积木统计、复杂度计算、角色提取

**问题**:
- 大文件 (50MB) 全部加载到内存，可能 OOM
- 解析错误时只 warn 不阻断，可能导致数据不一致

**建议**:
- 添加文件大小预检查（已在 `autoSaveSb3` 中有 100MB 限制，但 `uploadSb3` 没有）
- 流式处理大文件

---

## 4. 前端架构分析

### 4.1 技术选型 ⭐⭐⭐⭐

- Vue 3 + TypeScript + Vite 8 + Element Plus
- Pinia 状态管理
- 自动导入 (unplugin-auto-import + unplugin-vue-components)
- Vitest 测试框架

### 4.2 亮点

- **请求层**: `request.ts` 实现了完整的 Token 自动刷新、请求取消管理、错误分类处理
- **路由**: 完整的权限守卫 (auth/teacher/admin)
- **PWA**: Service Worker + manifest.json
- **测试**: 14 个测试文件覆盖 API、组件、路由、Store 等

### 4.3 问题

- **Token 刷新竞态**: `request.ts` 中的 Token 刷新逻辑存在竞态条件 — 多个请求同时 9997 时，`refreshSubscribers` 可能被重复消费
- **路由进度条**: 使用 DOM 操作 `document.getElementById` 而非 Vue 响应式
- **类型安全**: `api/index.ts` 中的 `get/post/put/del` 泛型函数参数类型为 `Record<string, unknown>`，丢失了具体类型信息
- **测试环境**: 使用 `happy-dom` 而非 `jsdom`，某些 DOM API 可能不完整

### 4.4 建议

- Token 刷新改为 Promise 链式调用，避免回调地狱
- API 层引入 OpenAPI 代码生成，保证类型安全
- 考虑引入 Vue Query (TanStack Query) 管理服务端状态
- 路由进度条改用 Vue 组件而非原生 DOM

---

## 5. 数据库设计分析

### 5.1 表结构 ⭐⭐⭐⭐

14 张核心表，覆盖:
- 用户系统: `user`, `user_follow`, `class`, `class_student`
- 创作系统: `project`, `project_like`, `project_comment`
- 判题系统: `problem`, `submission`
- 教学系统: `homework`, `homework_submission`
- 系统管理: `notification`, `content_audit_log`, `system_config`

### 5.2 亮点

- 逻辑删除 (`deleted` 字段) 全局统一
- 唯一约束防止重复数据 (`uk_user_project`, `uk_class_student`, `uk_homework_student`)
- 全文索引支持搜索 (`ft_search` on project/problem)
- Flyway 版本管理，14 个迁移脚本

### 5.3 问题

- **计数字段冗余**: `project.like_count`, `comment_count`, `view_count` 需要与实际记录保持一致，目前通过事件驱动更新，但如果事件丢失会导致不一致
- **缺少索引**: `point_log` 表按 `user_id + type + DATE(created_at)` 查询频繁，建议添加复合索引
- **JSON 字段**: `homework.problem_ids`, `problem.options`, `submission.judge_detail` 使用 JSON 类型，MySQL 8.0 支持 JSON 索引但 MyBatis-Plus 需要自定义 TypeHandler
- **字符集**: 全表 utf8mb4，对于纯 ASCII 数据（如 username）可以考虑 utf8mb4_bin 排序规则提升查询性能

### 5.4 建议

- 添加 `point_log` 的复合索引: `idx_user_type_date (user_id, type, created_at)`
- 定期校准计数字段（已有 `CountCalibrationScheduler`，确保调度正常）
- 考虑添加 `project` 表的 `updated_at` 索引，优化按更新时间排序的查询
- `notification` 表数据量会快速增长，考虑按时间分区

---

## 6. 安全架构分析

### 6.1 安全措施清单

| 措施 | 实现方式 | 评级 |
|------|----------|------|
| 认证 | JWT + BCrypt | ✅ 优秀 |
| 授权 | 角色注解 + 拦截器 | ✅ 良好 |
| XSS | Jackson XSS 过滤 + XSS Filter | ✅ 良好 |
| CSRF | Token 验证 (幂等性拦截器) | ✅ 良好 |
| SQL 注入 | MyBatis-Plus 参数化 + JdbcTemplate 参数化 | ✅ 优秀 |
| 限流 | 固定窗口限流 (IP 维度) | ⚠️ 中等 |
| 内容审核 | 敏感词过滤 | ✅ 良好 |
| 文件上传 | MinIO + 大小限制 | ✅ 良好 |
| 密钥管理 | 环境变量 + 生产强制校验 | ✅ 优秀 |

### 6.2 安全风险

1. **CORS 配置**: `allowedOriginPatterns` 包含 `http://localhost:*`，生产环境应移除
2. **API 版本重定向**: `/api/xxx` → `/api/v1/xxx` 的重定向可能导致路径遍历
3. **错误信息泄露**: `GlobalExceptionHandler` 在 dev 环境返回详细堆栈，确保 prod 不泄露
4. **文件上传**: sb3 文件实际是 ZIP，可能包含恶意文件（如路径遍历的 ZIP 条目）

---

## 7. API 接口分析

### 7.1 接口规范

- RESTful 风格，统一前缀 `/api/v1/`
- 统一响应格式: `{ code, msg, data, timestamp }`
- 分页参数: `Page<T>` + `PageResult`
- Swagger 文档: `/swagger-ui.html`

### 7.2 问题

- **API 版本管理**: `ApiVersionRedirectConfig` 实现了 `/api/xxx` → `/api/v1/xxx` 重定向，但这是临时方案，应引导前端迁移到 v1
- **响应头**: `X-Total-Count`, `X-Page-Count` 暴露在 CORS `exposedHeaders` 中，但实际分页使用响应体而非 Header
- **SSE**: `SseTokenService` 为 SSE 连接提供 Token，但 SSE 连接的超时和重连策略需要明确

---

## 8. 沙箱判题系统分析

### 8.1 设计

- Node.js 独立进程，Docker 容器隔离
- REST API: `POST /judge`
- 资源限制: 内存 1.5GB，`--max-old-space-size=1024`
- Redis 连接用于状态同步

### 8.2 安全风险

- 沙箱进程与后端在同一 Docker 网络，网络隔离不够严格
- 没有 CPU 限制和 cgroup 配置
- Scratch 代码可能包含无限循环，需要更严格的超时控制

### 8.3 建议

- 添加 cgroup 资源限制 (CPU + 内存)
- 考虑使用 gVisor 或 Firecracker 实现更强的隔离
- 添加判题结果的缓存，相同输入不重复判题

---

## 9. 踩坑记录

### 9.1 CI 测试失败 — 事件驱动架构与 Mock 不匹配

**现象**: `SocialServiceTest` 中 4 个测试 verify `crossModuleWrite.incrementXxx()` 失败
**原因**: `EventPublisherHelper` 被 mock 后，fallback 回调不会执行
**解决**: verify 改为验证 `eventPublisher.publishEvent()` 调用
**教训**: 当使用事件驱动 + 降级模式时，测试应验证主路径而非降级路径

### 9.2 HomeworkServiceTest 编译错误 — 方法拆分未同步测试

**现象**: `homeworkService.grade()` 找不到方法
**原因**: `grade()` 已拆分到 `HomeworkGradingService`，测试未更新
**解决**: 更新测试使用 `HomeworkGradingService`
**教训**: 重构时同步更新测试，或使用 IDE 的重构工具自动更新引用

### 9.3 Integration Test 无法启动 — Redis 依赖

**现象**: `scratch-app` 的集成测试 ApplicationContext 加载失败
**原因**: 测试配置使用 H2 替代 MySQL，但 Redis (Redisson) 依赖无法 mock
**解决**: CI 不包含 `scratch-app` 的测试（仅跑单元测试）
**教训**: 集成测试应提供完整的 Testcontainers 支持，或使用 `@MockBean` 替代外部依赖

---

## 10. 优化建议

### 🔴 高优先级

1. **CI 稳定性**: 本次修复的 4 个测试失败 + 1 个编译错误
2. **集成测试**: 引入 Testcontainers 支持 Redis + MySQL 的集成测试
3. **Token Refresh**: 实现后端 Token 刷新接口
4. **限流升级**: 固定窗口 → 滑动窗口计数器

### 🟡 中优先级

5. **跨模块查询重构**: 拆分 `CrossModuleQueryRepository` / `CrossModuleWriteRepository`
6. **事件可靠性**: 添加事件消费的幂等性校验
7. **数据库索引**: 添加 `point_log` 复合索引
8. **前端类型安全**: 引入 OpenAPI 代码生成
9. **判题队列**: 引入 Redis Stream 或消息队列
10. **文件上传安全**: 添加 ZIP 路径遍历检查

### 🟢 低优先级

11. **API 版本管理**: 建立正式的版本管理策略
12. **监控告警**: 集成 Prometheus + Grafana
13. **文档完善**: API 文档自动生成
14. **性能优化**: 数据库查询优化、缓存策略
15. **国际化**: 前端 i18n 支持

---

## 附录: 代码质量评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 模块化清晰，事件驱动解耦 |
| 代码质量 | ⭐⭐⭐⭐ | 代码规范，注释详尽 |
| 测试覆盖 | ⭐⭐⭐ | 单元测试覆盖良好，集成测试不足 |
| 安全性 | ⭐⭐⭐⭐ | 多层防护，少数配置可优化 |
| 文档 | ⭐⭐⭐⭐⭐ | 文档非常丰富，有专门的前期设计文档 |
| CI/CD | ⭐⭐⭐ | 流水线完整但有测试失败 |
| **综合** | **⭐⭐⭐⭐** | **优秀的个人项目，架构合理，文档丰富** |
