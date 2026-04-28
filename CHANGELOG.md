# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/).

---

## v3.5.2 (2026-04-28) — 深度目录重组

### 📁 目录重构

- **docker/ 子目录拆分**: compose/ monitoring/ nginx/ 三个子目录，告别 12 文件平铺
  - `docker/compose/` — docker-compose.yml + docker-compose.prod.yml
  - `docker/monitoring/` — prometheus.yml + alert_rules.yml + grafana/ + loki + promtail
  - `docker/nginx/` — nginx.conf + nginx.prod.conf
- **文档合并**: DEPLOYMENT.md 更新到 v3.0，添加到 PRODUCTION_DEPLOYMENT.md 的交叉引用
- **过时文档归档**: DEEP_OPTIMIZATION_REPORT.md (v0.22.0) 移入 archive/legacy/
- **子目录 README**: docker/ + mobile/ + scripts/ 各添加 README.md 说明

### 🔧 路径修复

- docker-compose.yml / docker-compose.prod.yml: 更新 init.sql/nginx/Dockerfile 相对路径
- .github/workflows/deploy.yml: 更新 compose 路径
- README.md / CONTRIBUTING.md / DEPLOYMENT.md / PRODUCTION_DEPLOYMENT.md: 同步更新

---

## v3.5.1 (2026-04-28) — 文档整理与归档优化

### 📚 文档整理

- **归档目录重构**: 消除 `docs/archive/planning/archive/` 三层嵌套，合并到 `docs/archive/planning/`
- **审计报告归档**: 28 份审计报告移入 `reports/` 子目录，新增 `README.md` 总结 6 轮审计历程
- **过时文件清理**: `sprint6_tables.sql` 移入 `archive/legacy/`
- **版本信息统一**: TODO.md / PROGRESS.md / INDEX.md 统一更新到 v3.5.0
- **README.md 更新**: 核心功能表新增安全架构、质量报告表扩充、开发路线更新
- **docs/INDEX.md 重写**: 新增优化报告索引、归档目录说明、维护规则

---

## v3.5.0 (2026-04-28) — 深度架构优化 + 性能增强

### ⚡ 性能优化

- **FeedService 热度排序时间衰减**: 热度公式添加 `TIMESTAMPDIFF` 时间衰减因子，参考 Hacker News 算法，新内容获得初始曝光，优质内容持续排名靠前
- **Caffeine 本地缓存**: `UserQueryRepository.getUserBasicInfo()` 添加 `@Cacheable` 缓存，10 分钟过期，减少高频跨模块查询的数据库压力
- **CrossModuleQueryRepository 废弃标记**: `getProjectFeed()` 标记为 `@Deprecated`，消除与 FeedService 的重复代码

### 🔧 后端改进

- **JwtUtils.getRefreshTokenExpiryDate()**: 新增方法直接返回过期时间 Date，无需先生成 Token 再解析，简化 UserService 中的 Token 过期计算
- **UserService Token 过期计算重构**: register() 和 login() 中的复杂日期计算替换为 `LocalDateTime.ofInstant(jwtUtils.getRefreshTokenExpiryDate().toInstant(), ...)`
- **GlobalExceptionHandler 增强**: 添加 `HttpRequestMethodNotSupportedException` (405) 和 `HttpMediaTypeNotSupportedException` (415) 处理
- **@RateLimit 注解实现**: 定义 + 拦截器 + 限流器缓存，支持 Controller 方法自定义限流策略
- **PointService doAddPoints() 文档**: 添加竞态窗口安全性说明，明确 Redisson 锁保护下的安全保证

### 🔧 前端改进

- **ErrorBoundary 增强**: 全局错误捕获 (window.onerror + unhandledrejection)、重试次数限制 (3 次)、复制错误信息、用户友好提示
- **API 类型安全增强**: 消除 `unknown[]`/`any` 类型，新增 `PointRankItem` 和 `RankItem` 类型定义

### 📚 文档

- **CHANGELOG.md**: v3.5.0 版本记录

---

## v3.4.0 (2026-04-28) — 全面架构优化 + 安全加固

### 🔒 安全改进

- **API 版本重定向路径遍历防护**: `ApiVersionRedirectConfig` 新增 URI `..` 检查，拒绝可疑的路径遍历请求，返回 400 Bad Request
- **SB3 上传 ZIP 路径遍历增强**: `SB3Unzipper` 在原有检查基础上增加 `Paths.get().normalize()` 规范化，防止编码绕过（如 `..%2F`）

