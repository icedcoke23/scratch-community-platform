# 🔍 Scratch Community Platform — 深度分析报告

> 分析日期：2026-04-24
> 项目版本：0.1.0-SNAPSHOT（Sprint 14 完成）
> 分析范围：架构、代码、API、数据库、前端、文档、踩坑记录

---

## 目录

1. [项目概览](#1-项目概览)
2. [架构分析](#2-架构分析)
3. [后端深度分析](#3-后端深度分析)
4. [数据库设计分析](#4-数据库设计分析)
5. [前端架构分析](#5-前端架构分析)
6. [Sandbox 判题沙箱分析](#6-sandbox-判题沙箱分析)
7. [API 接口分析](#7-api-接口分析)
8. [安全分析](#8-安全分析)
9. [DevOps & CI/CD 分析](#9-devops--cicd-分析)
10. [文档质量分析](#10-文档质量分析)
11. [踩坑记录评价](#11-踩坑记录评价)
12. [性能分析](#12-性能分析)
13. [综合优化建议](#13-综合优化建议)
14. [优先级排序](#14-优先级排序)

---

## 1. 项目概览

### 1.1 项目定位
面向少儿编程的 Scratch 编程社区平台，覆盖 **创作 + 社区 + 判题 + 教学** 全链路。面向 K12 学生和编程教师。

### 1.2 功能矩阵

| 模块 | 核心功能 | 完成度 | 代码质量 |
|------|---------|--------|---------|
| 🎨 创作引擎 | 项目 CRUD、sb3 上传/解析、Remix | ✅ 完整 | ⭐⭐⭐⭐ |
| 🤖 AI 点评 | 规则引擎 + LLM 双模式、SSE 流式 | ✅ 完整 | ⭐⭐⭐⭐ |
| 📚 教学管理 | 班级、作业、批改、学情分析 | ✅ 完整 | ⭐⭐⭐⭐ |
| 🏆 竞赛系统 | 创建/报名/答题/排名 | ✅ 完整 | ⭐⭐⭐ |
| 🎯 判题系统 | 选择/判断/Scratch 编程题 | ✅ 完整 | ⭐⭐⭐⭐ |
| 💰 积分体系 | 5 种规则、等级、签到、排行榜 | ✅ 完整 | ⭐⭐⭐⭐ |
| 👥 社区互动 | 点赞/评论/Feed/排行榜 | ✅ 完整 | ⭐⭐⭐⭐⭐ |
| 🔧 系统管理 | 审核/通知/配置 | ✅ 完整 | ⭐⭐⭐⭐ |
| 📱 移动端 | 响应式适配 | ✅ 完整 | ⭐⭐⭐ |

### 1.3 代码规模

| 类别 | 文件数 | 说明 |
|------|--------|------|
| Java 源文件 | ~169 | 含测试 |
| Vue 组件 | 19 | 页面 + 通用组件 |
| TypeScript 文件 | 17 | 含测试 |
| SQL 迁移脚本 | 8 | Flyway V1-V8 |
| Docker 配置 | 4 | Compose + Dockerfile |
| 文档 | 15+ | 开发文档 + 前期设计 |

---

## 2. 架构分析

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│ Nginx (前端静态 + 反向代理)                                │
├─────────────────────────────────────────────────────────┤
│ Spring Boot 3.2 模块化单体 (JDK 17)                      │
│ ┌─────────┬──────────┬─────────┬─────────┬──────────┐   │
│ │ user    │ editor   │ social  │ judge   │classroom │   │
│ │ 用户系统 │ 创作引擎  │ 社区系统 │ 判题系统 │ 教学管理  │   │
│ ├─────────┴──────────┴─────────┴─────────┴──────────┤   │
│ │ common (认证/异常/审核/限流/分布式锁/工具)              │   │
│ │ sb3-parser / judge-core (共享库)                     │   │
│ └────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│ MySQL 8.0 │ Redis 7 │ MinIO │ Node.js Sandbox           │
└─────────────────────────────────────────────────────────┘
```

### 2.2 架构评价

**✅ 优点：**
- **模块化单体** 选择了正确的架构模式 — 对于这个规模的项目，微服务是过度设计
- **部署单元少**（2 个：Spring Boot + Node.js Sandbox），运维简单
- **共享库设计**（scratch-sb3、scratch-judge-core）实现了关注点分离
- **进程隔离判题**，子进程 + 内存限制，安全性好

**⚠️ 问题：**
- `scratch-judge-core` — ✅ 已实现（JudgeEngine 接口 + JudgeResult 模型 + Verdict 架举）
- `CrossModuleQueryRepository` 读写不分，违反自身设计原则
- 模块间存在大量 `JdbcTemplate` 裸 SQL 跨模块查询，边界模糊
- `frontend/`（旧版）和 `frontend-vue/`（新版）共存，造成困惑

### 2.3 架构建议

1. **`scratch-judge-core` 已实现** — ✅ 包含 JudgeEngine 接口、JudgeResult 模型、Verdict 架举
2. **`CrossModuleQueryRepository` 拆分为读写两个 Repository** — `CrossModuleQueryRepository`（只读）+ `CrossModuleWriteRepository`（只写）
3. **删除 `frontend/` 旧版目录** — ✅ 已完成删除
4. **考虑引入 CQRS 模式** — 对于 Feed、排行榜等读多写少的场景

---

## 3. 后端深度分析

### 3.1 技术栈

| 技术 | 版本 | 评价 |
|------|------|------|
| Spring Boot | 3.2.5 | ✅ 最新稳定版 |
| JDK | 17 | ✅ LTS 版本 |
| MyBatis-Plus | 3.5.6 | ✅ 成熟稳定 |
| Redisson | 3.28.0 | ✅ 分布式锁 |
| MinIO | 8.5.10 | ✅ 对象存储 |
| JJWT | 0.12.5 | ✅ JWT 实现 |
| Hutool | 5.8.27 | ✅ 工具库 |
| fastjson2 | 2.0.47 | ⚠️ 与 Jackson 混用 |
| SpringDoc | 2.5.0 | ✅ Swagger |

### 3.2 代码质量分析

#### 3.2.1 优点

1. **分层清晰** — Controller → Service → Mapper 标准三层架构
2. **DTO/VO 分离** — 入参用 DTO，出参用 VO，不直接暴露 Entity
3. **统一响应** — `R<T>` 包装所有返回值
4. **全局异常处理** — `GlobalExceptionHandler` 统一捕获
5. **事件驱动** — 积分系统使用 Spring `ApplicationEvent` 解耦
6. **逻辑删除** — MyBatis-Plus 全局配置 `deleted` 字段
7. **审计字段** — `created_at` / `updated_at` 自动填充

#### 3.2.2 问题

**🔴 严重问题：**

1. **JSON 库混用** — scratch-sb3 用 fastjson2，其他模块用 Jackson。踩坑记录中有大量相关问题（坑 1、坑 10）。建议统一为 Jackson。

2. **`@Transactional` 自调用** — 坑 16 记录了 `AiReviewService` 中同类调用导致事务不生效的问题。虽然已修复，但这种模式可能在其他 Service 中存在。

3. **`LoginUser.get()` NPE 风险** — 坑 17 记录了未登录时 `LoginUser.get()` 返回 null 的问题。建议改为返回 `Optional<LoginUser>`。

4. **跨模块 JdbcTemplate 裸 SQL** — 多个 Service 直接写其他模块的表。已部分修复（引入 `CrossModuleQueryRepository`/`CrossModuleWriteRepository`），但仍有散落的 SQL。

**🟡 中等问题：**

5. **Lombok `@Data` 继承陷阱** — 坑 5 记录了 `ProjectDetailVO extends ProjectVO` 的 `equals()`/`hashCode()` 问题。建议 Entity 用 `@Getter/@Setter`，VO 不继承。

6. **文件大小限制分散** — Controller 和 Utils 各自定义不同的限制值（50MB vs 100MB）。应统一到常量类。

7. **`scratch-judge-core`** — ✅ 已实现。

8. **限流器 IP 获取可伪造** — 坑 36 记录了 `X-Forwarded-For` 伪造问题。应优先使用 `X-Real-IP`。

**🟢 轻微问题：**

9. **部分 Service 方法过长** — 如 `CompetitionService` 的排名更新逻辑可以提取为独立方法。

10. **注释语言不统一** — 大部分中文注释，少数英文注释。

### 3.3 模块详细分析

#### scratch-common（公共基础层）
- **认证** — JWT + Token 黑名单（支持 Token 级和用户级）
- **限流** — 自研滑动窗口限流器（有内存泄漏风险，需清理机制）
- **审核** — DFA 敏感词自动机（有类型安全问题，应用 `DfaNode` 类替代 `Map<Object, Object>`）
- **跨模块查询** — `CrossModuleQueryRepository` + `CrossModuleWriteRepository`
- **事件** — `PointEvent` 积分事件

#### scratch-sb3（sb3 解析库）
- **解析流程** — 解压 → JSON 解析 → 角色提取 → 积木统计 → 复杂度计算
- **问题** — 使用 fastjson2，与其他模块的 Jackson 不一致
- **优点** — 纯 Java 库，无 Spring 依赖，可独立使用

#### scratch-editor（创作引擎）
- **功能** — 项目 CRUD、sb3 上传/下载、发布、Remix
- **集成** — 通过 `EditorConfig` 注册 `SB3Parser` Bean

#### scratch-judge（判题系统）
- **流程** — 提交 → 异步判题 → 沙箱执行 → 结果回写
- **集成** — 调用 Node.js Sandbox 服务

#### scratch-social（社区系统）
- **功能** — 点赞/评论/Feed/排行榜
- **并发安全** — 使用 `INSERT IGNORE` + 原子递增避免竞态
- **问题** — 已引入 `CrossModuleWriteRepository`，但踩坑记录显示曾有多次编译错误

#### scratch-classroom（教学管理）
- **功能** — 班级、作业、提交、批改、学情分析
- **问题** — AnalyticsService 曾有 N+1 查询（坑 22），已修复

#### scratch-system（系统管理）
- **功能** — 通知、审核、配置
- **问题** — `@ConfigurationProperties` 未正确激活（坑 41）

---

## 4. 数据库设计分析

### 4.1 表结构概览

共 **20 张核心表**，覆盖 9 个业务模块：

| 模块 | 表名 | 说明 |
|------|------|------|
| 用户 | `user` | 用户基础信息 |
| 用户 | `user_follow` | 关注关系 |
| 用户 | `class` | 班级 |
| 用户 | `class_student` | 班级学生关系 |
| 创作 | `project` | 项目 |
| 社区 | `project_like` | 点赞 |
| 社区 | `project_comment` | 评论 |
| 判题 | `problem` | 题目 |
| 判题 | `submission` | 提交记录 |
| 教室 | `homework` | 作业 |
| 教室 | `homework_submission` | 作业提交 |
| 积分 | `point_log` | 积分变动记录 |
| 竞赛 | `competition` | 竞赛 |
| 竞赛 | `competition_registration` | 竞赛报名 |
| 竞赛 | `competition_ranking` | 竞赛排名 |
| AI | `ai_review` | AI 点评记录 |
| 系统 | `notification` | 通知 |
| 系统 | `content_audit_log` | 审核记录 |
| 系统 | `system_config` | 系统配置 |

### 4.2 数据库设计评价

**✅ 优点：**

1. **索引设计合理** — 每张表都有覆盖高频查询的复合索引
   - `idx_project_feed` (`status`, `deleted`, `created_at`) — Feed 查询
   - `idx_user_points_rank` (`points` DESC, `status`, `deleted`) — 积分排行
   - `idx_submission_user_problem` (`user_id`, `problem_id`, `verdict`) — 提交去重
2. **唯一约束到位** — `uk_username`、`uk_email`、`uk_follow`、`uk_user_project` 等
3. **逻辑删除** — `deleted` 字段统一使用
4. **UTF8MB4** — 支持 emoji 等 4 字节字符
5. **JSON 字段** — 合理使用（`options`、`expected_output`、`problem_ids`、`dimension_scores`）
6. **全文索引** — `project` 和 `problem` 表的 `ft_search` 使用 ngram 解析器

**⚠️ 问题：**

1. **`class` 是 MySQL 保留字** — 需要反引号包裹。建议改名为 `class_room` 或 `clazz`。

2. **`submission` 表的 `judge_detail` 用 TEXT** — 如果判题详情很长，建议改为 JSON 类型（MySQL 8.0 支持）。

3. **缺少 `competition_registration` 表的 `status` 字段** — 无法区分"已报名"和"已退赛"。

4. **`homework` 表的 `problem_ids` 用 JSON** — 无法直接 JOIN 查询，只能在应用层解析。如果需要统计"某道题被多少作业引用"，需要全文扫描。

5. **缺少软删除的 `deleted_at` 字段** — 当前只有 `deleted` (0/1)，无法知道何时删除。

6. **`user` 表的 `password` 长度 255** — BCrypt 固定 60 字符，255 浪费。建议改为 100。

7. **`notification` 表缺少过期机制** — 通知会无限增长，需要定期清理或归档。

### 4.3 数据库优化建议

1. **添加 `deleted_at` 字段** — 方便审计和数据恢复
2. **考虑分区表** — `submission`、`point_log`、`notification` 等增长快的表按月分区
3. **添加 `competition_registration.status` 字段**
4. **`class` 表改名** — 避免保留字问题
5. **考虑读写分离** — Feed、排行榜等读多写少的场景
6. **定期归档** — `notification`、`content_audit_log` 等表定期归档旧数据

---

## 5. 前端架构分析

### 5.1 技术栈

| 技术 | 版本 | 评价 |
|------|------|------|
| Vue | 3.5.32 | ✅ 最新稳定版 |
| TypeScript | 6.0.2 | ✅ 最新 |
| Vite | 8.0.10 | ✅ 最新（使用 Rolldown） |
| Element Plus | 2.13.7 | ✅ 成熟 UI 库 |
| Pinia | 3.0.4 | ✅ 状态管理 |
| Vue Router | 4.6.4 | ✅ 路由管理 |
| Axios | 1.15.2 | ✅ HTTP 客户端 |
| Vitest | 4.1.5 | ✅ 测试框架 |

### 5.2 前端架构评价

**✅ 优点：**

1. **TypeScript 全面使用** — 类型定义完整（`types/index.ts`）
2. **API 层封装好** — `api/index.ts` 统一管理所有接口，类型安全
3. **响应拦截完善** — Token 过期自动登出、限流提示、网络错误处理
4. **组件化做得好** — `ProjectCard`、`CommentList`、`EmptyState`、`LoadingSkeleton` 等通用组件
5. **SSE 流式支持** — AI 点评使用 EventSource 实现流式输出
6. **路由守卫** — 角色权限控制（`requiresAuth`、`requiresTeacher`、`requiresAdmin`）
7. **响应式适配** — 768px/480px 断点，底部 Tab 导航
8. **测试覆盖** — stores、utils、components、errorHandler 都有测试

**⚠️ 问题：**

1. **API 辅助函数参数类型** — 踩坑记录坑 39 指出 `params` 和 `data` 曾是 `any` 类型，已改为 `Record<string, unknown>`，但仍有改进空间。

2. **Vue 组件较少（19 个）** — 对于这个规模的项目，组件拆分可以更细。`AdminView`、`AnalyticsView` 等大组件可以拆分子组件。

3. **缺少全局状态管理** — 只有 `userStore`，没有项目、通知等状态管理。多个页面重复加载相同数据。

4. **缺少路由懒加载优化** — 所有路由都用 `() => import()` 动态导入，但没有按模块分组。

5. **`frontend/` 旧版目录** — ✅ 已删除。

6. **定时器管理** — 坑 42 记录了 `setInterval` 未清理的问题，已修复但需要持续关注。

### 5.3 前端优化建议

1. **拆分大组件** — `AdminView`、`AnalyticsView` 等拆分为子组件
2. **引入 Pinia store** — 为项目列表、通知等创建独立 store
3. **路由分组** — 按模块拆分路由文件
4. **删除 `frontend/` 旧版目录**
5. **添加 E2E 测试** — 当前只有单元测试，缺少端到端测试
6. **引入 PWA** — 对于教育场景，离线支持很有价值

---

## 6. Sandbox 判题沙箱分析

### 6.1 架构

```
Backend (Java) → HTTP POST /judge → Sandbox (Node.js Express)
                                        ↓
                                  fork 子进程
                                        ↓
                              judge-runner.js (scratch-vm headless)
                                        ↓
                                  返回判题结果
```

### 6.2 评价

**✅ 优点：**

1. **进程隔离** — 每个判题任务在独立子进程中执行，防止恶意 sb3 影响主服务
2. **内存限制** — 子进程 `--max-old-space-size=512`
3. **超时控制** — 双重超时（判题超时 + 10 秒缓冲）
4. **优雅关闭** — SIGTERM/SIGINT 处理，强制终止所有子进程
5. **Redis 降级** — Redis 不可用时自动降级为内存队列
6. **临时文件清理** — 判题完成后自动删除 sb3 文件

**⚠️ 问题：**

1. **内存队列无上限** — 降级模式下 `Map` 无限增长，需要限制最大任务数
2. **缺少并发控制** — 没有限制同时运行的判题任务数，可能耗尽系统资源
3. **`task_id_counter` 是全局变量** — 多实例部署时可能冲突
4. **缺少结果回调** — 当前是轮询模式（`GET /judge/status/:taskId`），可以改为 Webhook 回调
5. **缺少 sb3 文件校验** — 下载后未检查文件大小和格式

### 6.3 Sandbox 优化建议

1. **添加并发限制** — 限制同时运行的判题任务数（如 CPU 核心数）
2. **内存队列添加上限** — 如最大 1000 个任务
3. **添加结果回调** — 判题完成后主动通知 Backend
4. **添加 sb3 文件校验** — 下载后检查大小和 ZIP 格式
5. **使用 UUID 替代递增 ID** — 避免多实例冲突

---

## 7. API 接口分析

### 7.1 接口概览

| 模块 | API 数量 | 核心接口 |
|------|---------|---------|
| user | 10+ | 注册/登录/关注/班级管理 |
| editor | 11 | 项目 CRUD/sb3 上传/发布/Remix |
| social | 12 | 点赞/评论/Feed/排行榜 |
| judge | 9 | 题目管理/判题/提交记录 |
| classroom | 12 | 作业布置/提交/批改/学情分析 |
| system | 7 | 通知/审核/配置 |
| admin | 5 | 用户管理/数据面板 |
| point | 5 | 积分/签到/排行榜 |
| competition | 7 | 竞赛/报名/排名 |
| ai-review | 3 | AI 点评生成/查询 |
| **总计** | **~80+** | |

### 7.2 API 设计评价

**✅ 优点：**

1. **RESTful 风格** — 资源路径清晰（`/api/project/{id}`、`/api/social/comment`）
2. **统一响应格式** — `R<T>` 包装，`code=0` 表示成功
3. **分页支持** — MyBatis-Plus `Page<T>` 统一分页
4. **Swagger 文档** — SpringDoc 自动生成 API 文档
5. **版本兼容** — 同时支持 `/api/` 和 `/api/v1/` 路径

**⚠️ 问题：**

1. **缺少 API 版本策略** — `/api/` 和 `/api/v1/` 共存，但没有明确的版本迁移计划
2. **部分接口返回值不一致** — 有的返回 `R<Page<T>>`，有的返回 `R<List<T>>`
3. **缺少接口幂等性设计** — 部分 POST 接口不是幂等的
4. **SSE 端点 Token 通过 query 参数传递** — 有日志泄露风险

### 7.3 API 优化建议

1. **统一版本策略** — 明确 `/api/v1/` 为标准路径，设置 `/api/` 的废弃时间
2. **统一返回值格式** — 分页接口统一使用 `R<Page<T>>`
3. **添加接口幂等性** — 关键操作（如点赞、提交）支持幂等
4. **SSE Token 脱敏** — 在访问日志中过滤 `?token=xxx` 参数

---

## 8. 安全分析

### 8.1 安全措施

| 安全层 | 实现方式 | 评价 |
|--------|---------|------|
| 认证 | JWT + Token 黑名单 | ✅ 支持 Token 级和用户级 |
| 授权 | `@RequireRole` 注解 | ✅ 角色校验 |
| 限流 | 自研滑动窗口 | ⚠️ 有内存泄漏和 IP 伪造风险 |
| XSS | Vue 默认转义 + DFA 敏感词 | ⚠️ JSON Body 未过滤 |
| CSRF | CORS 限制 | ✅ 仅允许指定域名 |
| 文件上传 | 大小限制 + ZIP 炸弹防护 | ⚠️ 限制值不一致 |
| SQL 注入 | MyBatis-Plus 参数化 | ✅ 安全 |
| 密码存储 | BCrypt | ✅ 安全 |
| 敏感配置 | Docker Compose ${VAR:?error} | ✅ 强制要求设置 |

### 8.2 安全问题

**🔴 高危：**

1. **JWT 密钥有默认值** — `application.yml` 中 `scratch.jwt.secret` 有默认值，虽然 Docker Compose 强制要求设置，但直接运行 JAR 时会使用默认密钥。

2. **限流器 IP 获取可伪造** — 踩坑 36 已记录，但需要确认是否已修复。

3. **ConcurrentHashMap 内存泄漏** — 踩坑 48 已记录限流器无清理机制。

**🟡 中危：**

4. **XSS 防护不完整** — JSON Body 的 XSS 过滤依赖前端转义，后端未做 Jackson 层转义。

5. **SSE Token 通过 query 参数传递** — 有日志泄露风险。

6. **文件大小限制不一致** — Controller (50MB) vs Utils (100MB)。

### 8.3 安全优化建议

1. **JWT 密钥强制校验** — 在 `@PostConstruct` 中检查，非 dev 环境必须设置
2. **限流器 IP 使用 `X-Real-IP`** — 优先信任 Nginx 设置的不可伪造 Header
3. **限流器添加清理机制** — `@Scheduled` 定时清理过期窗口
4. **添加 Jackson XSS 过滤器** — 自定义 `String` 反序列化器
5. **SSE Token 脱敏** — 在访问日志中过滤 `?token=xxx`

---

## 9. DevOps & CI/CD 分析

### 9.1 CI/CD 流程

```
Push/PR → [Backend Build & Test] → [Frontend Build & Test] → [Sandbox Build & Test]
                                    ↓
                              [Docker Build Verify]
                                    ↓
                              [Code Quality Check]
                                    ↓
                              [Deploy to Production] (仅 main 分支)
```

### 9.2 评价

**✅ 优点：**

1. **多阶段 CI** — Backend、Frontend、Sandbox、Docker、Lint 5 个 Job 并行
2. **安全扫描** — Maven dependency:check + npm audit
3. **类型检查** — `vue-tsc --noEmit`
4. **测试覆盖** — 前后端都有测试
5. **Artifact 上传** — JAR 和前端 dist 可下载

**⚠️ 问题：**

1. **CD 是空壳** — Deploy Job 只打印日志，没有实际部署逻辑
2. **缺少代码覆盖率报告** — 没有 JaCoCo / Istanbul
3. **Checkstyle 可选** — `|| true` 使其永远通过
4. **缺少 Docker 镜像推送** — 没有推送到 Docker Hub / Harbor
5. **Sandbox 测试缺失** — `npm test --if-present` 但没有测试文件

### 9.3 CI/CD 优化建议

1. **实现实际 CD** — 推送到 Docker Hub，通过 SSH/Webhook 部署到 VPS
2. **添加代码覆盖率** — 后端 JaCoCo，前端 Istanbul
3. **强制 Checkstyle** — 去掉 `|| true`
4. **添加 Sandbox 测试** — 编写判题逻辑的单元测试
5. **添加 E2E 测试** — Playwright / Cypress

---

## 10. 文档质量分析

### 10.1 文档矩阵

| 文档 | 质量 | 说明 |
|------|------|------|
| README.md | ⭐⭐⭐⭐⭐ | 功能矩阵、架构图、快速启动、API 概览，非常完善 |
| DEV_PLAN.md | ⭐⭐⭐⭐ | Sprint 规划 + 里程碑 + 风险管理 |
| CODING_STANDARDS.md | ⭐⭐⭐⭐ | Java/Node.js/数据库/API/Git/安全规范 |
| MODULE_DEV_GUIDE.md | ⭐⭐⭐⭐ | 从 0 到 1 的标准开发流程 |
| QA_CHECKLIST.md | ⭐⭐⭐⭐ | PR 自检 + Sprint 审计模板 |
| PITFALLS.md | ⭐⭐⭐⭐⭐ | 52 条踩坑记录 + 49 条经验总结，非常详细 |
| PROGRESS.md | ⭐⭐⭐⭐ | Sprint 完成状态 + 审计记录 |
| DEPLOYMENT.md | ⭐⭐⭐ | 生产环境部署说明 |
| CHANGELOG.md | ⭐⭐⭐⭐ | 详细的版本变更记录 |
| CONTRIBUTING.md | ⭐⭐⭐ | 开发流程和规范 |

### 10.2 前期设计文档

| 文档 | 说明 |
|------|------|
| 系统设计草案 v0.3 | 架构方案 |
| 模块详细设计 v0.2 | 6 模块 + 2 共享库详细设计 |
| 数据库表设计 v0.2 | 18 张核心表结构 |
| 技术栈选型 v0.4 | 技术选型决策 |
| 功能清单 v0.5 | MVP 功能范围 |

### 10.3 文档评价

**✅ 优点：**
- 文档体系非常完整，从需求到设计到开发到部署全覆盖
- 踩坑记录极其详细，有现象、原因、修复、教训，是宝贵的工程经验
- 前期设计文档与实际实现基本一致

**⚠️ 问题：**
- 部分文档与实现已同步（`scratch-judge-core` 已实现，`frontend/` 已删除）
- 缺少 API 接口文档（依赖 Swagger 自动生成）
- 缺少架构决策记录（ADR）

---

## 11. 踩坑记录评价

### 11.1 踩坑记录质量

**评价：⭐⭐⭐⭐⭐（满分）**

踩坑记录是这个项目最大的亮点之一。52 条坑 + 49 条经验总结，覆盖了：

- **编译问题** — fastjson2 API、BCrypt 依赖、pom.xml 闭合
- **运行时问题** — @Transactional 自调用、LoginUser NPE、Redis RENAME
- **性能问题** — N+1 查询、关联子查询、全量逐条 UPDATE
- **安全问题** — JWT 默认密钥、X-Forwarded-For 伪造、ZIP 炸弹
- **架构问题** — 跨模块 SQL、读写不分、空模块
- **前端问题** — Axios 泛型、Vite 8 兼容、Element Plus 类型
- **运维问题** — Docker Compose 环境变量、git push 超时

每条坑都有：
1. **现象** — 具体的错误信息
2. **原因** — 深入的技术分析
3. **修复** — 可执行的代码
4. **教训** — 可泛化的经验

### 11.2 建议

1. **将踩坑记录纳入知识库** — 可以考虑用 Notion / Wiki 管理
2. **定期回顾** — 每个 Sprint 结束时回顾新增的坑
3. **转化为自动化检查** — 将高频坑转化为 Lint 规则或 CI 检查

---

## 12. 性能分析

### 12.1 已识别的性能问题

| 问题 | 状态 | 影响 |
|------|------|------|
| AnalyticsService N+1 查询 | ✅ 已修复 | 学情分析页面加载慢 |
| AdminService 10 次独立 COUNT | ✅ 已修复 | 管理面板加载慢 |
| CompetitionService 全量逐条 UPDATE | ✅ 已修复 | 竞赛提交慢 |
| PointService 关联子查询 | ✅ 已修复 | 积分排行慢 |
| 限流器 ConcurrentHashMap 内存泄漏 | ⚠️ 待修复 | 长期运行内存增长 |
| DFA 敏感词 Map<Object, Object> | ⚠️ 待修复 | 类型不安全 |

### 12.2 性能优化建议

1. **引入 Redis 缓存** — Feed、排行榜、用户信息等热点数据
2. **数据库连接池调优** — 根据实际负载调整 HikariCP 参数
3. **添加数据库索引监控** — 慢查询日志分析
4. **前端资源优化** — 图片懒加载、CDN 加速
5. **考虑读写分离** — 读多写少的场景（Feed、排行榜）

---

## 13. 综合优化建议

### 13.1 短期优化（1-2 周）

| # | 优化项 | 优先级 | 工作量 |
|---|--------|--------|--------|
| 1 | 限流器添加清理机制（坑 48） | 🔴 高 | 2h |
| 2 | DFA 敏感词改用类型安全的 DfaNode 类（坑 49） | 🟡 中 | 3h |
| 3 | 统一文件大小限制到常量类 | 🟡 中 | 1h |
| 4 | 删除 `frontend/` 旧版目录 | ✅ 已完成 | — |
| 5 | 实现 `scratch-judge-core` | ✅ 已完成 | — |
| 6 | JWT 密钥非 dev 环境强制校验 | 🔴 高 | 1h |
| 7 | 限流 IP 使用 X-Real-IP 优先 | 🔴 高 | 1h |

### 13.2 中期优化（1-2 月）

| # | 优化项 | 优先级 | 工作量 |
|---|--------|--------|--------|
| 8 | JSON 库统一为 Jackson | 🟡 中 | 2-3 天 |
| 9 | CrossModuleQueryRepository 拆分读写 | 🟡 中 | 1 天 |
| 10 | 前端大组件拆分 | 🟡 中 | 3-5 天 |
| 11 | 引入 Pinia store（项目列表、通知等） | 🟡 中 | 2-3 天 |
| 12 | Sandbox 添加并发限制和内存队列上限 | 🟡 中 | 1 天 |
| 13 | 添加代码覆盖率报告 | 🟢 低 | 半天 |
| 14 | 实现实际 CD 部署 | 🟡 中 | 2-3 天 |

### 13.3 长期优化（3-6 月）

| # | 优化项 | 优先级 | 工作量 |
|---|--------|--------|--------|
| 15 | 引入 Redis 缓存层 | 🟡 中 | 1-2 周 |
| 16 | 数据库读写分离 | 🟡 中 | 1-2 周 |
| 17 | 添加 E2E 测试 | 🟡 中 | 1-2 周 |
| 18 | 引入 PWA 支持 | 🟢 低 | 1 周 |
| 19 | 考虑微服务拆分（如果用户量增长） | 🔵 规划 | 1-2 月 |
| 20 | 添加数据看板 | 🟢 低 | 1 周 |

---

## 14. 优先级排序

### 🔴 必须修复（安全/稳定性）

1. **限流器添加清理机制** — 长期运行内存泄漏
2. **JWT 密钥强制校验** — 防止使用默认密钥
3. **限流 IP 使用 X-Real-IP** — 防止 IP 伪造绕过限流
4. **统一文件大小限制** — 防止安全漏洞

### 🟡 建议修复（代码质量）

5. **DFA 敏感词类型安全** — 用 `DfaNode` 替代 `Map<Object, Object>`
6. **JSON 库统一** — 消除 fastjson2/Jackson 混用
7. **CrossModuleQueryRepository 拆分读写** — 保持架构一致性
8. **删除 `frontend/` 旧版目录** — ✅ 已完成

### 🟢 可选优化（体验/效率）

9. **前端大组件拆分** — 提高可维护性
10. **添加代码覆盖率** — 提高测试质量
11. **实现实际 CD** — 自动化部署

---

## 总结

### 项目评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 模块化单体选型正确，但有空模块和边界模糊问题 |
| 代码质量 | ⭐⭐⭐⭐ | 分层清晰，有测试，但有 JSON 混用等问题 |
| 数据库设计 | ⭐⭐⭐⭐⭐ | 索引设计合理，表结构完整 |
| 前端架构 | ⭐⭐⭐⭐ | TypeScript 全面，组件化好，但组件较少 |
| 安全性 | ⭐⭐⭐⭐ | 多层防护，但有 IP 伪造和内存泄漏风险 |
| 文档质量 | ⭐⭐⭐⭐⭐ | 非常完整，踩坑记录是亮点 |
| DevOps | ⭐⭐⭐ | CI 完整但 CD 是空壳 |
| **综合** | **⭐⭐⭐⭐** | **高质量的个人项目，工程化程度很高** |

### 一句话评价

> 这是一个工程化程度很高的个人项目 — 从需求分析到架构设计到代码实现到文档到 CI/CD，都有系统性的思考。踩坑记录尤其出色，展现了真实的工程经验。主要改进方向是安全细节（限流器清理、IP 获取）和代码一致性（JSON 库统一、模块边界清晰化）。

---

*分析完成。如有疑问，欢迎讨论。*
