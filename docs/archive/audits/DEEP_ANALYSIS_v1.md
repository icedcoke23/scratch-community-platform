# Scratch Community Platform — 深度全面分析报告

> 分析时间: 2026-04-24
> 分析范围: 底层架构、代码逻辑、API 接口、数据库设计、前端架构、文档体系、踩坑记录

---

## 一、项目总览

这是一个**面向少儿编程的 Scratch 编程社区平台**，集创作、分享、判题、教学于一体。项目从需求分析到实现经历了 14 个 Sprint，目前已进入 v2.2 架构优化阶段。

**核心数据**:
- 后端: Spring Boot 3.2 + JDK 17，10 个 Maven 模块
- 前端: Vue 3 + TypeScript + Vite 8 + Element Plus
- 判题沙箱: Node.js 18 + scratch-vm headless
- 基础设施: MySQL 8.0 + Redis 7 + MinIO + Docker Compose
- 代码规模: ~150+ Java 文件，~30+ Vue/TS 文件，9 个 Flyway 迁移脚本
- 文档: 66 条踩坑记录，52+ 条经验总结，8 次审计报告

---

## 二、底层架构分析

### 2.1 整体架构: 模块化单体 (Modular Monolith)

```
┌─────────────────────────────────────────────────┐
│              Nginx (前端静态 + 反向代理)           │
├─────────────────────────────────────────────────┤
│         Spring Boot 3.2 模块化单体 (JDK 17)       │
│  ┌───────┬────────┬────────┬───────┬──────────┐ │
│  │ user  │ editor │ social │ judge │ classroom │ │
│  │       │        │        │       │  system   │ │
│  ├───────┴────────┴────────┴───────┴──────────┤ │
│  │   common (认证/异常/审核/限流/工具)            │ │
│  │   sb3-parser / judge-core (共享库)           │ │
│  └─────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────┤
│  MySQL 8.0 │ Redis 7 │ MinIO │ Node.js Sandbox  │
└─────────────────────────────────────────────────┘
```

**架构评价**: ⭐⭐⭐⭐☆

**优点**:
- 模块化单体是正确的起步选择——比纯单体清晰，比微服务简单
- 10 个 Maven 模块职责清晰，模块间通过 `CrossModuleQueryRepository` / `CrossModuleWriteRepository` 解耦
- 共享库 (`scratch-sb3`, `scratch-judge-core`) 独立为纯 Java 库，不依赖 Spring

**问题与建议**:

1. **模块间依赖方向不明确** — `scratch-social` 依赖 `scratch-common` 但直接写 `project` 表（editor 模块的表）。虽然通过 Repository 集中了 SQL，但没有用事件驱动来解耦。
   - **建议**: 引入 Spring Event 或消息队列，social 模块发出 `ProjectLikedEvent`，editor 模块自己更新 `like_count`。

2. **scratch-judge-core 实现较薄** — 只有 `JudgeEngine` 接口、`JudgeResult` 模型、`Verdict` 枚举。实际判题逻辑在 `JudgeService`（scratch-judge 模块）和 `JudgeWorker`（sandbox）中，核心库的价值有限。
   - **建议**: 要么将判题策略（比较逻辑、评分规则）也抽到 core 中，要么承认这个模块是预留的，降低架构图中的权重。

3. **缺少 API Gateway / BFF 层** — 所有 API 直接暴露在 Spring Boot 中。未来如果有移动端、第三方接入，需要统一的 API 网关做鉴权、限流、版本管理。
   - **当前阶段可以接受**，但架构图中应标注这是"Phase 5 规划"。

### 2.2 安全架构

**评价**: ⭐⭐⭐⭐⭐ (在同类项目中非常出色)

