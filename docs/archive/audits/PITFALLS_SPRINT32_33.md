# 踩坑文档 - Sprint 32/33 审计修复

> 日期: 2026-04-28
> 审计范围: Sprint 32 (日志聚合/CI-CD/OAuth2/推荐算法/学情看板/安全加固) + Sprint 33 (协作编辑/移动端App)

---

## 一、Flyway 迁移版本冲突

### 问题

多个迁移文件使用了相同的版本号，导致 Flyway 启动失败：

```
V11__email_unique.sql  ← 已存在
V11__oauth_login.sql   ← 新增，冲突！

V13__performance_indexes.sql  ← 已存在
V13__collab_editing.sql       ← 新增，冲突！
```

### 原因

新增迁移文件时未检查现有版本号，直接使用了 V10/V12/V13 等已被占用的版本。

### 修复

- `V11__oauth_login.sql` → `V15__oauth_login.sql`
- `V13__collab_editing.sql` → `V16__collab_editing.sql`

### 教训

**新增 Flyway 迁移前必须先检查现有版本号**：

```bash
ls backend/scratch-app/src/main/resources/db/migration/ | sort -t'V' -k2 -n
```

使用 `V{max+1}` 作为新版本号。建议在 DEV_PLAN.md 中记录已使用的版本号。

---

## 二、实体类名不匹配

### 问题

`RecommendationService` 引用了不存在的类：

```java
import com.scratch.community.module.social.entity.LikeRecord;      // ❌ 不存在
import com.scratch.community.module.social.mapper.LikeRecordMapper; // ❌ 不存在
```

实际实体是 `ProjectLike` / `ProjectLikeMapper`，且字段名不同：
- `LikeRecord.getTargetId()` → `ProjectLike.getProjectId()`
- `LikeRecord.getUserId()` → `ProjectLike.getUserId()`

### 原因

编写推荐算法时假设了实体名称，未检查实际代码。

### 修复

- 将所有 `LikeRecord` 替换为 `ProjectLike`
- 将 `LikeRecordMapper` 替换为 `ProjectLikeMapper`
- 将 `getTargetId()` 替换为 `getProjectId()`

### 教训

**引用实体前必须确认实际类名和字段名**：

```bash
find backend -name "*Like*.java" -path "*/entity/*"
grep -n "private.*Id" backend/.../entity/ProjectLike.java
```

---

## 三、WebSocket 中 ThreadLocal 失效

### 问题

`CollabWebSocketController` 使用 `LoginUser.getUserId()` 获取当前用户 ID：

```java
Long userId = LoginUser.getUserId(); // ❌ WebSocket 线程中返回 null
```

`LoginUser` 基于 `ThreadLocal` 存储用户信息，但 WebSocket 消息处理运行在独立的 WebSocket 线程池中，不是 HTTP 请求线程，所以 ThreadLocal 没有值。

### 原因

WebSocket 和 HTTP 请求使用不同的线程池，ThreadLocal 不跨线程传播。

### 修复

1. **WebSocket 握手时**：从 JWT 解析 userId，存入 session attributes
2. **STOMP 消息拦截器**：从 session attributes 恢复 userId 到 Principal
3. **WebSocket 控制器**：从 `Principal` 参数获取 userId

```java
// 修复前
Long userId = LoginUser.getUserId(); // null

// 修复后
@MessageMapping("/collab/{sessionId}/join")
public void joinSession(@DestinationVariable Long sessionId,
                        @Payload CollabMessage message,
                        Principal principal) {
    Long userId = resolveUserId(principal); // 从 Principal 获取
}
```

### 教训

**WebSocket 中不能使用 ThreadLocal**。Spring STOMP 的正确做法：

1. 握手拦截器 (`HandshakeHandler`) 提取认证信息，存入 session attributes
2. 消息拦截器 (`ChannelInterceptor`) 从 session attributes 设置 Principal
3. 控制器方法注入 `Principal` 参数获取用户身份

---

## 四、SockJS 不支持自定义 Header

### 问题

前端 STOMP 客户端尝试通过 `connectHeaders` 传递 JWT：

```typescript
client = new Client({
  webSocketFactory: () => new SockJS('/ws-collab'),
  connectHeaders: {
    Authorization: `Bearer ${token}`, // ❌ SockJS 不支持
  },
})
```

SockJS 的 HTTP 降级模式（`/info`、`/xhr` 等端点）不支持自定义请求头。

### 修复

