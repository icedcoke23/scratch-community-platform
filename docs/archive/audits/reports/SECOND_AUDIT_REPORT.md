# 🔍 二次审计报告

> 审计日期：2026-04-28
> 审计范围：全项目架构、代码、数据库、前端、文档
> 基于：[首次深度分析报告](COMPREHENSIVE_ANALYSIS.md) 的优化建议

---

## 📊 审计总览

| 维度 | 首次评分 | 二次评分 | 变化 | 说明 |
|------|---------|---------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ↑ | 线程池隔离、读写分离已完善 |
| 数据库设计 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ↑ | 补充 8 个高频查询索引 |
| 代码质量 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐½ | ↑ | 前端 any 类型清理 |
| 安全性 | ⭐⭐⭐⭐½ | ⭐⭐⭐⭐½ | → | 已有完善的安全机制 |
| 文档质量 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | → | 文档体系完整 |
| 测试覆盖 | ⭐⭐⭐ | ⭐⭐⭐ | → | 集成测试仍需 Testcontainers |
| 前端架构 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐½ | ↑ | 类型安全增强 |
| CI/CD | ⭐⭐⭐½ | ⭐⭐⭐½ | → | 缺少代码质量检查步骤 |

**综合评分：4.3 / 5.0**（首次 4.0）

---

## ✅ 本次优化内容

### 1. 数据库索引优化 (V18 迁移)

新增 `V18__additional_indexes.sql`，补充 8 个高频查询缺失索引：

| 索引 | 表 | 用途 |
|------|-----|------|
| `idx_submission_user_problem_time` | submission | 用户做题历史查询 |
| `idx_project_like_project_time` | project_like | 按时间排序的点赞列表 |
| `idx_notification_user_type_read` | notification | 按类型筛选未读通知 |
| `idx_project_hot` | project | 热门项目查询优化 |
| `idx_homework_submission_student_status` | homework_submission | 学生作业查询 |
| `idx_user_follow_following_time` | user_follow | 关注列表查询 |
| `idx_user_follow_follower_time` | user_follow | 粉丝列表查询 |
| `idx_competition_ranking_rank` | competition_ranking | 竞赛排名查询 |

**影响**：
- Feed 流查询（热门排序）从全表扫描变为索引覆盖扫描
- 通知未读查询避免 `(user_id, is_read)` 索引的回表操作
- 竞赛排名查询直接走索引排序，无需 filesort

### 2. 异步线程池隔离

在 `AsyncConfig.java` 中新增 `eventExecutor` 线程池：

| 线程池 | 用途 | 核心/最大/队列 |
|--------|------|---------------|
| `judgeExecutor` | 判题专用（CPU 密集型） | 4 / 16 / 100 |
| `taskExecutor` | SSE 流式输出（低延迟） | 2 / 8 / 50 |
| `eventExecutor` | 事件监听（IO 密集型） | 2 / 8 / 200 |

**好处**：
- 判题高负载不会阻塞 SSE 推送
- 事件监听失败不影响主业务
- 各线程池独立监控和调优

### 3. 前端类型安全增强

修复 3 个文件中的 `any` 类型：

| 文件 | 修改前 | 修改后 |
|------|--------|--------|
| `OAuthCallbackView.vue` | `err: any` | `err: unknown` + `instanceof Error` |
| `OAuthButtons.vue` | `error: any` | `error: unknown` + `instanceof Error` |
| `collab.ts` | `data: any`, `payload: any` | `Record<string, unknown>` |

**好处**：
- TypeScript 编译期类型检查更严格
- IDE 自动补全更准确
- 减少运行时类型错误

### 4. 文档体系整理

- **归档旧版审计报告**：24 份旧版审计报告移入 `docs/archive/audits/`
- **归档前期设计文档**：`前期/` 目录移入 `docs/archive/planning/`
- **创建文档导航**：新增 `docs/INDEX.md`，包含所有文档的分类索引
- **更新 README**：文档链接指向最新版本，移除过时引用

---

## 🔍 遗留问题审计

### 已确认无问题的项目

| 检查项 | 状态 | 说明 |
|--------|------|------|
| JWT 双令牌机制 | ✅ | Access Token + Refresh Token，密钥校验完善 |
| Token 黑名单 | ✅ | 支持 Token 级和用户级黑名单 |
| SSE 一次性 Token | ✅ | `SseTokenService` + Redis/内存双模式 |
| 幂等性拦截器 | ✅ | `@Idempotent` 注解 + Redis SET NX |
| DFA 敏感词过滤 | ✅ | 类型安全的 `DfaNode` 内部类 |
| 滑动窗口限流 | ✅ | Redis Lua 脚本 + 本地 ConcurrentHashMap |
| XSS 分层防护 | ✅ | Jackson 自定义反序列化器 + 前端默认转义 |
| 乐观锁 | ✅ | 4 个 Entity 有 `@Version` 字段 |
| 跨模块读写分离 | ✅ | `CrossModuleQueryRepository` + `CrossModuleWriteRepository` |
| 事件驱动解耦 | ✅ | `@TransactionalEventListener(AFTER_COMMIT)` |
| 事件发布降级 | ✅ | `EventPublisherHelper` 统一 try-catch + fallback |
| Flyway 迁移 | ✅ | V1-V18 版本化管理 |
| 文件大小限制 | ✅ | `FileConstants` 统一常量 |
| 判题沙箱安全 | ✅ | 进程隔离 + 超时控制 + 文件传递 |
| 前端 Token 刷新防并发 | ✅ | `refreshPromise` 单例 + `pendingRequests` 队列 |
| 前端请求取消管理 | ✅ | `AbortController` + Map 管理 |
| 前端 sessionStorage | ✅ | 关闭标签页自动清除 Token |
| 响应式断点 | ✅ | 768px/480px 双断点 + `MobileNav` |
| PWA 支持 | ✅ | `manifest.json` + `sw.js` |
| 深色模式 | ✅ | `useTheme` composable |
| i18n 支持 | ✅ | `useI18n` composable |