### ⚡ 性能优化

- **V19 数据库索引迁移**: 添加 4 个缺失的复合索引
  - `idx_point_user_type_date (user_id, type, created_at)` — 优化今日积分查询
  - `idx_point_user_date (user_id, created_at)` — 优化积分时间线
  - `idx_notification_user_time (user_id, created_at DESC)` — 优化通知列表
  - `idx_project_user_status_time (user_id, status, created_at DESC)` — 优化用户项目列表

### 🔧 后端改进

- **CrossModuleQueryRepository 模块化拆分**: 新增 `ProjectQueryRepository` 和 `UserQueryRepository`，按模块拆分跨模块查询，原 `CrossModuleQueryRepository` 保留向后兼容
- **异步判题指数退避重试**: `JudgeService.judgeAsync()` 重试间隔从线性递增 (1s, 2s, 3s) 改为指数退避 (1s, 2s, 4s)，使用 `CompletableFuture.delayedExecutor()` 替代 `Thread.sleep`
- **SseTokenService 清理机制完善**: 内存清理器在构造函数中无条件启动，覆盖 Redis 运行时断连的边界情况

### 🔧 前端改进

- **Token 刷新竞态条件修复**: 引入 `isRetryingPending` 标志，防止 `refreshPromise.finally()` 过早清除共享状态。将 `retryPendingRequests()` 移入 `doRefreshToken()` 内部，确保重试完成后再清除
- **路由进度条 Vue 组件化**: 新增 `RouteLoadingBar.vue` 组件 + `useRouteLoading()` composable，替代原 `document.getElementById` DOM 操作方案
- **API 层类型安全改进**: `post<T>` / `put<T>` 的 `data` 参数类型从 `Record<string, unknown>` 改为 `unknown`，支持更灵活的类型推断

### 📚 文档

- **FULL_OPTIMIZATION_REPORT.md**: 全面优化分析报告（后端/前端/数据库/安全/测试）
- **SECOND_OPTIMIZATION_AUDIT.md**: 二次审计报告（修改清单 + 风险评估 + 兼容性检查）
- **PITFALLS.md**: 新增坑 97-101（Token 刷新竞态/路由进度条/路径遍历/SseToken 清理/索引缺失），经验总结 126 条

---

## v3.3.0 (2026-04-28) — 二次深度优化 + 安全加固

### 🔒 安全改进

- **Role 枚举统一管理**: 新增 `Role` 枚举类 (`scratch-common-core`)，统一角色定义，消除散落在各处的字符串 typo 风险。`AuthInterceptor` 角色校验改用 `Role.fromString()` 安全解析，不区分大小写，无法匹配时默认 STUDENT
- **Redis 固定窗口限流竞态修复**: `RedisRateLimiter.isAllowed()` 原来使用 `INCR` + `EXPIRE` 两步操作，存在进程崩溃导致 key 永不过期的风险。改为 Lua 脚本保证原子性
- **关注接口竞态修复**: `UserService.follow()` 的 check-then-insert 改为 `INSERT IGNORE`，与点赞接口保持一致，并发安全
- **ScratchBridge postMessage 安全加固**: `postMessage` 的 targetOrigin 从通配符 `'*'` 改为 iframe 实际 origin，防止恶意页面接收消息
- **沙箱 seccomp 配置修复**: 移除 `seccomp` 在允许列表中的冲突项（同时出现在 allowed 和 denied 列表）
- **分页大小上限限制**: 7 个 Controller 的分页参数统一添加 `@Min(1) @Max(100)` 注解，防止恶意大分页查询

### ⚡ 性能优化

- **OpenAI Provider 可用性缓存**: `isAvailable()` 方法增加 5 分钟 TTL 缓存，避免每次调用都发真实 API 请求消耗 token 和网络

### 🔧 前端改进

- **WebSocket 协作编辑自动重连**: `useCollabWebSocket` 增加指数退避重连机制（初始 1 秒，最大 30 秒，最多 10 次），添加 ±20% 随机抖动避免重连风暴。支持 `reconnectAttempt` 状态暴露给 UI
- **日志工具统一**: WebSocket composable 中的 `console.log/warn/error` 替换为 `createLogger('Collab')`，生产环境自动禁用调试日志

### 📦 数据库

- **Flyway 迁移安全化**: V17 迁移文件的 `CREATE INDEX` 语句全部添加 `IF NOT EXISTS`，防止重复执行报错

