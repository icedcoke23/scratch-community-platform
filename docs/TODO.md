# 开发待办

> 最后更新：2026-04-28 | 当前版本: v3.5.0

---

## ✅ 已完成（Phase 1-10, Sprint 1-30）

<details>
<summary>点击展开历史 Sprint 记录</summary>

- **Phase 1-3** (Sprint 1-14): MVP 核心闭环 → 教学增强 → AI 点评 / 积分 / Remix / 移动端
- **Phase 3.5** (Sprint 15-28): Vue 3 前端全面建设 — 14 页面 / i18n / PWA / 深色模式 / 166 测试
- **Phase 4** (v2.0): 沙箱隔离 / 敏感词 / 分布式锁 / 限流 / 事件驱动解耦
- **Phase 5** (v2.3): SSE Token / 幂等性 / 乐观锁 / JWT 双令牌
- **Phase 6** (v2.5): Scratch 编辑器 / PWA / 数据看板 / 全文搜索
- **Phase 7** (v2.6-v2.7): 关联表 / 缓存 / 安全加固 / fastjson2→Jackson 迁移
- **Phase 8** (Sprint 29-30): CI 修复 / 集成测试 / E2E 框架 / 质量加固
- **Phase 9** (v3.0-v3.2): 生产就绪 — Docker Compose Prod / Nginx / Grafana / k6 / 安全深度优化
- **Phase 10** (v3.3.0): 二次深度优化 — Role 枚举 / Redis 限流修复 / AI 缓存 / WebSocket 重连

</details>

---

## 📋 当前待办

### 🔴 高优先级

| # | 任务 | 说明 | 状态 |
|---|------|------|------|
| 1 | Testcontainers 集成测试 | 用 `@Container` 自动管理 Redis/MySQL，让 `scratch-app` 的集成测试可在 CI 运行 | 📋 |
| 2 | Refresh Token httpOnly Cookie | 将 Refresh Token 改为 httpOnly + Secure + SameSite=Strict Cookie，防止 XSS 窃取 | 📋 |
| 3 | 判题回调机制 | sandbox 判题完成后通过 HTTP 回调或 Redis Pub/Sub 通知后端，替代轮询 | 📋 |

### 🟡 中优先级

| # | 任务 | 说明 | 状态 |
|---|------|------|------|
| 4 | OpenAPI 代码生成 | 前端 API 层引入 openapi-typescript-codegen，保证类型安全 | 📋 |
| 5 | notification 表分区 | 数据会快速增长，按月分区或定期归档 | 📋 |
| 6 | 判题队列 | 引入 Redis Stream 实现判题排队，避免线程池耗尽 | 📋 |
| 7 | competition JSON 清理 | 删除 `problem_ids` / `problem_scores` JSON 字段，统一用 `competition_problem` 关联表 | 📋 |
| 8 | CrossModuleWriteRepository 拆分 | 按模块拆分为 `ProjectCountWriter`、`PointWriter` 等小类 | 📋 |

### 🟢 低优先级

| # | 任务 | 说明 | 状态 |
|---|------|------|------|
| 9 | HTTPS 证书自动化 | Let's Encrypt certbot | 📋 |
| 10 | 日志聚合 | ELK / Loki 集成 | 📋 |
| 11 | CI/CD 完整部署流水线 | GitHub Actions → VPS 自动部署 | 📋 |
| 12 | 前端 Token 改为内存存储 | Access Token 存 Pinia state，不持久化到 sessionStorage | 📋 |
| 13 | 推荐算法 | 基于用户行为的个性化项目推荐 | 📋 |

---

## 📊 项目当前状态

| 维度 | 状态 | 说明 |
|------|------|------|
| 后端测试 | ✅ 7/7 | 单元测试全部通过 |
| 前端测试 | ✅ 166/166 | 14 个测试文件全部通过 |
| TypeScript | ✅ 0 错误 | vue-tsc 类型检查通过 |
| 前端构建 | ✅ 通过 | Vite 8 + Rolldown |
| CI/CD | ✅ 全部通过 | Backend + Frontend + Sandbox + Docker + Lint |
| 踩坑记录 | 📝 104 条 | 129 条经验总结 |
| Flyway 迁移 | 📦 19 个 | V1-V19 版本化迁移 |
| 文档版本 | v3.5.0 | 2026-04-28 |