安全层设计非常扎实:
- **JWT + Token 黑名单** (Token 级 + 用户级双重检查)
- **DFA 敏感词自动机** (O(n) 匹配，类型安全的 DfaNode 内部类)
- **Redisson 分布式锁** (积分并发安全)
- **滑动窗口限流** (Redis + Lua，IP 级)
- **BCrypt 密码加密**
- **生产环境强制校验 JWT 密钥** (拒绝默认密钥)
- **Docker Compose 敏感配置强制设置** (`${VAR:?error}`)

**亮点**:
- `JwtUtils.validateConfig()` 在 prod 环境直接拒绝默认密钥，启动失败 > 安全妥协
- `AuthInterceptor` 同时支持 Header 和 query 参数（SSE 场景），但限制 `/stream` 端点
- `SensitiveWordFilter` 使用 `DfaNode` 类型安全内部类，消除了 `Map<Object, Object>` 的类型不安全

**仍需注意**:
- **XSS 防护只覆盖了 JSON Body 的部分字段** — 自定义 Jackson 反序列化器对所有 String 做 HTML 转义，但 `@RequestBody` 中嵌套对象的 String 字段可能未覆盖
- **CSRF 防护缺失** — 如果前端通过 Cookie 存储 Token（当前用 localStorage + Header，OK），但如果将来改用 Cookie，需要 CSRF Token
- **SQL 注入防护** — 已参数化，但 `CrossModuleWriteRepository` 中的 `INSERT IGNORE` 和 `CASE WHEN` 批量更新需要持续审查

### 2.3 判题沙箱架构

**评价**: ⭐⭐⭐⭐☆

```
Spring Boot (JudgeService) ──HTTP──> Node.js Sandbox (Express)
                                        │
                                        ├── JudgeWorker (子进程管理)
                                        │   └── fork('judge-runner.js')
                                        │       └── scratch-vm headless
                                        └── Redis (任务队列 + 降级内存队列)
```

**优点**:
- 进程隔离 — 判题在子进程中执行，不会影响主进程
- 资源限制 — `NODE_OPTIONS: --max-old-space-size=1024` + Docker `memory: 1.5G`
- 降级方案 — Redis 不可用时降级为内存队列
- 优雅关闭 — SIGTERM/SIGINT 处理，终止所有活跃判题进程

**问题**:
1. **任务状态查询是轮询模式** — 后端通过 `GET /judge/status/:taskId` 轮询沙箱。如果判题时间长（30s 超时），后端线程池会被占满。
   - **建议**: 改用回调模式 — 沙箱判题完成后主动 POST 回调后端，或用 Redis Pub/Sub 通知。

2. **测试用例通过临时文件传递是正确的** (坑 56 已修复)，但临时文件清理依赖所有退出路径都执行 `fs.unlinkSync`。如果进程被 SIGKILL 直接杀死，临时文件会残留。
   - **建议**: 添加启动时清理逻辑 — 沙箱启动时扫描并删除 `/tmp/test_cases_*.json`。

3. **缺少判题并发控制** — 沙箱没有限制同时运行的判题任务数。如果大量提交同时到达，可能耗尽内存。
   - **建议**: 添加 `MAX_CONCURRENT_JOBS` 配置 + 信号量控制。

---

## 三、代码逻辑分析

### 3.1 后端代码质量

**评价**: ⭐⭐⭐⭐☆

**优点**:
- 分层清晰: Controller → Service → Mapper，DTO/VO 分离
- 全局异常处理完善 (`GlobalExceptionHandler`)，生产环境不暴露堆栈
- 事务管理正确 — `@Transactional(readOnly=true)` 用于查询，写操作有事务保护
- 日志规范 — 不同级别 (debug/info/warn/error) 使用恰当

**问题**:

1. **Service 类仍然偏大**:
   - `CompetitionService` 566 行（虽已拆出 `CompetitionRankingService`，但主 Service 仍大）
   - `HomeworkService` 预计 400+ 行
   - **建议**: 超过 300 行的 Service 继续拆分。将纯计算逻辑抽到 Helper/Calculator 类中。

