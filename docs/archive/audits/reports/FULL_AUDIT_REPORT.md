# 🔍 Scratch Community Platform — 全面深度审计报告

> **审计日期**: 2026-04-24
> **审计范围**: 后端全模块 + 前端 Vue 3 + 沙箱 + Docker + CI/CD + 数据库
> **审计视角**: 高级 Java/Spring Boot 架构师

---

## 目录

1. [架构设计分析](#a-架构设计分析)
2. [数据库设计分析](#b-数据库设计分析)
3. [API 接口分析](#c-api-接口分析)
4. [安全分析](#d-安全分析)
5. [代码质量分析](#e-代码质量分析)
6. [性能分析](#f-性能分析)
7. [前端架构分析](#g-前端架构分析)
8. [DevOps 分析](#h-devops-分析)
9. [优化建议汇总（按优先级排序）](#优化建议汇总)
10. [整体评分](#整体评分)

---

## A. 架构设计分析

### A1. 模块划分与依赖关系

**模块清单** (14 个 Maven 模块):

| 模块 | 职责 | 依赖 |
|------|------|------|
| `scratch-app` | 启动模块，聚合所有模块 | 所有业务模块 |
| `scratch-common-core` | 公共核心层 (返回体/异常/事件/工具/配置) | 无业务模块依赖 |
| `scratch-common-redis` | 公共 Redis 层 (分布式锁/限流/缓存/调度锁) | common-core |
| `scratch-common-security` | 公共安全层 (JWT/认证/角色/幂等性) | common-redis |
| `scratch-common-audit` | 公共审核层 (敏感词过滤) | common-core |
| `scratch-user` | 用户/班级/积分 | common-security |
| `scratch-editor` | 项目创作引擎 | common-security, sb3 |
| `scratch-judge` | 判题/竞赛 | common-security |
| `scratch-social` | 社区(点赞/评论/Feed/AI点评) | common-security, common-audit |
| `scratch-classroom` | 教室/作业/学情分析 | common-security |
| `scratch-system` | 系统管理/通知/审核/配置 | common-security, common-audit |
| `scratch-sb3` | SB3 文件解析库 | common-core |
| `scratch-judge-core` | 判题核心(预留) | — |

**评价**:
- 🟢 模块划分合理，按业务域划分，职责清晰
- 🟢 `scratch-sb3` 作为纯 Java 库独立，不依赖 Spring，设计优秀
- 🟢 公共层拆分为 4 个细粒度模块（core/redis/security/audit），职责更清晰
- 🟢 依赖方向正确：security → redis → core，业务模块按需依赖
- 🟡 `scratch-judge-core` 模块存在但似乎未实际使用，`JudgeService` 直接在 `scratch-judge` 中实现
- 🟡 跨模块查询（如 `SocialService` 查 `project` 表）通过 `JdbcTemplate` 实现，避免了模块耦合但牺牲了类型安全

### A2. 分层规范

**分层架构**: Controller → Service → Mapper

| 模块 | Controller | Service | Mapper | Entity | DTO/VO |
|------|-----------|---------|--------|--------|--------|
| user | 3 | 4 | 5 | 5 | 6/4 |
| editor | 1 | 1 | 1 | 1 | 2/2 |
| judge | 3 | 3 | 5 | 5 | 3/5 |
| social | 2 | 4 | 3 | 3 | 1/4 |
| classroom | 2 | 2 | 2 | 2 | 3/3 |
| system | 1 | 3 | 3 | 3 | 2/3 |

**评价**:
- 🟢 分层清晰，Controller 只做参数接收和调用 Service
- 🟢 Service 层正确使用 `@Transactional`
- 🟢 DTO/VO 分离规范，DTO 接收入参，VO 返回出参
- 🟢 使用 `@Valid` + Jakarta Validation 进行参数校验
- 🟡 部分 Service 通过 `JdbcTemplate` 直接写 SQL，绕过了 MyBatis-Plus 的类型安全

### A3. 循环依赖检查

- 🟢 无循环依赖。模块间依赖方向单一：`scratch-app` → 业务模块 → `scratch-common`
- 🟢 跨模块数据访问通过 `JdbcTemplate` 或 Spring Event 解耦

### A4. 公共层职责评估

公共层已拆分为 4 个细粒度模块：

**scratch-common-core** (26 个类):
- `result/` — 统一返回体 (R, ErrorCode, PageResult)
- `exception/` — 异常处理 (BizException, GlobalExceptionHandler)
- `event/` — 积分事件 (PointEvent, ProjectCommentEvent, ProjectLikeEvent, ProjectViewEvent, EventPublisherHelper)
- `util/` — 工具类 (FileConstants, FileUploadUtils, RedisUtils, ApiVersion)
- `repository/` — 跨模块仓库 (CrossModuleQueryRepository, CrossModuleWriteRepository)
- `config/` — 全局配置 (AsyncConfig, JacksonXssConfig, XssFilterConfig, OpenApiConfig, RequestLoggingConfig, ApiVersionRedirectConfig, MybatisPlusConfig, MybatisMetaObjectHandler, MinioConfig, RateLimitConfig)

**scratch-common-redis** (6 个类):
- `ratelimit/` — Redis 分布式限流 (RedisRateLimiter, RedisRateLimitInterceptor, RedisRateLimitConfig)
- `config/` — Redis 配置 (RedissonConfig, CacheConfig, ShedLockConfig)

**scratch-common-security** (9 个类):
- `auth/` — JWT + 拦截器 + 注解 (JwtUtils, AuthInterceptor, LoginUser, RequireRole, SseTokenService, TokenBlacklistService)
- `idempotent/` — 幂等性 (Idempotent, IdempotentInterceptor)
- `config/` — Web MVC 配置 (WebMvcConfig)

**scratch-common-audit** (1 个类):
- `audit/` — 敏感词过滤 (SensitiveWordFilter)

**评价**:
- 🟢 职责范围合理，按关注点分离为 4 个模块
- 🟢 `PointEvent` 放在 common-core 中是正确的，因为多个模块都需要发布积分事件
- 🟢 模块依赖链清晰：security → redis → core，audit → core
- 🟡 `SensitiveWordFilter` 的敏感词硬编码在代码中，应从数据库/配置文件加载

---

## B. 数据库设计分析

### B1. 表结构设计

**共 18 张表**:

| 模块 | 表名 | 说明 |
|------|------|------|
| user | user, user_follow, class, class_student | 用户/关注/班级 |
| editor | project | 项目 |
| social | project_like, project_comment, ai_review | 点赞/评论/AI点评 |
| judge | problem, submission, competition, competition_registration, competition_ranking | 题目/提交/竞赛 |
| classroom | homework, homework_submission | 作业 |
| system | notification, content_audit_log, system_config | 通知/审核/配置 |
| user(V3) | point_log | 积分记录 |

**评价**:
- 🟢 表结构设计合理，字段命名规范 (snake_case)
- 🟢 每张表都有 `id`, `created_at`, `updated_at`, `deleted` 基础字段
- 🟢 使用 `BIGINT` 作为主键类型，支持大规模数据
- 🟢 合理使用 `JSON` 类型存储灵活数据 (options, problem_ids, parse_result 等)
- 🟢 索引策略合理：唯一索引 (uk_)、普通索引 (idx_)、全文索引 (ft_search)

### B2. 字段类型选择

| 字段 | 类型 | 评价 |
|------|------|------|
| `user.password` | VARCHAR(255) | 🟢 BCrypt 哈希长度 60 字符，255 足够 |
| `user.role` | VARCHAR(20) | 🟡 可用 ENUM 但 VARCHAR 更灵活 |
| `project.status` | VARCHAR(20) | 🟡 同上，应用层保证值域 |
| `project.description` | TEXT | 🟢 长文本合适 |
| `submission.runtime_ms` | BIGINT | 🟢 毫秒级时间合适 |
| `notification.data` | JSON | 🟢 灵活存储附加数据 |
| `competition.problem_ids` | JSON | 🟢 存储题目 ID 列表合理 |

### B3. 外键/约束

- 🟡 **无物理外键** — 设计决策（应用层保证一致性），这是常见的微服务/高并发场景做法
- 🟢 唯一约束完善：`uk_username`, `uk_follow`, `uk_invite_code`, `uk_user_project`, `uk_homework_student`, `uk_competition_user`
- 🟡 `user_follow` 表缺少防止自关注的 CHECK 约束（应用层已校验）
- 🟡 `submission` 表的 `user_id` + `problem_id` + `verdict='PENDING'` 组合无唯一约束（依赖应用层去重）

### B4. 冗余/缺失字段

**冗余字段**:
- 🟢 `project.like_count`, `comment_count`, `view_count` — 计数器冗余是合理的性能优化
- 🟢 `homework.submit_count`, `graded_count` — 同上

**缺失字段**:
- 🔴 `user` 表缺少 `email` 或 `phone` 字段 — 无法实现密码找回、邮箱验证等功能
- 🟡 `project` 表缺少 `version` 字段 — 无法实现乐观锁/版本控制
- 🟡 `submission` 表缺少 `ip_address` 字段 — 无法做提交来源分析和反作弊
- 🟡 `notification` 表缺少 `title` 字段的多语言支持

### B5. Flyway 迁移脚本质量

| 版本 | 内容 | 评价 |
|------|------|------|
| V1 | 初始 Schema (13 张表) | 🟢 结构清晰，注释完整 |
| V2 | 初始数据 (管理员+配置) | 🟢 简洁有效 |
| V3 | 积分+Remix+学情 | 🟡 UPDATE 子查询复杂，首次部署可能慢 |
| V4 | AI 点评表 | 🟢 简单直接 |
| V5 | 竞赛表 (3 张) | 🟢 设计合理 |

**问题**:
- 🟡 `docker/init.sql` 与 Flyway 迁移脚本重复 — init.sql 应仅用于 Docker 首次初始化，Flyway 用于版本管理
- 🟢 `baseline-on-migrate: true` + `baseline-version: 0` 配置正确，支持已有数据库迁移

---

## C. API 接口分析

### C1. RESTful 规范程度

**URL 设计评价**:

| 接口 | 风格 | 评价 |
|------|------|------|
| `POST /api/user/register` | 🟢 | 注册是动作，POST 合理 |
| `GET /api/user/me` | 🟢 | 获取当前用户信息 |
| `PUT /api/user/me` | 🟢 | 更新当前用户信息 |
| `POST /api/project` | 🟢 | 创建项目 |
| `GET /api/project/{id}` | 🟢 | 获取项目详情 |
| `POST /api/project/{id}/publish` | 🟢 | 发布是动作 |
| `POST /api/project/{id}/remix` | 🟢 | Remix 是动作 |
| `POST /api/judge/submit` | 🟢 | 提交判题 |
| `GET /api/social/feed` | 🟢 | 获取 Feed 流 |
| `POST /api/social/comment` | 🟡 | 应为 `POST /api/project/{id}/comment` 更 RESTful |
| `GET /api/points/me` | 🟢 | 获取积分信息 |
| `POST /api/points/checkin` | 🟢 | 签到是动作 |

**总体**: 🟢 接口设计基本符合 RESTful 规范，少数可以优化。

### C2. 参数校验

- 🟢 使用 Jakarta Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`, `@NotNull`)
- 🟢 `RegisterDTO` 有密码强度校验 (`@Pattern` 要求字母+数字)
- 🟢 `ProblemController` 使用 `@Min(1)` `@Max(100)` 校验分页参数
- 🟢 分页 size 有上限限制 (`Math.min(size, 100)`)
- 🟡 `CreateHomeworkDTO.type` 缺少 `@Pattern` 校验合法值
- 🟡 `CreateCompetitionDTO.type` 缺少 `@Pattern` 校验

### C3. 响应格式统一性

- 🟢 全局统一返回体 `R<T>`: `{code, msg, data}`
- 🟢 成功: `code=0`, 失败: `code=错误码`
- 🟢 分页返回使用 `PageResult<T>`
- 🟢 全局异常处理器 `GlobalExceptionHandler` 统一处理

### C4. 错误处理一致性

- 🟢 `GlobalExceptionHandler` 处理 `BizException`, `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException`, `Exception`
- 🟢 `ErrorCode` 枚举覆盖所有模块的错误码
- 🟢 错误码分段清晰: 10000-19999(用户), 20000-29999(创作), 30000-39999(社区), 40000-49999(判题), 50000-59999(教室), 60000-69999(系统)
- 🟡 部分错误使用硬编码数字而非 ErrorCode 枚举 (如 `throw new BizException(20003, "...")`)

### C5. 接口文档完整性

- 🟢 集成 SpringDoc (Swagger UI)，自动生成 API 文档
- 🟢 Controller 使用 `@Tag`, `@Operation` 注解
- 🟢 生产环境可通过配置禁用 Swagger
- 🟡 部分 Controller 方法缺少 `@Operation` 注解 (如 `ProjectController` 的部分方法)
- 🟡 缺少请求/响应示例 (`@Schema(example = "...")`)

---

## D. 安全分析

### D1. JWT 实现

**实现位置**: `JwtUtils.java`

- 🟢 使用 `io.jsonwebtoken` (JJWT) 库，HS256 签名
- 🟢 Token 包含 userId, username, role
- 🟢 默认过期时间 24 小时
- 🟢 使用 `Keys.hmacShaKeyFor()` 生成密钥
- 🟡 密钥通过配置文件提供，有默认值 — 生产环境必须通过环境变量覆盖
- 🔴 **无 Token 刷新机制** — Token 过期后用户必须重新登录
- 🔴 **无 Token 黑名单** — 无法实现登出/Token 失效

### D2. 权限控制

- 🟢 `@RequireRole` 注解实现角色校验
- 🟢 `AuthInterceptor` 拦截器处理认证和角色校验
- 🟢 `WebMvcConfig` 配置公开接口白名单
- 🟢 角色体系: STUDENT / TEACHER / ADMIN
- 🟡 **无接口级权限矩阵文档** — 不清楚哪些接口需要什么角色
- 🟡 `AdminController` 的 `@RequireRole("ADMIN")` 正确，但缺少防止 ADMIN 禁用自己的逻辑（实际代码有，OK）

### D3. SQL 注入/XSS 防护

**SQL 注入**:
- 🟢 MyBatis-Plus 的 `LambdaQueryWrapper` 自动参数化
- 🟢 `@Select` 注解中的 `#{}` 参数化
- 🟢 `FeedService` 对排序参数做白名单校验
- 🟢 `FeedService` 的 IN 子句使用 `?` 占位符参数化
- 🟡 `AdminService.getDashboard()` 使用 `JdbcTemplate.queryForObject()` 直接拼接 SQL — 但参数来自 `LocalDateTime` 对象，安全
- 🟡 `CompetitionService.updateRankings()` 使用 `placeholders()` 方法拼接 IN 子句 — 参数为 Long 类型，但模式不理想

**XSS**:
- 🟡 未见全局 XSS 过滤器
- 🟡 前端使用 Vue 3 (默认转义 HTML)，基本安全
- 🟡 后端返回的用户输入 (nickname, bio, comment) 未做 HTML 转义

### D4. 敏感数据处理

- 🟢 密码使用 BCrypt 加密存储
- 🟢 `UserVO` 不包含 password 字段（脱敏）
- 🟢 JWT Token 通过 `Authorization: Bearer` Header 传输
- 🟡 `LoginDTO.password` 通过明文 HTTP 传输 — **必须使用 HTTPS**
- 🟡 `system_config` 表可能存储敏感配置，API 返回时未做脱敏

### D5. 限流实现

**位置**: `RateLimitConfig.java`

- 🟢 基于滑动窗口的 IP 限流
- 🟢 不同接口不同限流策略: 全局 60/min, 登录 10/min, 判题 30/min, AI点评 20/min
- 🟡 **基于内存实现** — 多实例部署时限流不生效（注释中已标注）
- 🟡 **无 IP 伪造防护** — `X-Forwarded-For` 头可被伪造
- 🟡 限流器 `ConcurrentHashMap` 无过期清理机制 — 长期运行可能导致内存泄漏

---

## E. 代码质量分析

### E1. 命名规范

- 🟢 包名、类名、方法名严格遵循 Java 命名规范
- 🟢 Entity 与数据库表名对应 (user → User, project_like → ProjectLike)
- 🟢 DTO/VO 命名规范: `CreateProjectDTO`, `ProjectDetailVO`
- 🟢 常量使用 `UPPER_SNAKE_CASE`
- 🟢 数据库字段 `snake_case`，Java 字段 `camelCase`，MyBatis-Plus 自动映射

### E2. 异常处理

- 🟢 全局异常处理器 `GlobalExceptionHandler`
- 🟢 业务异常 `BizException` 携带错误码
- 🟢 各 Service 正确抛出业务异常
- 🟡 部分 `catch` 块只打日志不抛异常 (如 `HomeworkService.toVO` 中查询班级名称失败)
- 🟡 `RankService.refreshRankings()` 的 catch 块只打日志，排行榜刷新失败无告警

### E3. 日志记录

- 🟢 使用 Lombok `@Slf4j` 统一日志
- 🟢 关键操作有日志: 用户登录、积分变动、文件上传、判题结果
- 🟢 生产环境日志级别配置合理 (info/warn)
- 🟢 日志文件滚动策略配置 (100MB/文件, 30天保留, 1GB 总量)
- 🟡 缺少请求链路追踪 (Trace ID)
- 🟡 缺少结构化日志 (JSON 格式)

### E4. 代码重复

- 🟡 `toVO()` 方法在多个 Service 中重复实现 (UserService, AdminService, ClassService, ProjectService 等)
- 🟡 `getAndCheck()` / `getAndCheckOwner()` 模式在多个 Service 中重复
- 🟡 `Page` → `PageResult` 转换代码重复
- 🟡 可考虑抽取 `BaseService` 或使用 MapStruct

### E5. 单元测试覆盖

| 模块 | 测试文件 | 用例数 | 覆盖范围 |
|------|---------|--------|---------|
| user | UserServiceTest | ~10 | 注册/登录/关注 |
| judge | JudgeServiceTest | ~8 | 即时判题/异步判题 |
| social | SocialServiceTest | 10 | 点赞/评论/权限 |
| classroom | HomeworkServiceTest | 12 | 创建/提交/批改 |
| sb3 | SB3ParserTest | ~8 | 解析/异常 |
| **总计** | **5 个测试类** | **~48** | — |

**评价**:
- 🟡 核心 Service 有基本测试，但覆盖率偏低
- 🔴 **无集成测试** — 缺少 API 层集成测试
- 🔴 **无 PointService 测试** — 积分逻辑复杂但无测试
- 🔴 **无 CompetitionService 测试** — 竞赛排名逻辑复杂但无测试
- 🟡 **无前端测试** — Vue 3 有 26 个 vitest 用例，但覆盖有限

---

## F. 性能分析

### F1. 数据库查询优化

- 🟢 `CompetitionService` 已做 N+1 优化 (双策略: 增量+全量)
- 🟢 `RankService.getRank()` 批量查询用户信息，消除 N+1
- 🟢 `FeedService` 批量查询点赞状态
- 🟢 `HomeworkService.toVOPage()` 批量查询班级名称
- 🟢 全文索引 `ft_search` 支持 ngram 分词
- 🟡 `AnalyticsService.getHomeworkStats()` 在循环中执行 `AVG` 查询 — 应改为单次聚合查询
- 🟡 `PointService.getPointRanking()` 使用子查询计算总积分 — 应预计算或缓存
- 🟡 `AdminService.getDashboard()` 执行 10 次独立 COUNT 查询 — 可合并

### F2. 缓存策略

- 🟢 Redis 用于排行榜 (Sorted Set) + 任务队列
- 🟢 Redis 内存限制 256MB + LRU 淘汰策略
- 🟡 **无业务数据缓存** — 用户信息、项目详情等热数据未缓存
- 🟡 **无查询结果缓存** — 每次请求都查数据库
- 🟡 排行榜每小时全量刷新 — 可改为增量更新

### F3. 异步处理

- 🟢 `@EnableAsync` 启用异步支持
- 🟢 `judgeExecutor` 线程池: core=4, max=16, queue=100
- 🟢 积分事件 `PointEventListener` 使用 `@Async("judgeExecutor")` 异步处理
- 🟢 判题 `JudgeService.judgeAsync()` 使用 `@Async` 非阻塞
- 🟡 线程池拒绝策略直接抛异常 — 可考虑降级或排队

### F4. 连接池配置

**MySQL (HikariCP)**:
```yaml
minimum-idle: 5
maximum-pool-size: 20
idle-timeout: 30000
max-lifetime: 1800000
```
- 🟢 配置合理，适合中等并发场景

**Redis (Lettuce)**:
```yaml
max-active: 20
max-idle: 10
min-idle: 5
```
- 🟢 配置合理

---

## G. 前端架构分析 (Vue 3)

### G1. 组件设计

**页面组件** (12 个):
- FeedView, ProjectDetailView, ProblemsView, CompetitionView, CompetitionDetailView
- RankView, PointsView, HomeworkView, HomeworkDetailView, AnalyticsView, AdminView

**评价**:
- 🟢 页面组件按业务域划分
- 🟢 使用 Element Plus 组件库
- 🟡 缺少公共组件提取 (评论列表、排名列表等复用组件)
- 🟡 部分页面组件较大，可进一步拆分

### G2. 状态管理

- 🟢 使用 Pinia 进行状态管理
- 🟢 `stores/user.ts` 管理用户认证状态
- 🟡 仅有一个 Store — 项目状态、排行榜状态等未集中管理

### G3. 路由设计

- 🟢 使用 Vue Router
- 🟢 路由守卫实现认证检查
- 🟢 12 个路由配置合理
- 🟡 缺少路由懒加载 (`() => import(...)`) — 首屏加载可能较慢

### G4. API 封装

- 🟢 使用 Axios 封装 API 客户端
- 🟢 请求拦截器自动添加 Token
- 🟢 响应拦截器统一处理错误
- 🟢 提供类型安全的辅助函数
- 🟡 API 基础地址硬编码 — 应从环境变量读取

### G5. TypeScript 使用

- 🟢 使用 TypeScript
- 🟢 `types/index.ts` 定义了 20+ 类型
- 🟢 `vue-tsc` 类型检查 0 错误
- 🟢 Element Plus 按需导入
- 🟡 部分组件使用 `any` 类型

---

## H. DevOps 分析

### H1. Docker 配置

**docker-compose.yml**:
- 🟢 服务完整: MySQL 8.0, Redis 7, MinIO, Backend, Sandbox, Frontend (Nginx)
- 🟢 健康检查配置完善 (每个服务都有 healthcheck)
- 🟢 资源限制合理: MySQL 1G, Redis 512M, MinIO 512M, Backend 1G, Sandbox 1.5G
- 🟢 `depends_on` + `condition: service_healthy` 确保启动顺序
- 🟢 使用环境变量配置，支持 `.env` 文件
- 🟢 `restart: unless-stopped` 保证服务自动恢复

**Nginx 配置**:
- 🟢 SPA 路由 (`try_files $uri $uri/ /index.html`)
- 🟢 API 反向代理到 Backend
- 🟢 WebSocket 支持 (`Upgrade` + `Connection`)
- 🟢 Swagger UI 代理
- 🟡 缺少静态资源缓存配置
- 🟡 缺少 gzip 压缩配置
- 🟡 缺少 HTTPS 配置

**Sandbox Dockerfile**:
- 🟢 基于 `node:18-alpine`，镜像小巧
- 🟢 非 root 用户运行
- 🟢 健康检查配置
- 🟢 内存限制 1GB

### H2. CI/CD 流程

**GitHub Actions** (4 个 Job):
1. **Backend**: Maven 编译 + 测试 + 打包 + 依赖漏洞扫描
2. **Frontend**: npm ci + 类型检查 + 测试 + 构建 + 安全审计
3. **Sandbox**: npm install + 测试 + 安全审计
4. **Docker**: 镜像构建验证
5. **Lint**: Checkstyle + 编译验证

**评价**:
- 🟢 CI 流程完整，覆盖后端/前端/沙箱
- 🟢 依赖漏洞扫描 (`mvn dependency:check`, `npm audit`)
- 🟢 产物上传 (backend-jar, frontend-dist)
- 🔴 **无 CD (持续部署)** — 只有构建验证，没有自动部署
- 🟡 **无代码覆盖率报告** — 应集成 JaCoCo
- 🟡 **无 E2E 测试** — 应集成 Playwright/Cypress

### H3. 环境管理

- 🟢 `.env.example` 提供环境变量模板
- 🟢 `application.yml` / `application-dev.yml` / `application-prod.yml` 多环境配置
- 🟢 生产环境禁用 Swagger, 调整日志级别
- 🟡 `.env.example` 包含默认密码 (`scratch123`, `minioadmin`) — 应标注必须修改
- 🟡 缺少 `application-test.yml` — 测试环境配置

### H4. 监控告警

- 🟢 Spring Actuator 暴露 `/api/health` 端点
- 🟢 Docker 健康检查配置
- 🟡 **无应用监控** — 缺少 Prometheus + Grafana
- 🟡 **无日志聚合** — 缺少 ELK/Loki
- 🟡 **无告警机制** — 服务异常无通知
- 🟡 **无 APM** — 缺少链路追踪 (SkyWalking/Jaeger)

---

## 优化建议汇总

### 🔴 严重问题 (必须修复)

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| 1 | 无 Token 刷新/黑名单机制 | JwtUtils.java | 实现 Refresh Token + Redis 黑名单 |
| 2 | 无 HTTPS 强制 | nginx.conf, application.yml | 生产环境必须配置 HTTPS |
| 3 | 无集成测试 | test/ | 添加 API 层集成测试 |
| 4 | 无 CD 自动部署 | ci.yml | 添加自动部署流程 |
| 5 | user 表缺少 email/phone | V1__init_schema.sql | 添加联系方式字段 |
| 6 | 无 XSS 全局过滤 | common/ | 添加 XSS 过滤器或 Content-Security-Policy |

### 🟡 中等问题 (建议修复)

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| 7 | 限流基于内存，多实例失效 | RateLimitConfig.java | 改为 Redis + Lua 分布式限流 |
| 8 | 敏感词硬编码 | SensitiveWordFilter.java | 从数据库/配置文件加载 |
| 9 | AnalyticsService N+1 查询 | AnalyticsService.java | 循环中的 AVG 查询改为单次聚合 |
| 10 | 缺少业务缓存 | 各 Service | 用户信息/项目详情等热数据加 Redis 缓存 |
| 11 | 缺少请求链路追踪 | common/ | 集成 SkyWalking 或添加 Trace ID |
| 12 | toVO() 代码重复 | 各 Service | 抽取 BaseService 或使用 MapStruct |
| 13 | 路由未懒加载 | router/index.ts | 使用动态 import 实现懒加载 |
| 14 | 缺少代码覆盖率 | ci.yml | 集成 JaCoCo + Codecov |
| 15 | IP 限流可伪造 | RateLimitConfig.java | 校验 X-Forwarded-For 来源 |
| 16 | 限流器无过期清理 | RateLimitConfig.java | 定期清理过期 Window |

### 🟢 轻微问题 (可选优化)

| # | 问题 | 位置 | 建议 |
|---|------|------|------|
| 17 | 部分 Controller 缺少 @Operation | ProjectController 等 | 补充 Swagger 注解 |
| 18 | 缺少 gzip 压缩 | nginx.conf | 添加 gzip 配置 |
| 19 | 缺少静态资源缓存 | nginx.conf | 添加 Cache-Control |
| 20 | AdminService 10 次 COUNT 查询 | AdminService.java | 合并为单次聚合查询 |
| 21 | 缺少结构化日志 | application.yml | 使用 JSON 格式日志 |
| 22 | scratch-judge-core 未使用 | 项目结构 | 清理或合并到 scratch-judge |
| 23 | docker/init.sql 与 Flyway 重复 | docker/ | 明确职责划分 |

---

## 整体评分

| 维度 | 评分 | 说明 |
|------|------|------|
| **A. 架构设计** | **8/10** | 模块划分合理，分层清晰，无循环依赖。扣分: judge-core 未使用，跨模块查询用 JdbcTemplate |
| **B. 数据库设计** | **8/10** | 表结构设计良好，索引合理，Flyway 管理规范。扣分: 缺少 email 字段，无物理外键 |
| **C. API 接口** | **8/10** | RESTful 规范，参数校验完善，统一返回体。扣分: 部分接口缺少文档注解 |
| **D. 安全** | **6/10** | BCrypt 加密、JWT 认证、限流、敏感词过滤。扣分: 无 Token 刷新/黑名单、无 HTTPS、无 XSS 过滤、限流基于内存 |
| **E. 代码质量** | **7/10** | 命名规范、异常处理完善、日志记录良好。扣分: 测试覆盖率低、代码重复、无集成测试 |
| **F. 性能** | **7/10** | N+1 优化到位、异步处理合理、连接池配置得当。扣分: 缺少缓存、部分查询可优化 |
| **G. 前端架构** | **7/10** | Vue 3 + TypeScript + Pinia 架构合理。扣分: 缺少懒加载、公共组件不足、状态管理简单 |
| **H. DevOps** | **6/10** | Docker 配置完善、CI 流程完整。扣分: 无 CD、无监控告警、无 HTTPS |
| **综合评分** | **7.1/10** | — |

---

## 项目亮点 ✨

1. **模块化设计优秀** — 10 个 Maven 模块职责清晰，依赖方向正确
2. **SB3 解析库独立** — 纯 Java 库，不依赖 Spring，可独立测试和复用
3. **积分系统设计精巧** — Spring Event 驱动，跨模块解耦，Redisson 分布式锁保证并发安全
4. **竞赛排名优化出色** — 双策略(增量+全量)，352 次查询优化到 10 次
5. **AI 点评规则引擎** — 5 维度自动分析，Markdown 报告生成，预留 LLM 接口
6. **判题沙箱进程隔离** — 独立子进程执行，防止恶意代码影响主服务
7. **文档体系完善** — PITFALLS/TODO/DEV_PLAN/CODING_STANDARDS/PROGRESS 五件套
8. **踩坑记录详实** — 19 个实际问题记录，对后续开发极有价值

---

*审计完成。本报告基于 2026-04-24 代码库状态，覆盖全部后端模块、前端 Vue 3、沙箱、Docker、CI/CD。*
