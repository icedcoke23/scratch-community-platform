# 🔍 Scratch Community Platform — 第三轮深度分析报告

> 分析日期: 2026-04-29 | 分析范围: 全栈架构 + CI/CD + 代码质量 + 安全 + 性能

---

## 📋 概要

本次分析基于本地完整编译部署后的实际运行情况，结合源码审查，发现并修复了以下关键问题：

### 已修复问题

| # | 问题 | 严重度 | 修复方案 |
|---|------|--------|---------|
| 1 | CI 连续 4 次失败（UserServiceTest NPE） | 🔴 高 | 修正 mock 方法名 |
| 2 | Flyway 迁移冲突（2 个 V19 文件） | 🔴 高 | 合并去重，重命名 V20 |
| 3 | MySQL 8.0 不支持 `CREATE INDEX IF NOT EXISTS` | 🔴 高 | 批量移除 IF NOT EXISTS |
| 4 | V6/V9 索引名重复冲突 | 🔴 高 | 重命名冲突索引 |
| 5 | V6/V11 uk_email 唯一约束重复 | 🟡 中 | V11 改为 no-op |
| 6 | RedisRateLimit Bean 创建失败 | 🔴 高 | 移除 @ConditionalOnBean 链 |
| 7 | 积分排行榜 JOIN 性能问题 | 🟡 中 | 改用 user.points 冗余字段 |

---

## 1. CI/CD 分析

### 1.1 CI 失败根因

**失败链**: `UserServiceTest` → `login_success()` → `NullPointerException`

**根因**: 测试 mock 了 `jwtUtils.getRefreshTokenExpiry(String)` 但代码实际调用 `jwtUtils.getRefreshTokenExpiryDate()`（无参方法）。Mockito 默认返回 null，导致 NPE。

**教训**: 重构方法签名时，必须同步更新所有测试中的 mock 调用。建议使用 IDE 的 "Find Usages" 功能。

### 1.2 CI 流水线评估

```yaml
5 个 Job: backend + frontend + sandbox + docker + lint
平均耗时: ~2 分钟
并行度: backend/frontend/sandbox/lint 并行，docker 依赖 backend+sandbox
```

**优点**:
- 测试粒度好：单元测试和集成测试分离
- 安全扫描：`mvn dependency:check` + `npm audit`
- Docker 构建验证

**建议**:
- 添加代码覆盖率报告（JaCoCo + Codecov）
- 考虑添加 E2E 测试（Playwright 已有配置）
- Node.js 20 即将废弃，需升级到 Node.js 24

---

## 2. 后端架构深度分析

### 2.1 模块化设计 ⭐⭐⭐⭐

**13 个 Maven 模块**，职责清晰：
```
common-core    → 公共工具/异常/返回体/跨模块查询
common-redis   → 分布式锁/限流/缓存/调度锁
common-security → JWT/认证/角色/幂等性
common-audit   → 敏感词过滤
sb3            → SB3 解析库
judge-core     → 判题核心库
user/editor/social/judge/classroom/system → 业务模块
app            → 启动模块
```

**问题**:
- `CrossModuleQueryRepository` (485 行) 仍是 God Object，虽已标记 @Deprecated 但大量代码仍在使用
- `CrossModuleWriteRepository` 同样需要拆分

**建议**: 按模块拆分为 `SocialQueryRepository`、`EditorQueryRepository` 等

### 2.2 事件驱动架构 ⭐⭐⭐⭐

```java
// 模式: 事件发布 + 降级 fallback
try {
    eventPublisher.publishEvent(new ProjectViewEvent(this, projectId, userId));
} catch (Exception e) {
    crossModuleWrite.incrementProjectViewCount(projectId);  // 降级
}
```

**优点**: 模块解耦，异步处理
**问题**: 降级逻辑容易让开发者误解（如 CI 测试失败）
**建议**: 考虑使用 `@TransactionalEventListener` 替代手动降级

### 2.3 安全架构 ⭐⭐⭐⭐

| 措施 | 实现 | 评级 |
|------|------|------|
| JWT 认证 | 双令牌（Access + Refresh） | ✅ |
| 密码加密 | BCrypt (strength=10) | ✅ |
| 限流 | 滑动窗口计数器（Redis + 内存双模式） | ✅ |
| XSS 防护 | Jackson XSS + XssFilter | ✅ |
| 幂等性 | IdempotentInterceptor | ✅ |
| 敏感词 | DFA 自动机 | ✅ |
| 路径遍历 | FileUploadUtils 检查 | ✅ |

**问题**:
- CORS 配置 `http://localhost:*` 在生产环境应移除
- Token 黑名单依赖 Redis，Redis 不可用时降级策略不明确

### 2.4 数据库设计 ⭐⭐⭐⭐

**21 张表**，Flyway 版本管理（V1-V19）。

**亮点**:
- 逻辑删除全局统一
- 乐观锁（@Version）保护并发更新
- 全文索引支持搜索
- Caffeine 本地缓存高频查询

**问题**:
- 积分排行榜原来用 JOIN+SUM 聚合 point_log，但 user.points 已有冗余字段（已修复）
- notification 表数据量会快速增长，考虑按时间分区
- JSON 字段（homework.problem_ids, problem.options）缺少索引