### 📊 二次审计结果

| 检查项 | 结果 |
|--------|------|
| 敏感信息泄露 | ✅ 无泄露 |
| SQL 注入风险 | ✅ 全部参数化 |
| @Transactional 覆盖 | ✅ 写操作全覆盖 |
| 前端类型安全 | ✅ 仅 3 处合理 `any` |
| console 输出 | ✅ 统一使用 logger |
| Flyway 安全性 | ✅ 新迁移全部 IF NOT EXISTS |
| 分页大小限制 | ✅ 全部 Controller @Max(100) |
| 并发竞态 | ✅ 关注/点赞均 INSERT IGNORE |
| postMessage 安全 | ✅ 指定 targetOrigin |
| seccomp 配置 | ✅ 无冲突 |

---

## v3.2.1 (2026-04-28) — CI 修复 + init.sql 同步 + 代码清理

### 🐛 Bug 修复

- **Dockerfile.backend CI 阻断修复**: `Dockerfile.backend` 引用了已不存在的 `scratch-common/pom.xml`（模块已拆分为 4 个子模块），导致 Docker Build Verify 步骤失败。更新为引用 `scratch-common-core`、`scratch-common-redis`、`scratch-common-security`、`scratch-common-audit` 的 pom.xml
- **docker/init.sql 同步 Flyway 迁移**: 补充 V8-V18 迁移中遗漏的表和字段：
  - `user` 表: 添加 `last_login_at`、`login_count`、`oauth_source`、`refresh_token`、`refresh_token_expires_at` 字段
  - 新增 `user_oauth` 表（第三方登录绑定，V15）
  - 新增 `collab_session`、`collab_participant` 表（协作编辑，V16）
  - 补充 20+ 条性能索引（V9/V13/V17/V18）

### 🔧 改进

- **前端 Token 刷新清理**: 移除 `request.ts` 中未使用的 `isRefreshing` 死代码变量

### 📊 验证

- 后端测试: 7/7 通过
- 前端测试: 14 文件 166/166 通过
- TypeScript 类型检查: 通过
- 前端构建: 通过

---

## v3.1.0 (2026-04-28)

### 🏗️ 基础设施

- **E2E 测试框架**: Playwright 配置 + 4 个核心流程测试文件
  - `homepage.spec.ts` — 首页加载和重定向
  - `auth.spec.ts` — 登录/注册/退出流程
  - `navigation.spec.ts` — 路由切换和导航
  - `project.spec.ts` — 项目浏览和详情
- **性能压测脚本**: k6 脚本 (`scripts/performance-test.js`)
  - 健康检查/Feed/排行榜/登录 4 个场景
  - 阶梯式加压，P95 < 500ms 阈值
- **生产部署配置**:
  - `docker/docker-compose.prod.yml` — 完整生产环境编排
  - `docker/nginx.prod.conf` — Nginx 生产配置 (HTTPS/CSP/SSE)
- **监控仪表板**:
  - Prometheus + Grafana 集成
  - 6 面板 Grafana 仪表板 (QPS/延迟/内存/状态码/线程/服务状态)
  - Prometheus 告警规则 (宕机/高错误率/高延迟/JVM内存/判题异常)
- **健康检查增强**: 返回 JVM 版本/OS 信息/内存使用详情

### 🔧 改进

- HealthController 返回更丰富的服务信息
- Nginx 配置增加 Content-Security-Policy 安全头
- Nginx SSE 端点使用正则匹配 (更精确)

---

## [v3.1.0] - 2026-04-28

### Changed
- **scratch-common 模块拆分**: 将 `scratch-common` 大而全的公共模块拆分为 4 个细粒度模块：
  - `scratch-common-core` — 返回体/异常/事件/工具/配置 (26 个类)
  - `scratch-common-redis` — 分布式锁/限流/缓存/调度锁 (6 个类)
  - `scratch-common-security` — JWT/认证/角色/幂等性 (9 个类)
  - `scratch-common-audit` — 敏感词过滤 (1 个类)