2. **MyBatis-Plus 使用深度不够**:
   - 大量使用 `LambdaQueryWrapper`，这是好的
   - 但跨模块查询仍用 `JdbcTemplate` 裸 SQL — 这是不得已的做法（模块间不共享 Mapper），但增加了维护成本
   - **建议**: 考虑使用 MyBatis-Plus 的 `@Select` 注解或 XML Mapper 来管理跨模块查询，比裸 SQL 更类型安全

3. **缺少接口/抽象层**:
   - Service 直接依赖具体实现（如 `UserMapper`, `ProjectMapper`）
   - 如果将来需要 Mock 或切换实现（如从 MySQL 切到 PostgreSQL），改动面大
   - **当前阶段可接受**，但如果有测试需求，应抽接口

4. **事件驱动不完整**:
   - `PointEvent` 和 `PointEventListener` 存在，说明有事件机制
   - 但很多跨模块操作（如点赞更新 like_count）仍用同步 JDBC
   - **建议**: 扩大事件驱动范围 — `LikeEvent`, `CommentEvent`, `SubmissionEvent` 等

### 3.2 前端代码质量

**评价**: ⭐⭐⭐☆☆

**优点**:
- TypeScript 全覆盖，类型定义完整 (`types/index.ts`)
- API 层封装良好 — 统一的请求/响应拦截，Token 自动注入，过期自动登出
- Pinia 状态管理，Vue Router 路由守卫
- Vite 8 + Rolldown 构建，manualChunks 分包策略合理

**问题**:

1. **组件拆分不够** — 只有 8 个通用组件 (`EmptyState`, `LoadingSkeleton`, `CommentList`, `ProjectCard`, `ErrorBoundary`, `JudgeDetail`, `ProjectPreview`, `LoginDialog`)。各 View 文件可能非常大。
   - **建议**: 每个 View 超过 300 行时，拆出子组件。特别是 `FeedView`, `CompetitionDetailView`, `HomeworkDetailView`。

2. **缺少全局 Loading/Error 状态管理** — 虽然有 `useLoading` composable 和 `errorHandler.ts`，但各 View 独立管理自己的 loading 状态。
   - **建议**: 使用 Pinia store 统一管理全局 loading/error 状态，或使用 Vue Suspense。

3. **缺少路由懒加载的 Loading 骨架** — `() => import(...)` 懒加载时，用户会看到空白页面。
   - **建议**: 添加 NProgress 或自定义 Loading 组件。

4. **SSE 流式实现有安全隐患** — Token 通过 URL query 参数传递 (`?token=xxx`)。虽然限制了 `/stream` 端点，但 Token 会出现在：
   - 浏览器历史记录
   - 服务器访问日志
   - 代理服务器日志
   - **建议**: 使用一次性 SSE Token (服务端生成，短期有效，绑定用户) 替代 JWT Token。

5. **测试覆盖率不明确** — 有 `__tests__/` 目录 (4 个测试文件)，但没有看到覆盖率报告配置。
   - **建议**: 添加 vitest coverage 配置 (`@vitest/coverage-v8`)，在 CI 中检查覆盖率。

---

## 四、API 接口分析

### 4.1 API 设计评价

**评价**: ⭐⭐⭐⭐☆

**优点**:
- RESTful 风格，路径命名清晰 (`/api/project/{id}`, `/api/social/feed`)
- 统一返回体 `R<T>` 包含 `code`, `msg`, `data`, `timestamp`
- 分页支持 (`PageResult<T>`)
- Swagger/SpringDoc 自动生成 API 文档
- API 数量充足 (~80+ 接口覆盖所有功能模块)

**问题**:

1. **API 版本管理缺失** — 所有接口在 `/api/` 下，没有版本号 (`/api/v1/`)。
   - **建议**: 从现在开始用 `/api/v1/`，为未来 breaking change 做准备。

