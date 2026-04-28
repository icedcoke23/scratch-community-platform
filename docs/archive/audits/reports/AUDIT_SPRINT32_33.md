# Sprint 32/33 审计报告

> 审计日期: 2026-04-28
> 审计人: AI 助手
> 审计范围: Sprint 32 (日志聚合/CI-CD/OAuth2/推荐算法/学情看板/安全加固) + Sprint 33 (协作编辑/移动端App)

---

## 一、审计发现汇总

| # | 问题 | 严重性 | 影响范围 | 状态 |
|---|------|--------|----------|------|
| 1 | Flyway 迁移版本冲突 (V11, V13) | 🔴 高 | 服务启动失败 | ✅ 已修复 |
| 2 | RecommendationService 引用不存在的实体 | 🔴 高 | 编译失败 | ✅ 已修复 |
| 3 | WebSocket 中 ThreadLocal 失效 | 🔴 高 | 协作功能不可用 | ✅ 已修复 |
| 4 | SockJS 不支持自定义 Header | 🟡 中 | WebSocket 连接失败 | ✅ 已修复 |
| 5 | OAuthService substring 越界 | 🟡 中 | 第三方登录异常 | ✅ 已修复 |
| 6 | @Transactional 内广播 WebSocket | 🟡 中 | 状态不一致风险 | ⚠️ 已知 |
| 7 | 前端 WebSocket 依赖缺失 | 🟡 中 | 构建失败 | ✅ 已修复 |
| 8 | WebSocket 连接时序问题 | 🟡 中 | 协作连接不建立 | ✅ 已修复 |
| 9 | SQL 迁移注释版本号错误 | 🟢 低 | 无功能影响 | ✅ 已修复 |

---

## 二、修复详情

### 2.1 Flyway 迁移版本冲突

- `V11__oauth_login.sql` → `V15__oauth_login.sql`
- `V13__collab_editing.sql` → `V16__collab_editing.sql`
- 更新 SQL 注释中的版本号

### 2.2 RecommendationService 实体修复

- `LikeRecord` → `ProjectLike`
- `LikeRecordMapper` → `ProjectLikeMapper`
- `getTargetId()` → `getProjectId()`
- 移除未使用的 `UserMapper` 依赖

### 2.3 WebSocket 安全修复

- 重写 `WebSocketConfig`：添加 JWT 握手处理器 + STOMP 消息拦截器
- 重写 `CollabWebSocketController`：从 `Principal` 获取 userId，不再依赖 `LoginUser`
- 前端通过 URL query 参数传递 token（SockJS 限制）

### 2.4 OAuthService 安全修复

- `substring(0, 8)` 前检查 openId 长度

### 2.5 前端依赖补全

- 添加 `@stomp/stompjs`、`sockjs-client`、`@types/sockjs-client`

### 2.6 WebSocket 连接时序修复

- 移除 `onMounted` 中的自动连接
- 添加 `watch` 监听 sessionId 变化自动连接

---

## 三、代码质量评估

### 后端 (Java)

| 模块 | 文件数 | 质量 | 备注 |
|------|--------|------|------|
| OAuth2 登录 | 10 | ✅ 良好 | 接口抽象合理，支持多平台扩展 |
| 协作编辑 | 10 | ✅ 良好 | 乐观锁设计合理，冲突检测完整 |
| 推荐算法 | 1 | ✅ 良好 | 协同过滤+热度+时间衰减混合策略 |
| 学情看板 | 1 | ✅ 良好 | 多维度分析，含预警机制 |

### 前端 (Vue/TS)

| 模块 | 文件数 | 质量 | 备注 |
|------|--------|------|------|
| OAuth2 组件 | 3 | ✅ 良好 | 弹窗+回调双模式 |
| 协作组件 | 5 | ✅ 良好 | composable 封装合理 |
| WebSocket | 1 | ✅ 良好 | STOMP over SockJS，自动重连 |

### 移动端 (uni-app)

