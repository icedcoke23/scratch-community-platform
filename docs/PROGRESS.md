# 开发进度

> 最后更新：2026-04-30 | 当前版本: v3.7.0

---

## 总览

| Phase | 名称 | 版本 | 状态 | 日期 |
|-------|------|------|------|------|
| 1 | MVP 核心闭环 | v0.1 | ✅ | 2026-04-23 |
| 2 | 教学闭环增强 | v0.5 | ✅ | 2026-04-24 |
| 3 | 增强体验 | v1.0 | ✅ | 2026-04-24 |
| 3.5 | Vue 3 前端迁移 | v1.5 | ✅ | 2026-04-24 |
| 3.6-3.7 | 全面深度优化 | v2.0 | ✅ | 2026-04-24 |
| 4 | 安全与架构 | v2.3 | ✅ | 2026-04-25 |
| 5 | 体验增强 | v2.5 | ✅ | 2026-04-25 |
| 6 | 架构深度优化 | v2.7 | ✅ | 2026-04-25 |
| 7 | Vue 3 前端全面建设 | v2.8 | ✅ | 2026-04-25 |
| 8 | 质量加固 | v3.0 | ✅ | 2026-04-25 |
| 9 | 生产就绪 | v3.1 | ✅ | 2026-04-28 |
| 10 | CI 修复 + 文档整理 | v3.2.1 | ✅ | 2026-04-28 |
| 11 | 全面架构优化 | v3.4.0 | ✅ | 2026-04-28 |
| 12 | 深度优化 | v3.5.0 | ✅ | 2026-04-28 |
| 13 | 可观测性与韧性 | v3.6.0 | ✅ | 2026-04-29 |

---

## 关键里程碑

### v3.6.0 (2026-04-29) — 可观测性与韧性
- TraceId 链路追踪: 每个请求生成唯一 TraceId (MDC)，日志自动携带
- 线程池 Micrometer 监控: 3 个线程池指标注册到 Prometheus (active/pool/queue/completed)
- Resilience4j 熔断器: 判题沙箱调用增加熔断保护 (滑动窗口 + 半开探测)
- @Idempotent 扩展: ProjectController create/publish/remix 增加幂等性保护
- ErrorCode 增强: 新增 RATE_LIMITED/RESOURCE_NOT_FOUND/METHOD_NOT_ALLOWED/IDEMPOTENT_CONFLICT
- init.sql 修复: 移除 MySQL 8.0 不支持的语法和重复索引
- CI Node 版本统一: 20 → 22 (ci.yml + deploy.yml + Dockerfile.frontend)
- 第三轮深度审计报告

### v3.2.1 (2026-04-28) — CI 修复 + 文档整理
- Dockerfile.backend 模块引用修复（CI Docker Build 通过）
- init.sql 同步 V8-V18 Flyway 迁移（3 张新表 + 5 个字段 + 20+ 索引）
- 文档体系整理（冗余归档、索引重构、TODO 精简）
- 前端 request.ts 死代码清理

### v3.1.0 (2026-04-28) — 生产就绪
- E2E 测试框架 (Playwright) + 性能压测 (k6)
- Docker Compose Prod + Nginx 生产配置
- Prometheus + Grafana 监控仪表板
- 健康检查增强 (JVM/OS/内存)

### v3.0.0 (2026-04-25) — 质量加固
- ProjectService 补全作者信息 + 点赞状态
- Token Refresh 后端接口
- ScratchEditorView i18n 全覆盖

### v2.8.0 (2026-04-25) — CI 修复 + 深度分析
- SocialServiceTest 事件驱动测试修复
- HomeworkServiceTest 方法拆分同步
- 全栈深度分析报告

### v2.7.0 (2026-04-25) — 架构深度优化
- fastjson2 → Jackson 全面迁移
- 乐观锁 (@Version) 4 个 Entity
- 关联表 + 性能索引 (V12-V18)
- SSE 一次性 Token 机制

### v2.5.0 (2026-04-25) — 体验增强
- Scratch 在线编辑器 (TurboWarp iframe)
- PWA (Service Worker + manifest)
- 全文搜索 + 数据看板
- i18n 国际化 (150+ 翻译键)

### v2.0.0 (2026-04-24) — 安全与架构
- 沙箱进程隔离 (Node.js fork)
- 敏感词过滤 (DFA 自动机)
- 分布式锁 (Redisson)
- 滑动窗口限流 (Redis Lua)
- 事件驱动解耦 (Spring Event)

### v1.0.0 (2026-04-24) — 增强体验
- AI 点评 (LLM + 规则引擎降级)
- 积分体系 (5 种规则 + 等级)
- Remix (Fork) 功能
- 学情分析

### v0.1.0 (2026-04-23) — MVP
- 用户系统 / 创作引擎 / 判题系统 / 社区互动 / 教学管理 / 竞赛系统

---

## 审计记录

| 日期 | 审计轮次 | 综合评分 | 关键发现 |
|------|---------|---------|---------|
| 2026-04-23 | 初始审计 | — | 项目结构分析 |
| 2026-04-24 | 3-21 轮 | 7.4→8.0 | JWT 安全 / N+1 查询 / XSS / 索引优化 |
| 2026-04-24 | 22-25 轮 | 8.0→8.5 | DFA 类型安全 / 读写分离 / 限流器内存泄漏 |
| 2026-04-25 | 26-30 轮 | 8.5→8.8 | fastjson2 迁移 / 乐观锁 / SSE Token / CI 修复 |
| 2026-04-28 | 31 轮 | 8.8→8.9 | CI Docker Build 修复 / init.sql 同步 / 文档整理 |
| 2026-04-28 | 32 轮 (v3.4.0) | 8.9→9.1 | V19 索引 / Token 竞态修复 / 路径遍历防护 / RateLimit 注解 |
| 2026-04-28 | 33 轮 (v3.5.0) | 9.1→9.3 | Feed 时间衰减 / Caffeine 缓存 / ErrorBoundary / 类型安全 |
| 2026-04-29 | 34 轮 (v3.6.0) | 9.3→9.5 | TraceId / 线程池监控 / Resilience4j 熔断器 / 幂等性扩展 / ErrorCode 增强 |

---

> 详细的 Sprint 记录请查看 [archive/audits/](archive/audits/) 中的历次审计报告。