通过 URL query 参数传递 token：

```typescript
// 前端
webSocketFactory: () => new SockJS(`/ws-collab?token=${userStore.token}`)

// 后端握手处理器
String query = request.getURI().getQuery();
if (query != null) {
    for (String param : query.split("&")) {
        if (param.startsWith("token=")) {
            return param.substring(6);
        }
    }
}
```

### 教训

**SockJS 环境下不能用 HTTP header 传 token**，必须用 query 参数。如果是原生 WebSocket（不用 SockJS），可以用 header。

---

## 五、substring 越界风险

### 问题

`OAuthService.registerFromOAuth` 中：

```java
String baseUsername = oauthInfo.getProvider() + "_" + oauthInfo.getOpenId().substring(0, 8);
```

如果 `openId` 长度小于 8，会抛出 `StringIndexOutOfBoundsException`。

### 修复

```java
String suffix = openId.length() >= 8 ? openId.substring(0, 8) : openId;
String baseUsername = oauthInfo.getProvider() + "_" + suffix;
```

### 教训

**substring 前必须检查长度**，特别是外部数据（第三方 API 返回值）。

---

## 六、Homework 实体字段假设

### 问题

`TeacherDashboardService` 查询 `Homework::getDeleted`，但最初审计时怀疑 `Homework` 没有 `deleted` 字段。

### 实际情况

`Homework` 实体在 line 71-72 确实有 `deleted` 字段（带 `@TableLogic` 注解）。初次审计时 `head -15` 截断了输出，导致误判。

### 教训

**检查实体字段时要读完整文件**，不能只看前 N 行。`@TableLogic` 注解的字段通常在文件末尾。

---

## 七、@Transactional 内广播 WebSocket 消息

### 问题

`CollabService` 的多个方法在 `@Transactional` 内直接调用 `messagingTemplate.convertAndSend()`：

```java
@Transactional
public void leaveSession(Long sessionId, Long userId) {
    // ... 数据库操作 ...
    broadcastEvent(sessionId, CollabEvent.of("user_left", ...)); // 事务未提交就广播了
}
```

如果事务回滚，WebSocket 消息已经发出去了，导致客户端和服务端状态不一致。

### 影响

当前场景下影响较小（WebSocket 操作本身是幂等的），但在高并发场景可能导致问题。

### 后续优化

使用 `TransactionSynchronizationManager.registerSynchronization()` 在事务提交后广播：

```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            broadcastEvent(sessionId, event);
        }
    }
);
```

---

## 八、前端 WebSocket 依赖缺失

### 问题

`useCollabWebSocket.ts` 引用了 `@stomp/stompjs` 和 `sockjs-client`，但 `package.json` 中未声明这些依赖。

### 修复

添加到 `dependencies`：

```json
"@stomp/stompjs": "^7.0.0",
"sockjs-client": "^1.6.1"
```

添加到 `devDependencies`：

```json
"@types/sockjs-client": "^1.5.4"
```

### 教训

**引入新库时同步更新 package.json**，不要依赖隐式安装。

---

## 九、useCollabWebSocket 时序问题

### 问题

`useCollabWebSocket` 在 `onMounted` 中自动连接，但 `sessionId` 是异步获取的。composable 的 `onMounted` 先于父组件的 `onMounted` 执行，此时 `sessionId()` 返回 null，连接不会建立。

### 修复

使用 `watch` 监听 `sessionId` 变化，自动重连：

```typescript
watch(() => sessionId(), (newId, oldId) => {
  if (newId && newId !== oldId) {
    if (client) disconnect()
    connect()
  }
})
```

### 教训

**异步依赖的数据不要在 onMounted 中直接使用**，用 watch 监听变化更可靠。

---

## 总结

| 问题 | 严重性 | 状态 |
|------|--------|------|
| Flyway 版本冲突 | 🔴 高 | ✅ 已修复 |
| 实体类名不匹配 | 🔴 高 | ✅ 已修复 |
| WebSocket ThreadLocal 失效 | 🔴 高 | ✅ 已修复 |
| SockJS 不支持自定义 Header | 🟡 中 | ✅ 已修复 |
| substring 越界 | 🟡 中 | ✅ 已修复 |
| @Transactional 内广播 | 🟡 中 | ⚠️ 已知，后续优化 |
| 前端依赖缺失 | 🟡 中 | ✅ 已修复 |
| 时序问题 | 🟡 中 | ✅ 已修复 |