- **PageResult 重构**: 去掉 MyBatis-Plus IPage 依赖，改为纯 POJO `PageResult.of()` 方法
- **依赖链优化**: security → redis → core，业务模块按需依赖，传递依赖更清晰
- **Docker 网络隔离**: 沙箱容器禁止外网访问，仅允许内部服务通信
- **CORS 配置优化**: 支持通过环境变量配置生产域名，开发/生产环境分离
- **Prometheus 监控**: 添加 micrometer-registry-prometheus，暴露 /actuator/prometheus 端点
- **自定义业务指标**: 添加用户注册/登录、项目创建/发布、判题提交等计数器
- **自定义健康检查**: 添加数据库连接和磁盘空间健康检查
- **文档同步更新**: README、ADR、MODULE_DEV_GUIDE、FULL_AUDIT_REPORT 等文档反映新模块结构

### Added
- **Nginx 生产配置**: 添加 HTTPS、反向代理、SSE、Gzip、安全头等配置
- **数据库备份脚本**: 自动备份、压缩、验证、清理旧备份
- **Prometheus 告警规则**: 服务宕机、高错误率、高延迟、JVM 内存等告警
- **生产部署指南**: 完整的部署流程、配置说明、监控告警、备份恢复文档

### Fixed
- **CI 配置更新**: `.github/workflows/ci.yml` 的 `-pl` 参数更新为 4 个新模块名
- **ZIP 路径遍历防护**: SB3Unzipper 添加 Zip Slip 攻击检测，拒绝包含 `../` 的路径

---

## [v3.0.0] - 2026-04-26

### Fixed
- **集成测试 Redis 依赖**: 创建 `TestRedisMockConfig` Mock Redis 组件，集成测试可在无 Redis 环境运行
- **CI 测试覆盖**: 将 `scratch-app` 集成测试加入 CI 流水线

### Changed
- **ScratchEditorView i18n 全覆盖**: 编辑器页面所有硬编码中文替换为 i18n 调用，新增 35 个翻译键
- **远程分支清理**: 删除已合并的 `feature/optimization-v3` 和 `optimize/v2.0` 分支

### Added
- `TestRedisMockConfig.java` — 集成测试 Redis Mock 配置
- `docs/SPRINT30_ANALYSIS.md` — Sprint 30 深度分析报告
- 编辑器 i18n 翻译键 35 个（中英双语）

---

## [v2.9.0] - 2026-04-26

### Fixed
- **ProjectService 作者信息 + 点赞状态**: 项目详情页现在正确显示作者昵称/头像，以及当前用户的点赞状态（通过 CrossModuleQueryRepository 跨模块查询）
- **Token Refresh 竞态条件**: 前端请求层用 Promise 单例锁替代旧的订阅队列模式，9997/401 处理逻辑统一，加 10s 超时防止挂起

### Changed
- **限流器升级为滑动窗口计数器**: 从 Fixed Window 升级为 Sliding Window Counter（10 子窗口），消除窗口边界 2x 突发流量问题
- **App.vue 组件拆分**: 提取 AppHeader / MobileNav / AuthDialog 三个独立组件，App.vue 从 ~16KB 瘦身至 ~11.5KB
- **keep-alive 改用路由 meta 控制**: 7 个高频页面路由添加 `keepAlive: true`，App.vue 动态读取路由 meta 构建缓存列表

### Added
- `AppHeader.vue` — 顶部导航组件
- `MobileNav.vue` — 移动端底部导航组件
- `AuthDialog.vue` — 登录/注册弹窗组件
- `docs/OPTIMIZATION_V3.md` — v2.9.0 深度优化报告

---

## [v2.8.1] - 2026-04-25

### Changed
- **开发计划全面重写**: `docs/DEV_PLAN.md` 从 Sprint 4 更新到 Sprint 30，反映当前项目状态
- **README 路线图更新**: 合并 Phase 1-8 为简洁的已完成列表，新增 Phase 9 当前阶段和 Phase 10-12 未来规划
- **TODO.md Sprint 30 更新**: 基于深度分析的优先级排序（P0 功能补全 / P1 安全闭环 / P2 质量保障）
- **README 测试章节更新**: 补充测试命令、集成测试说明、测试覆盖统计
- **文档索引创建**: `docs/README.md` — 35+ 文档的分类导航

### Fixed
- 修复 DEV_PLAN.md 严重过时问题（仍在描述 Sprint 4-5 计划）

---

## [v2.8.0] - 2026-04-25

### Fixed
- **CI 测试修复**: `SocialServiceTest` 4 个测试从验证降级路径改为验证事件发布调用
- **CI 编译修复**: `HomeworkServiceTest` grade() 调用从 `HomeworkService` 改为 `HomeworkGradingService`
- CI 流水线全部通过（Backend + Frontend + Sandbox + Code Quality）