| 模块 | 文件数 | 质量 | 备注 |
|------|--------|------|------|
| App 骨架 | 9 | ✅ 良好 | 复用后端 API，4 个核心页面 |

---

## 四、已知遗留问题

### 4.1 @Transactional 内 WebSocket 广播

**严重性**: 🟡 中
**影响**: 事务回滚时 WebSocket 消息已发出，客户端状态可能不一致
**建议**: 使用 `TransactionSynchronization.afterCommit()` 延迟广播
**优先级**: P2（当前场景影响小，高并发时需处理）

### 4.2 协作编辑并发性能

**严重性**: 🟢 低
**影响**: 内存中的版本号和操作历史在多实例部署时无法共享
**建议**: 使用 Redis 存储会话版本号和操作历史
**优先级**: P3（单实例部署无影响）

### 4.3 OAuth Token 安全存储

**严重性**: 🟡 中
**影响**: accessToken/refreshToken 明文存储在数据库
**建议**: 加密存储或使用 Vault
**优先级**: P2

---

## 五、文件变更清单

### 新增文件 (25 个)

```
backend/
  scratch-common-core/.../config/WebSocketConfig.java
  scratch-social/.../entity/CollabSession.java
  scratch-social/.../entity/CollabParticipant.java
  scratch-social/.../mapper/CollabSessionMapper.java
  scratch-social/.../mapper/CollabParticipantMapper.java
  scratch-social/.../dto/CollabMessage.java
  scratch-social/.../dto/EditOperation.java
  scratch-social/.../dto/CollabEvent.java
  scratch-social/.../service/CollabService.java
  scratch-social/.../service/RecommendationService.java
  scratch-social/.../controller/CollabController.java
  scratch-social/.../controller/CollabWebSocketController.java
  scratch-user/.../entity/UserOAuth.java
  scratch-user/.../mapper/UserOAuthMapper.java
  scratch-user/.../dto/OAuthLoginDTO.java
  scratch-user/.../vo/OAuthCallbackVO.java
  scratch-user/.../oauth/OAuthProvider.java
  scratch-user/.../oauth/OAuthUserInfo.java
  scratch-user/.../oauth/WeChatOAuthProvider.java
  scratch-user/.../oauth/QQOAuthProvider.java
  scratch-user/.../service/OAuthService.java
  scratch-user/.../service/TeacherDashboardService.java
  scratch-user/.../controller/OAuthController.java
  scratch-app/.../db/migration/V15__oauth_login.sql
  scratch-app/.../db/migration/V16__collab_editing.sql

frontend-vue/
  src/api/oauth.ts
  src/api/collab.ts
  src/components/OAuthButtons.vue
  src/components/CollabToolbar.vue
  src/components/CollabCursors.vue
  src/components/CollabChat.vue
  src/views/OAuthCallbackView.vue
  src/views/collab/CollabEditorView.vue
  src/composables/useCollabWebSocket.ts

mobile/
  manifest.json, pages.json, App.vue, main.js
  src/api/index.js
  src/pages/index/index.vue
  src/pages/project/detail.vue
  src/pages/rank/rank.vue
  src/pages/user/profile.vue

docker/
  loki-config.yml, promtail-config.yml
  grafana/datasources/loki.yml
  grafana/dashboards/logs.json

docs/
  SANDBOX_SECURITY.md
  COLLABORATIVE_EDITING.md
  MOBILE_APP_PLAN.md
  PITFALLS_SPRINT32_33.md

sandbox/
  sandbox-seccomp.json

.github/workflows/
  deploy.yml
```

### 修改文件 (6 个)

```
backend/scratch-app/pom.xml                         (添加 WebSocket 依赖)
backend/.../ErrorCode.java                          (添加 THIRD_PARTY_AUTH_FAILED)
backend/.../application.yml                         (添加 OAuth 配置)
frontend-vue/package.json                           (添加 STOMP/SockJS 依赖)
frontend-vue/src/router/index.ts                    (添加 OAuth/协作路由)
docs/DEV_PLAN.md                                    (更新 Sprint 状态)
```