2. **部分接口设计不够 RESTful**:
   - `POST /social/project/{id}/like` 和 `DELETE /social/project/{id}/like` — 这是好的
   - 但 `POST /homework/grade` 应该是 `PUT /homework/submission/{id}/grade`
   - `POST /points/checkin` 不是 CRUD，用 POST 可以，但语义上是 action

3. **缺少接口幂等性保护** — 如提交判题 (`POST /judge/submit`)，用户快速双击会创建两个 submission。
   - **建议**: 添加幂等性 Token (客户端生成唯一 ID，服务端去重)。

4. **SSE Token 暴露问题** (已在前端部分提及) — 这是 API 设计层面的问题。

### 4.2 接口安全

- ✅ JWT 认证 + 角色校验 (`@RequireRole`)
- ✅ IP 限流 (120 次/分钟)
- ✅ 敏感词过滤
- ✅ Token 黑名单 (登出/禁用)
- ⚠️ 缺少接口幂等性
- ⚠️ 缺少请求体大小限制 (除文件上传外的 JSON Body)
- ⚠️ 缺少 CORS 精细控制 (当前应该是 `*` 或 localhost)

---

## 五、数据库设计分析

### 5.1 表结构评价

**评价**: ⭐⭐⭐⭐☆

**核心表** (18+ 张):
```
user, user_follow, class, class_student,
project, project_like, project_comment,
problem, submission,
homework, homework_submission,
notification, content_audit_log, system_config,
point_log,
competition, competition_registration, competition_ranking,
ai_review
```

**优点**:
- 范式合理 — 关系表有唯一约束 (`uk_follow`, `uk_class_student`, `uk_homework_student`, `uk_competition_user`)
- 逻辑删除 (`deleted` 字段) — 数据可恢复
- Flyway 版本管理 — 9 个迁移脚本，有序演进
- 索引设计合理 — V9 迁移添加了 8 个性能索引
- 全文索引 (`FULLTEXT ... WITH PARSER ngram`) — 支持中文搜索

**问题**:

1. **`project.like_count` / `comment_count` / `view_count` 冗余计数字段** — 这是典型的反范式设计，为了查询性能。
   - **风险**: 如果 `INSERT IGNORE` 更新 like_count 但事务回滚，计数会不准。
   - **建议**: 定期 (每天/每周) 运行校准脚本，将计数字段与实际 COUNT 对比修正。

2. **`user.points` / `user.level` 冗余字段** — 同上，需要校准机制。

3. **`homework.problem_ids` 用 JSON 存储** — 无法建外键，无法 JOIN 查询。
   - **建议**: 如果需要查询"某题出现在哪些作业中"，应该用关联表 `homework_problem`。

4. **`project.tags` 用逗号分隔的 VARCHAR** — 同样无法建索引。
   - **建议**: 使用标签表 `tag` + 关联表 `project_tag`，或用 MySQL 的 JSON 类型 + 虚拟列索引。

5. **缺少 `email` 字段的唯一约束** — V6 添加了 email 字段，但 User 实体有 email 唯一性检查代码 (Service 层)，数据库层面没有唯一约束。
   - **建议**: 添加 `UNIQUE KEY uk_email (email)`。

6. **缺少 `created_at` 的默认索引** — 几乎所有表都按 `created_at DESC` 排序，但只有少数表有这个索引。
   - **建议**: 对高频查询的表 (project, submission, notification) 添加 `created_at` 索引。

### 5.2 数据完整性

- ✅ 外键关系通过应用层维护 (MyBatis-Plus)
- ⚠️ 数据库层面没有外键约束 — 如果直接操作数据库，可能产生孤儿数据
- ✅ 唯一约束防止重复数据
- ✅ 逻辑删除保护数据

---

## 六、前端架构分析

### 6.1 技术选型

**评价**: ⭐⭐⭐⭐☆