### Added
- **深度分析报告**: `docs/COMPREHENSIVE_ANALYSIS.md` — 全栈架构、CI/CD、安全、数据库、API 分析
- **踩坑记录**: 新增坑 82-84（CI 测试相关），经验总结扩展至 108 条
- README 添加深度分析报告链接

### Changed
- 踩坑记录分类新增「CI/测试」类别

---

## [v2.7.0] - 2026-04-25

### Changed
- **统一 JSON 库**：移除 fastjson2 依赖，scratch-sb3 模块全部迁移至 Jackson（`ObjectMapper` + `JsonNode`）
- **前端 Token 存储**：从 `localStorage` 改为 `sessionStorage`，关闭标签页自动清除，降低 XSS 窃取风险
- **ErrorBoundary 增强**：App.vue 全局包裹，新增错误上报预留接口（可接入 Sentry）
- **init.sql 同步**：与 Flyway V1-V14 迁移完全对齐，包含全部 20 张表 + 所有字段

### Added
- **乐观锁**：`Project`、`User`、`Homework`、`CompetitionRanking` Entity 添加 `@Version` 字段（V14 迁移）
- **API 限流响应头**：`X-RateLimit-Limit`、`X-RateLimit-Remaining`、`Retry-After` 标准 HTTP 头
- **FileConstants 常量类**：统一定义 SB3_MAX_SIZE(100MB)、AVATAR_MAX_SIZE(5MB)、ZIP_ENTRY_MAX_SIZE(50MB) 等限制
- **踩坑记录**：新增坑 77-81（fastjson2 混用、乐观锁缺失、文件限制分散、init.sql 不同步、localStorage 安全）
- **经验总结**：累计 81 条坑 + 100 条经验

### Fixed
- `CompetitionRankingService` 全量逐条 UPDATE 改为 CASE WHEN 批量更新
- `CompetitionService.reorderRankings()` 竞态条件修复
- `SB3Unzipper` 引用 `FileConstants.ZIP_ENTRY_MAX_SIZE` 统一限制

### Removed
- fastjson2 依赖（`com.alibaba.fastjson2:fastjson2:2.0.47`）从父 pom.xml 和 scratch-sb3/pom.xml 中移除

---

## [v2.6.0] - 2026-04-25

### Added
- JSON 字段改为关联表（`homework_problem`、`competition_problem`）
- `submission` 表添加 `competition_id` 字段
- Caffeine 本地缓存（1000 条 / 10 分钟过期）
- ShedLock 分布式调度锁（多实例部署保护）
- 前端 API 层按模块拆分（14 个独立文件）
- ER 图 + ADR + 优化报告
- 6 个性能索引（V13 迁移）

---

## [v2.5.0] - 2026-04-25

### Added
- Scratch 在线编辑器集成（TurboWarp iframe + sb3 导入导出）
- PWA 基础支持（Service Worker + Manifest）
- 数据看板（管理后台可视化指标 + 关键比率 + 快速导航）
- 全文搜索（SearchView + 热门标签 + 分页结果）

---

## [v2.3.0] - 2026-04-24

### Added
- SSE 一次性 Token 机制（防止 JWT URL 泄露）
- 接口幂等性保护（`@Idempotent` + Redis SET NX）
- API 版本管理（`/api/v1/`）
- 冗余计数字段每日校准
- Spring Event 事件驱动解耦跨模块写操作
- Service 拆分（HomeworkGradingService, AdminDashboardService）
- 判题沙箱并发控制
- 前端全局状态扩展（notification store, project store）
- email 数据库唯一约束

---

## [v2.0.0] - 2026-04-23

### Added
- 判题沙箱进程隔离（子进程 + 内存限制）
- DFA 敏感词自动机（O(n) 匹配）
- Redisson 分布式锁（积分并发安全）
- 接口限流（滑动窗口 IP 级）
- 前端 keep-alive + useLoading Composable
- CI/CD 增强（前端构建 + 安全扫描）
- Docker 资源限制 + 健康检查

---

## [v1.0.0] - 2026-04-22

### Added
- MVP 核心闭环（Sprint 1-14）
- 用户系统、创作引擎、社区互动、判题系统、教学管理、竞赛系统、积分体系、系统管理
- Vue 3 前端 + Spring Boot 3.2 后端 + Node.js 判题沙箱
- Docker Compose 一键部署
