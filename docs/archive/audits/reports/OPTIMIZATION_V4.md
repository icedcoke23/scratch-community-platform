# 🔧 深度优化报告 v4.0

> 日期：2026-04-28
> 基于：全面深度分析后的系统性优化

---

## 一、优化清单总览

| 优先级 | 编号 | 优化项 | 状态 |
|--------|------|--------|------|
| 🔴 P0 | #1 | 分页大小限制（防恶意大分页） | ✅ 完成 |
| 🔴 P0 | #2 | JWT 双令牌体系（Access + Refresh Token） | ✅ 完成 |
| 🔴 P0 | #3 | 数据库索引补充（V17 迁移） | ✅ 完成 |
| 🟡 P1 | #4 | 前端 Token 恢复验证 | ✅ 完成 |
| 🟡 P1 | #5 | 前端请求拦截器增强（Refresh Token 优先） | ✅ 完成 |
| 🟡 P1 | #6 | ErrorCode 扩展（Refresh Token 错误码） | ✅ 完成 |
| 🟡 P1 | #7 | 登录/注册返回 Refresh Token | ✅ 完成 |
| 🟡 P1 | #8 | 登出清除 Refresh Token | ✅ 完成 |

---

## 二、详细修改说明

### 2.1 分页大小限制

**文件**: `MybatisPlusConfig.java`

```java
PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
paginationInterceptor.setMaxLimit(100L); // 新增：限制单页最大 100 条
```

**作用**: 防止恶意大分页查询（如 `?size=10000`），避免数据库全表扫描。

---

### 2.2 JWT 双令牌体系

**核心变更**:

| 文件 | 变更 |
|------|------|
| `JwtUtils.java` | 新增 `generateRefreshToken()`, `validateRefreshToken()`, `getUserIdFromRefreshToken()`, `getRefreshTokenExpiry()` |
| `LoginVO.java` | 新增 `refreshToken` 字段 |
| `User.java` (Entity) | 新增 `refreshToken`, `refreshTokenExpiresAt` 字段 |
| `UserService.java` | 登录/注册同时生成 Refresh Token 并存储到数据库 |
| `UserController.java` | refresh 端点改为使用 Refresh Token，支持自动续期 |

**安全设计**:
- Access Token: 1 小时有效期（短令牌）
- Refresh Token: 7 天有效期（长令牌）
- Refresh Token 使用独立密钥（`JWT_REFRESH_SECRET`）
- Refresh Token 存储在数据库，支持服务端校验和撤销
- 登出时清除数据库中的 Refresh Token
- Refresh Token 快过期（< 1 天）时自动返回新的 Refresh Token

**配置** (`application.yml`):
```yaml
scratch:
  jwt:
    secret: ${JWT_SECRET:...}
    refresh-secret: ${JWT_REFRESH_SECRET:...}
    expiration: ${JWT_EXPIRATION:3600000}       # 1 小时
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 天
```

---

### 2.3 数据库索引补充（V17）

**迁移脚本**: `V17__jwt_and_indexes.sql`

```sql
-- Refresh Token 字段
ALTER TABLE `user`
    ADD COLUMN `refresh_token` VARCHAR(512),
    ADD COLUMN `refresh_token_expires_at` DATETIME;

-- 补充索引
CREATE INDEX idx_submission_user_created ON `submission` (`user_id`, `created_at` DESC);
CREATE INDEX idx_project_user_status_created ON `project` (`user_id`, `status`, `created_at` DESC);
CREATE INDEX idx_user_follow_following ON `user_follow` (`following_id`, `created_at` DESC);
CREATE INDEX idx_user_follow_follower ON `user_follow` (`follower_id`, `created_at` DESC);
CREATE INDEX idx_collab_session_project ON `collab_session` (`project_id`, `status`);
CREATE INDEX idx_collab_participant_user ON `collab_participant` (`user_id`, `joined_at`);
```

---

### 2.4 前端 Token 恢复验证

**文件**: `stores/user.ts`

新增 `validateToken()` 方法：
- 页面刷新时自动调用 `GET /user/me` 验证 Token 有效性
- 验证失败自动清除登录状态
- 在路由守卫中首次加载时触发

**文件**: `router/index.ts`

```typescript
let tokenValidated = false

router.beforeEach(async (to) => {
  if (!tokenValidated && userStore.isLoggedIn) {
    tokenValidated = true
    await userStore.validateToken()
  }
  // ... 权限检查
})
```

---

### 2.5 前端请求拦截器增强

**文件**: `api/request.ts`

改进点：
1. 401 响应优先使用 Refresh Token 刷新（而非旧的 Access Token）
2. 刷新成功后自动重试原始请求
3. 刷新失败触发登出 + 提示
4. 防并发：多个请求同时 401 时只发一次刷新请求
5. 刷新成功后重试所有等待中的请求

---

### 2.6 ErrorCode 扩展

**文件**: `ErrorCode.java`

```java
REFRESH_TOKEN_EXPIRED(9995, "Refresh Token 已过期"),
REFRESH_TOKEN_INVALID(9994, "Refresh Token 无效"),
```

---

## 三、修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/.../MybatisPlusConfig.java` | 修改 | 分页限制 100 |
| `backend/.../JwtUtils.java` | 修改 | Refresh Token 方法 |
| `backend/.../LoginVO.java` | 修改 | 新增 refreshToken 字段 |
| `backend/.../User.java` | 修改 | 新增 refreshToken 字段 |
| `backend/.../UserService.java` | 修改 | 登录/注册返回 Refresh Token |
| `backend/.../UserController.java` | 修改 | refresh 端点重构 |
| `backend/.../ErrorCode.java` | 修改 | 新增错误码 |
| `backend/.../application.yml` | 修改 | JWT 配置 |
| `backend/.../V17__jwt_and_indexes.sql` | 新增 | 数据库迁移 |
| `frontend-vue/src/types/index.ts` | 修改 | LoginVO 类型 |
| `frontend-vue/src/stores/user.ts` | 修改 | Refresh Token + Token 验证 |
| `frontend-vue/src/api/request.ts` | 修改 | Refresh Token 刷新逻辑 |
| `frontend-vue/src/router/index.ts` | 修改 | Token 验证守卫 |

---

## 四、环境变量清单

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `JWT_SECRET` | Access Token 密钥 | 开发默认值（生产必须设置） |
| `JWT_REFRESH_SECRET` | Refresh Token 密钥 | 开发默认值（生产必须设置） |
| `JWT_EXPIRATION` | Access Token 有效期（ms） | 3600000（1h） |
| `JWT_REFRESH_EXPIRATION` | Refresh Token 有效期（ms） | 604800000（7d） |

---

## 五、向后兼容性

- ✅ 旧版 Token 仍可正常使用（有效期 24h 内）
- ✅ 不含 Refresh Token 的响应不影响前端（可选字段）
- ✅ 数据库迁移使用 `ADD COLUMN`（非破坏性）
- ✅ 索引使用 `CREATE INDEX IF NOT EXISTS`（幂等）

---

## 六、后续建议

1. **监控 Refresh Token 使用率**：统计 Refresh Token 刷新频率，评估 Token 有效期是否合理
2. **Refresh Token 轮换策略**：每次使用 Refresh Token 时自动签发新的（已实现快过期自动续期）
3. **多设备登录管理**：当前 Refresh Token 只存储一个，后登录的设备会踢掉先登录的
4. **Token 黑名单优化**：考虑使用 Redis SET 替代单个 key，支持批量撤销