| 选型 | 评价 |
|------|------|
| Vue 3 + Composition API | ✅ 正确选择，现代 Vue 开发标准 |
| TypeScript | ✅ 类型安全，减少运行时错误 |
| Vite 8 + Rolldown | ✅ 最新构建工具，速度快 |
| Element Plus | ✅ 成熟的 UI 库，适合快速开发 |
| Pinia | ✅ Vue 3 官方推荐状态管理 |
| Axios | ✅ 成熟的 HTTP 客户端 |
| Auto Import + Components | ✅ 减少样板代码 |

### 6.2 架构问题

1. **Scratch 编辑器集成方案** — 使用 TurboWarp iframe 嵌入是正确的选择 (避免 fork scratch-gui 的噩梦)。但:
   - iframe 与主应用的通信依赖 postMessage，需要严格验证消息来源
   - iframe 加载时间可能较长，需要 Loading 状态
   - 移动端 iframe 体验可能不佳

2. **缺少全局状态管理的分层**:
   - 只有 `user.ts` store
   - 缺少 `project.ts`, `competition.ts`, `notification.ts` 等 store
   - 各 View 自己管理数据，导致重复的 API 调用逻辑

3. **响应式设计** — README 提到 768px/480px 断点适配，但 Element Plus 本身是桌面优先的。移动端体验可能不够原生。
   - **建议**: 考虑使用 Vant 或 NutUI 作为移动端 UI 库，或使用 CSS 媒体查询精细化 Element Plus 的移动端样式。

4. **缺少 PWA 支持** — 对于少儿编程社区，PWA (离线缓存 + 推送通知) 会是很好的体验增强。

---

## 七、文档体系分析

### 7.1 文档评价

**评价**: ⭐⭐⭐⭐⭐ (在同类项目中极为出色)

文档体系非常完善:
- **前期设计**: 需求文档 (v0.5)、架构设计 (v0.3)、技术选型 (v0.4)、竞品分析、数据库设计
- **开发文档**: 开发计划、编码规范、模块开发指南、QA 检查清单、部署指南
- **踩坑记录**: 66 条真实踩坑 + 49+ 条经验总结 + 8 次审计报告
- **版本管理**: CHANGELOG.md 记录每个版本的变更

**亮点**:
- 踩坑记录不是简单的"问题→修复"，而是包含现象、原因、错误代码、修复方案、教训总结
- 每条踩坑标注了严重性 (🔴/🟡/🟢)，便于优先级排序
- 审计报告逐项对照源码确认修复状态

**问题**:
- **文档量过大** — PITFALLS.md 已经 1000+ 行，信息密度高但阅读负担重
  - **建议**: 按模块拆分踩坑记录，或创建一个"快速索引"章节
- **设计文档版本过多** — `前期/archive/` 下有大量历史版本
  - **建议**: 只保留最新版本，历史版本通过 Git 历史追溯

---

## 八、DevOps 与部署分析

### 8.1 Docker Compose

**评价**: ⭐⭐⭐⭐☆

**优点**:
- 完整的基础设施: MySQL + Redis + MinIO + Backend + Sandbox + Frontend
- 健康检查: 所有服务都有 healthcheck
- 资源限制: 每个服务都有 memory limit
- 敏感配置强制设置: `${VAR:?error}` 语法
- 依赖顺序: `depends_on` + `condition: service_healthy`

**问题**:
1. **单机部署** — Docker Compose 适合开发和小规模部署，但不适合生产环境的高可用需求。
   - **建议**: 生产环境考虑 Kubernetes 或 Docker Swarm。

2. **缺少日志收集** — 各容器日志分散，没有集中收集。
   - **建议**: 添加 ELK/Loki 或使用 Docker 的日志驱动。

3. **缺少监控告警** — 有 Actuator 健康检查，但没有 Prometheus + Grafana。
   - **建议**: 添加 Prometheus metrics 暴露 + Grafana 看板。

### 8.2 CI/CD