### 仍需改进的项目

| 优先级 | 项目 | 说明 | 建议 |
|--------|------|------|------|
| 🔴 高 | 集成测试 | CI 只跑单元测试，`scratch-app` 被排除 | 引入 Testcontainers |
| 🟡 中 | 前端 Store 拆分 | `user` Store 承担过多职责 | 拆为 `auth` + `user` + `notification` |
| 🟡 中 | API OpenAPI 注解 | DTO/VO 缺少 `@Operation`/`@Schema` | 补充注解，生成完整 API 文档 |
| 🟡 中 | 限流响应头 | 缺少 `X-RateLimit-*` 标准 Header | 在限流拦截器中补充 |
| 🟢 低 | Design Token | 颜色/间距硬编码 | 定义 CSS 变量体系 |
| 🟢 低 | 判题结果缓存 | 重复提交重新判题 | Redis 缓存 `judge:{userId}:{problemId}:{hash}` |
| 🟢 低 | CI 代码质量检查 | 缺少 SpotBugs/ESLint | 增加 CI 步骤 |
| 🟢 低 | CI 安全扫描 | 缺少依赖漏洞扫描 | 增加 Dependabot/Snyk |

---

## 📈 架构改进趋势

```
Phase 1-7:  功能开发为主（MVP → 完整功能）
Phase 8:    Vue 3 前端全面建设
Phase 9:    深度优化（限流/Token/Service 拆分）
Phase 10:   生产就绪 + 安全加固
Phase 11:   ← 当前（索引优化/线程池隔离/类型安全/文档治理）
Phase 12:   未来（微服务/读写分离/CDN/消息队列）
```

### 关键架构决策回顾

| 决策 | 选择 | 理由 | 状态 |
|------|------|------|------|
| 单体 vs 微服务 | 模块化单体 | MVP 阶段，团队规模小 | ✅ 正确 |
| JWT vs Session | JWT 双令牌 | 前后端分离，移动端兼容 | ✅ 正确 |
| 固定窗口 vs 滑动窗口 | 滑动窗口 | 避免边界突发 2x 流量 | ✅ 正确 |
| localStorage vs sessionStorage | sessionStorage | 关闭标签页清除，降低 XSS 风险 | ✅ 正确 |
| 事件驱动 vs 直接写 | 事件驱动 + 降级 | 解耦 + 可靠性 | ✅ 正确 |
| fastjson2 vs Jackson | 统一 Jackson | 消除混用，减少维护成本 | ✅ 正确 |

---

## 🎯 下一步建议

### 短期（1-2 Sprint）

1. **Testcontainers 集成测试**：让 `scratch-app` 的集成测试在 CI 中跑起来
2. **OpenAPI 注解补充**：至少覆盖核心 DTO/VO（User/Project/Problem/Homework）
3. **限流响应头**：`X-RateLimit-Limit`/`X-RateLimit-Remaining`/`X-RateLimit-Reset`

### 中期（3-5 Sprint）

4. **前端 Store 拆分**：`user` → `auth` + `user` + `notification`
5. **CI 代码质量**：SpotBugs + ESLint + 依赖漏洞扫描
6. **判题结果缓存**：Redis 缓存避免重复判题

### 长期（Phase 12）

7. **微服务拆分**：从 `CrossModuleQuery/WriteRepository` 切换为 RPC 调用
8. **读写分离**：MySQL 主从复制 + 读写路由
9. **CDN 加速**：静态资源（sb3 文件、封面图）上 CDN
10. **消息队列**：事件驱动从 Spring Event 升级为 RabbitMQ/Kafka

---

## 📋 审计检查清单

- [x] 数据库索引覆盖高频查询
- [x] 线程池按用途隔离
- [x] 前端 any 类型清理
- [x] 文档归档整理
- [x] 文档导航索引创建
- [x] README 更新
- [x] Flyway 迁移脚本验证
- [x] 跨模块 SQL 审计（无散落）
- [x] 安全配置审计（无漏洞）
- [x] 事件驱动架构审计（事务边界正确）

**审计结论：项目架构健康，代码质量良好，安全防护到位。主要改进空间在测试基础设施和前端状态管理。**

---

*二次审计完成。*