### 2.5 异步判题设计 ⭐⭐⭐

```java
@Async("judgeExecutor")
public void judgeAsync(Long submissionId) { ... }
```

**问题**:
- `Thread.sleep(1000L * attempt)` 阻塞线程池线程
- 没有判题队列，高并发时线程池可能耗尽
- 缺少判题超时的精确控制

**建议**: 使用 `@Retryable` + Spring Event 替代手动重试

---

## 3. 前端架构分析

### 3.1 技术栈 ⭐⭐⭐⭐

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5.32 | 框架 |
| TypeScript | - | 类型安全 |
| Vite | 8.0.10 | 构建工具 |
| Element Plus | 2.13.7 | UI 组件 |
| Pinia | - | 状态管理 |
| Vitest | - | 测试框架 |
| Playwright | - | E2E 测试 |

### 3.2 亮点

- **Token 自动刷新**: `request.ts` 实现了防并发的 Refresh Token 机制
- **请求取消管理**: AbortController 管理，页面切换时自动取消
- **PWA 支持**: Service Worker + manifest.json
- **组件化**: 20+ 个通用组件
- **测试覆盖**: 14 个测试文件，166 个测试

### 3.3 问题

- **Token 刷新竞态**: `isRetryingPending` 标志在 async 函数中被提前清除
- **类型安全**: `api/index.ts` 的 `get/post/put/del` 泛型参数类型为 `unknown`，丢失具体类型
- **重复代码**: 多个 API 模块（user.ts, social.ts 等）的函数签名高度相似
- **i18n**: `useI18n.ts` (584 行) 是最大的 composables，翻译数据硬编码

### 3.4 建议

- 引入 OpenAPI 代码生成，保证 API 类型安全
- i18n 翻译字典外置到 JSON 文件
- 考虑引入 TanStack Query 管理服务端状态
- 组件按功能域分组（目前全部平铺在 components/）

---

## 4. 沙箱判题系统

### 4.1 设计

- Node.js 独立进程
- scratch-vm headless 模式
- REST API: `POST /judge`
- 资源限制: 内存 1.5GB

### 4.2 安全风险

- 没有 CPU 限制和 cgroup 配置
- Scratch 代码可能包含无限循环
- 沙箱进程与后端在同一 Docker 网络

### 4.3 建议

- 添加 cgroup 资源限制
- 使用 gVisor 或 Firecracker 实现更强隔离
- 添加判题结果缓存

---

## 5. 性能优化

### 5.1 已实施

| 优化 | 影响 |
|------|------|
| 积分排行榜: JOIN+SUM → 直接查询 user.points | 查询从 O(N) 降到 O(1)，N=point_log 行数 |
| Caffeine 本地缓存 (getUserBasicInfo) | 高频跨模块查询命中本地缓存 |
| 滑动窗口限流 (Redis Lua) | 单次限流检查 ~1ms |

### 5.2 建议

- Feed 流查询: 考虑 Redis 缓存热门结果
- 项目详情: 批量预加载作者信息（避免 N+1）
- 前端: 路由懒加载（检查是否已配置）
- 数据库: 考虑读写分离（高并发场景）

---

## 6. 文档质量 ⭐⭐⭐⭐⭐

项目文档非常丰富：
- 20+ 个 Markdown 文档
- ER 图（Mermaid 格式）
- 踩坑记录 (104 条坑 + 129 条经验)
- 架构决策记录 (ADR)
- 编码规范
- 部署指南

**建议**: 文档过多可能导致维护负担，考虑合并和精简。

---

## 7. 综合评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 模块化清晰，事件驱动解耦 |
| 代码质量 | ⭐⭐⭐⭐ | 规范良好，注释详尽 |
| 测试覆盖 | ⭐⭐⭐⭐ | 166 个前端测试 + 后端单元测试，CI 通过 |
| 安全性 | ⭐⭐⭐⭐ | 多层防护，JWT 双令牌 + 限流 + XSS |
| 性能 | ⭐⭐⭐⭐ | Caffeine 缓存 + 滑动窗口 + 乐观锁 |
| 文档 | ⭐⭐⭐⭐⭐ | 极其丰富 |
| CI/CD | ⭐⭐⭐⭐ | 5 Job 流水线，已修复全部失败 |
| **综合** | **⭐⭐⭐⭐** | **优秀的全栈项目，架构合理，代码规范** |

---

## 8. 后续优化计划

### 高优先级
1. ✅ CI 修复（已完成）
2. ✅ Flyway 迁移冲突（已修复）
3. ✅ RedisRateLimit Bean 问题（已修复）
4. ✅ 积分排行榜性能优化（已完成）
5. 📋 CrossModuleQueryRepository 拆分
6. 📋 前端 API 类型安全增强

### 中优先级
7. 📋 引入 Testcontainers 支持集成测试
8. 📋 判题队列化（Redis Stream）
9. 📋 通知表分区
10. 📋 i18n 翻译字典外置

### 低优先级
11. 📋 代码覆盖率报告
12. 📋 E2E 测试完善
13. 📋 Node.js 24 升级
14. 📋 文档精简合并