- ✅ GitHub Actions CI (`.github/workflows/ci.yml`)
- ✅ 前端构建 + 安全扫描
- ⚠️ 缺少自动部署流程 (CD)
- ⚠️ 缺少环境管理 (staging/production)

---

## 九、综合优化建议

### 🔴 高优先级 (影响安全/稳定性)

| # | 问题 | 建议 | 工作量 |
|---|------|------|--------|
| 1 | SSE Token 通过 URL 传递有日志泄露风险 | 使用一次性 SSE Token | 1d |
| 2 | 缺少判题并发控制 | 添加 MAX_CONCURRENT_JOBS + 信号量 | 0.5d |
| 3 | 冗余计数字段无校准机制 | 添加定时校准脚本 | 1d |
| 4 | email 字段缺少数据库唯一约束 | Flyway 添加 UNIQUE KEY | 0.5d |
| 5 | 缺少接口幂等性保护 | 添加幂等性 Token 机制 | 1d |

### 🟡 中优先级 (影响可维护性/扩展性)

| # | 问题 | 建议 | 工作量 |
|---|------|------|--------|
| 6 | Service 类仍然偏大 | 继续拆分超 300 行的 Service | 2d |
| 7 | 前端组件拆分不够 | 拆分大 View 为子组件 | 2d |
| 8 | 缺少全局状态 store | 添加 project/competition/notification store | 2d |
| 9 | 跨模块写操作仍用 JdbcTemplate | 扩大事件驱动范围 | 3d |
| 10 | API 版本管理缺失 | 从 `/api/v1/` 开始 | 1d |
| 11 | tags/problem_ids 用 JSON/VARCHAR 存储 | 改用关联表 | 2d |
| 12 | 判题轮询模式 | 改用回调或 Pub/Sub | 2d |

### 🟢 低优先级 (体验增强)

| # | 问题 | 建议 | 工作量 |
|---|------|------|--------|
| 13 | 缺少路由懒加载 Loading | 添加 NProgress | 0.5d |
| 14 | 缺少 PWA 支持 | 添加 Service Worker + Manifest | 2d |
| 15 | 缺少监控告警 | 添加 Prometheus + Grafana | 2d |
| 16 | 缺少日志收集 | 添加 ELK/Loki | 2d |
| 17 | 移动端体验优化 | 考虑 Vant UI 或精细化 CSS | 3d |
| 18 | 测试覆盖率 | 添加 vitest coverage + CI 检查 | 1d |

---

## 十、架构演进路线建议

```
当前状态 (v2.2)                    目标状态 (v3.0)
─────────────────                  ─────────────────
模块化单体                    →    模块化单体 + 事件驱动
JdbcTemplate 跨模块查询       →    Spring Event / 消息队列
轮询判题                      →    回调/Pub/Sub 判题
单机 Docker Compose           →    K8s / Docker Swarm
无监控                        →    Prometheus + Grafana
无 API 版本                   →    /api/v1/ + OpenAPI 3.0
纯 Web                        →    Web + PWA + 小程序(可选)
```

---

## 十一、总结

**这是一个完成度很高、工程质量优秀的项目。** 从一个学习项目的角度来看：

- ✅ 架构选型正确 (模块化单体 + Vue 3)
- ✅ 安全意识极强 (66 条踩坑 + 8 次审计)
- ✅ 文档体系完善 (前期设计 + 开发文档 + 踩坑记录)
- ✅ 代码质量良好 (分层清晰、异常处理完善、日志规范)
- ✅ DevOps 基础扎实 (Docker Compose + CI + Flyway)

**主要提升方向**:
1. 事件驱动解耦 (替代 JdbcTemplate 跨模块写)
2. 前端组件化和状态管理深化
3. 生产环境可观测性 (监控/日志/告警)
4. API 治理 (版本管理/幂等性/文档)

**作为个人/学习项目，这个仓库的质量已经超过了大多数同类开源项目。** 特别是踩坑记录和审计报告的详尽程度，本身就是极好的技术学习资料。
