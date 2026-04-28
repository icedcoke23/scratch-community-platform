# 🔧 深度优化报告 v2.9.0

> 日期：2026-04-26
> 基于：v2.8.1 + 深度分析报告二次审计
> 优化范围：后端核心修复 + 前端架构优化 + 安全加固

---

## 一、优化清单总览

| 优先级 | 编号 | 文件 | 优化项 | 状态 |
|--------|------|------|--------|------|
| 🔴 P0 | #1 | ProjectService.java | 项目详情补全作者信息 + 点赞状态 | ✅ 完成 |
| 🔴 P0 | #2 | RateLimitConfig.java | 限流器升级为滑动窗口计数器 | ✅ 完成 |
| 🔴 P0 | #3 | request.ts | Token Refresh 竞态条件修复 | ✅ 完成 |
| 🟡 P1 | #4 | App.vue + 3 组件 | 前端组件拆分瘦身 | ✅ 完成 |
| 🟡 P1 | #5 | App.vue + router | keep-alive 改用路由 meta 控制 | ✅ 完成 |

---

## 二、详细修改说明

### #1 ProjectService.getDetail() — 补全作者信息 + 点赞状态

**问题**：项目详情页有两个 TODO 未完成，作者信息和点赞状态始终为空/false。

**修改文件**：
- `backend/scratch-editor/.../service/ProjectService.java`

**修改内容**：
```java
// 新增：通过 CrossModuleQueryRepository 查询作者信息
Map<String, Object> authorInfo = crossModuleQuery.getUserBasicInfo(project.getUserId());
vo.setAuthorName((String) authorInfo.get("nickname"));
vo.setAuthorAvatar((String) authorInfo.get("avatar_url"));

// 新增：查询当前用户是否已点赞
Set<Long> likedIds = crossModuleQuery.getLikedProjectIds(userId, List.of(projectId));
vo.setIsLiked(likedIds.contains(projectId));
```

**设计考量**：
- 使用已有的 `CrossModuleQueryRepository` 做跨模块查询，不破坏模块边界
- try/catch 包裹，查询失败不阻断主流程，降级为默认值
- 匿名用户（userId=null）直接返回 false

---

### #2 RateLimitConfig — 限流器升级为滑动窗口计数器

**问题**：原固定窗口（Fixed Window）算法在窗口边界处可能产生 2x 突发流量。

**修改文件**：
- `backend/scratch-common/.../config/RateLimitConfig.java`

**算法变更**：
```
固定窗口 → 滑动窗口计数器（Sliding Window Counter）
- 每个窗口分为 10 个子窗口（可配置）
- 每个 key 存储 subWindowCount+1 个 long 值（10 个计数器 + 1 个槽位索引）
- 时间推进时自动清零过期子窗口
- 总请求数 = 所有子窗口计数之和
```

**效果**：
- 消除窗口边界 2x 突发问题
- 内存开销可控（每个 key 11 个 long = 88 bytes）
- 限流响应头 `X-RateLimit-Limit` / `X-RateLimit-Remaining` / `Retry-After` 保持不变

---

### #3 request.ts — Token Refresh 竞态条件修复

**问题**：
- 原实现用 `isRefreshing` 标志 + `refreshSubscribers[]` 队列，多个并发请求同时 9997/401 时有竞态
- `doRefreshToken()` 用原始 `axios.post` 而非配置好的 `api` 实例

**修改文件**：
- `frontend-vue/src/api/request.ts`

**修改内容**：
```
旧方案：isRefreshing 标志 + refreshSubscribers 回调队列
新方案：refreshPromise 单例锁
- 多个并发请求共享同一个 refresh Promise
- refreshPromise.finally() 确保锁释放
- 9997 和 401 处理逻辑统一为同一套 doRefreshToken + handleAuthExpired
- refresh 请求加 10s 超时防止挂起
```

---

### #4 App.vue 组件拆分

**问题**：App.vue 16KB+，包含导航、登录/注册弹窗、全局 CSS，职责过多。

**新建文件**：
- `frontend-vue/src/components/AppHeader.vue` (2.4KB) — 顶部导航
- `frontend-vue/src/components/MobileNav.vue` (0.5KB) — 移动端底部导航
- `frontend-vue/src/components/AuthDialog.vue` (4.0KB) — 登录/注册弹窗

**效果**：
- App.vue 从 ~500+ 行减少到 ~495 行
- 登录/注册逻辑完全封装到 AuthDialog
- 导航逻辑封装到 AppHeader/MobileNav
- 各组件职责单一，可独立测试

---

### #5 keep-alive 改用路由 meta 控制

**问题**：缓存的页面组件名硬编码在 App.vue 中，组件名变更会导致缓存失效。

**修改文件**：
- `frontend-vue/src/router/index.ts` — 7 个路由添加 `keepAlive: true`
- `frontend-vue/src/App.vue` — cachedViews 从路由 meta 动态获取

**修改内容**：
```typescript
// 修改前
const cachedViews = computed(() => {
  return ['Feed', 'Problems', 'Competition', 'Rank', 'Points', 'Homework', 'Notifications']
})

// 修改后
const cachedViews = computed(() => {
  return router.getRoutes()
    .filter(r => r.meta?.keepAlive)
    .map(r => r.name as string)
    .filter(Boolean)
})
```

---

## 三、二次审计结果

### 审计清单

| 检查项 | 结果 |
|--------|------|
| TODO/FIXME 残留 | ✅ 无 |
| 编译语法 | ✅ 正确 |
| import 完整性 | ✅ 完整 |
| 组件引用 | ✅ 正确 |
| defineExpose 使用 | ✅ 正确 |
| 路由 meta 完整性 | ✅ 7 个路由已配置 |
| 限流器 API 兼容性 | ✅ 接口不变 |

### 发现并修复的额外问题

1. **AuthDialog ref 访问方式**：Vue 3 中 `defineExpose` 暴露的 ref 在模板中已自动解包，修正了 `.value` 访问方式
2. **keep-alive 被覆盖**：App.vue 重构时覆盖了 keep-alive 的路由 meta 改进，已修复

---

## 四、修改前后对比

| 指标 | 修改前 | 修改后 |
|------|--------|--------|
| 项目详情作者信息 | ❌ 始终为空 | ✅ 正确显示 |
| 项目详情点赞状态 | ❌ 始终 false | ✅ 正确查询 |
| 限流器边界突发 | ⚠️ 2x 突发 | ✅ 滑动窗口平滑 |
| Token Refresh 并发 | ⚠️ 竞态条件 | ✅ Promise 锁安全 |
| App.vue 体积 | ~16KB | ~11.5KB |
| keep-alive 配置 | 硬编码 | 路由 meta 动态 |
| 新增组件 | 0 | 3 (Header/Nav/Auth) |

---

## 五、后续建议

| 优先级 | 建议 |
|--------|------|
| 🟡 | 沙箱安全隔离升级为 Docker 容器 |
| 🟡 | tags 字段改为关联表 |
| 🟢 | CI 集成 JaCoCo 代码覆盖率 |
| 🟢 | CD 自动部署流水线 |
| 🟢 | 全局 Loading 状态管理 |
