# 修复计划 — Scratch Community Platform

## 修复总览

| # | 问题 | 严重性 | 预估工时 | 状态 |
|---|------|--------|----------|------|
| 1 | JWT 密钥默认值危险 | 🔴 P0 | 1h | ⏳ 待修复 |
| 2 | MinIO 凭证未校验 | 🔴 P0 | 0.5h | ⏳ 待修复 |
| 3 | 事务管理缺失 | 🔴 P0 | 3h | ⏳ 待修复 |
| 4 | CI 测试失败（2 个） | 🔴 P0 | 1h | ⏳ 待修复 |
| 5 | 限流器固定窗口 | 🟡 P1 | 3h | ⏳ 待修复 |
| 6 | 外键约束缺失 | 🟡 P1 | 4h | ⏳ 待修复 |
| 7 | Token 刷新竞态 | 🟡 P1 | 2h | ⏳ 待修复 |
| 8 | 文件病毒扫描 | 🟡 P1 | 2h | ⏳ 待修复 |
| 9 | 日志敏感信息 | 🟡 P1 | 1h | ⏳ 待修复 |
| 10 | Thread.sleep 阻塞 | 🟡 P1 | 2h | ⏳ 待修复 |

**总预估**: 19.5 小时（2.5 个工作日）

---

## Phase 1 - 安全修复（本周）

### 修复 1：JWT 密钥强制环境变量

**文件**: `backend/scratch-app/src/main/resources/application.yml`

```diff
-    secret: ${JWT_SECRET:scratch-community-secret-key-at-least-32bytes-long!!}
-    refresh-secret: ${JWT_REFRESH_SECRET:scratch-community-refresh-secret-key-at-least-32b}
+    secret: ${JWT_SECRET:}
+    refresh-secret: ${JWT_REFRESH_SECRET:}
```

**新增校验** (JwtUtils.java):
```java
@PostConstruct
public void validate() {
    Assert.hasText(secret, "JWT_SECRET 未配置，请通过环境变量设置");
    Assert.hasText(refreshSecret, "JWT_REFRESH_SECRET 未配置");
    Assert.isTrue(secret.length() >= 32, "JWT_SECRET 至少 32 字节");
}
```

---

### 修复 2：MinIO 凭证校验

**文件**: `MinioConfig.java`

```java
@PostConstruct
public void validateCredentials() {
    Assert.hasText(accessKey, "MINIO_ROOT_USER 未配置");
    Assert.hasText(secretKey, "MINIO_ROOT_PASSWORD 未配置");
}
```

---

### 修复 3：数据库事务补充

**目标**:
- `ConfigService.updateConfig()`
- `NotifyService.markAllRead()`
- `NotifyService.updateNotification()`
- `AuditService.updateAuditLog()`

**操作**: 添加 `@Transactional(rollbackFor = Exception.class)`

---

### 修复 4：CI 测试修复

**问题**:
- `SocialServiceTest` - 4 个测试失败
- `HomeworkServiceTest` - 编译错误

**修复**:
- 添加 `@MockBean ApplicationEventPublisher`
- 更新 `grade()` 方法调用为新签名

---

## Phase 2 - 架构优化（下周）

### 修复 5：滑动窗口限流器

**方案**: 评估并启用 `RedisRateLimiter` 替换固定窗口

---

### 修复 6：外键约束

**步骤**: 数据清洗 → 添加外键 → 更新文档

---

### 修复 7：Token 刷新竞态

**文件**: `frontend-vue/src/api/request.ts`

应用 `isRetryingPending` 标志 + 延迟清除 `refreshPromise`

---

## 执行顺序

1. ✅ 安全修复（P0）
2. ✅ CI 修复（P0）
3. ✅ 事务修复（P0）
4. ⏳ 限流器/外键（P1）
5. ⏳ 前端优化（P2）

---

**总预估**: 19.5 小时  
**负责人**: 小跃  
**截止**: 2026-05-07（分 3 批 PR）
