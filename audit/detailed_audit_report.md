# Scratch Community Platform — 深度代码审计报告

**审计日期**: 2026-04-29  
**版本**: 3.0.0-SNAPSHOT  
**审计范围**: 全栈（后端 20,620 行 Java + 前端 12,903 行 TS/Vue）

---

## 📊 整体评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐ | 模块化清晰，但部分模块耦合度偏高 |
| 代码质量 | ⭐⭐⭐⭐ | 代码规范较好，但存在部分技术债务 |
| 安全性 | ⭐⭐⭐ | 存在安全隐患（JWT 密钥、默认配置） |
| 性能 | ⭐⭐⭐⭐ | 缓存策略合理，但限流器需升级 |
| 可维护性 | ⭐⭐⭐⭐ | 文档较全，日志规范 |
| 测试覆盖 | ⭐⭐⭐ | 单元测试存在但覆盖率不足 |
| **综合** | **⭐⭐⭐⭐** | **质量良好，需修复 8 个关键问题** |

---

## 🔴 关键问题（P0 - 必须修复）

### 1. JWT 密钥使用默认值 ⚠️ 高危

**位置**: `backend/scratch-app/src/main/resources/application.yml:93-94`

```yaml
jwt:
  secret: ${JWT_SECRET:scratch-community-secret-key-at-least-32bytes-long!!}
  refresh-secret: ${JWT_REFRESH_SECRET:scratch-community-refresh-secret-key-at-least-32b}
```

**风险**: 
- 默认密钥硬编码在配置文件中，任何能接触到代码的人都能伪造 Token
- 生产环境若未设置环境变量，将使用弱密钥

**影响**: 身份伪造、权限绕过、数据泄露

**修复建议**:
```yaml
# 生产环境必须通过环境变量注入
jwt:
  secret: ${JWT_SECRET:}  # 删除默认值，启动时校验非空
  refresh-secret: ${JWT_REFRESH_SECRET:}
```

```java
// JwtUtils.java 添加启动校验
@PostConstruct
public void init() {
    Assert.hasText(secret, "JWT_SECRET 未配置，请通过环境变量设置");
    Assert.hasText(refreshSecret, "JWT_REFRESH_SECRET 未配置");
}
```

**优先级**: 🔴 P0

---

### 2. 数据库事务管理缺失

**位置**: 多个 Service 方法缺少 `@Transactional`

示例（已发现）：
- `ConfigService.updateConfig()` - 修改系统配置
- `NotifyService.updateNotification()` - 更新通知状态
- `AuditService.updateAuditLog()` - 更新审计日志

**风险**:
- 多步骤操作可能部分成功部分失败，导致数据不一致
- 例如：更新配置 + 记录审计日志，若第二个失败，第一个已生效

**修复建议**:
```java
@Service
public class ConfigService {
    
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String key, String value, String description) {
        // 1. 更新配置
        // 2. 记录审计日志
        // 两个操作要么都成功，要么都回滚
    }
}
```

**优先级**: 🔴 P0

---

### 3. 限流器为固定窗口（可能瞬间溢出）

**位置**: `backend/scratch-common-core/src/main/java/com/scratch/community/common/config/RateLimitConfig.java`

```java
private final RateLimiter globalLimiter = new RateLimiter(60, 60000);  // 固定窗口
private final RateLimiter loginLimiter = new RateLimiter(10, 60000);
```

**问题**: 固定窗口在窗口边界处可能瞬间双倍流量（例如：第 60 秒用尽，第 61 秒立刻又有新配额）

**修复建议**: 升级为**滑动窗口**或**令牌桶算法**
```java
// 推荐使用 Redis 分布式令牌桶（Guava RateLimiter 仅限单机）
@Bean
public RedisRateLimiter redisRateLimiter(RedissonClient redisson) {
    return new RedisRateLimiter(redisson);
}
```

**优先级**: 🟡 P1

---

### 4. 前端 Token 刷新存在竞态条件

**位置**: `frontend-vue/src/api/request.ts:96-111`

```typescript
let pendingRequests: Array<(token: string) => void> = []
// ...
pendingRequests.forEach(cb => cb(newToken))
pendingRequests = []
```

**问题**: 
- `pendingRequests = []` 在 forEach 之后立即清空
- 若此时有新的请求 401，会在 forEach 执行期间加入新回调
- forEach 使用旧数组，新回调丢失 → 请求永远挂起

**修复建议**（已在注释中标记）：
```typescript
// 标记重试状态
let isRetryingPending = false

// 在重试完成前不清除 refreshPromise
isRetryingPending = true
retryPendingRequests(newToken)
isRetryingPending = false

// 延迟清除 refreshPromise
refreshPromise.finally(() => {
  if (!isRetryingPending) {
    refreshPromise = null
  } else {
    setTimeout(() => { refreshPromise = null }, 100)
  }
})
```

**优先级**: 🟡 P1

---

### 5. 数据库缺少外键约束

**位置**: 所有 19 张表迁移脚本

**现状**: Flyway 迁移脚本中未发现任何 `FOREIGN KEY` 约束

**风险**:
- 应用层需自行保证数据一致性
- 可能出现孤儿数据（例如：用户删除后，其作品、评论仍存在）
- 级联操作需在代码中手动处理，易遗漏

**建议**:
```sql
-- 示例：评论表外键
ALTER TABLE comment 
  ADD CONSTRAINT fk_comment_user 
  FOREIGN KEY (user_id) REFERENCES user(id) 
  ON DELETE CASCADE;
```

**权衡**: 微服务架构有时会故意去掉外键以提升性能，但本项目的单体架构**建议添加**。

**优先级**: 🟡 P1

---

### 6. 测试覆盖率不足

**现状**:
- 后端单元测试存在，但多个测试失败（`SocialServiceTest` 4 个测试失败）
- `HomeworkServiceTest` 编译错误（`grade()` 方法重构后测试未更新）
- 前端无单元测试（Jest/Vitest）

**影响**: 
- CI 流水线不通过
- 代码重构风险高
- 隐性 Bug 难以发现

**修复建议**:
1. 修复 `SocialServiceTest` - Mock 事件发布器
2. 更新 `HomeworkServiceTest` - 适配新方法签名
3. 添加核心服务测试（UserService、ProjectService、JudgeService）
4. 前端添加组件测试（@vue/test-utils）

**优先级**: 🟡 P1

---

### 7. MinIO 默认凭证未修改

**位置**: `backend/scratch-common-core/src/main/java/com/scratch/community/common/config/MinioConfig.java`

```java
private String accessKey = "minioadmin";
private String secretKey = "minioadmin";
```

**风险**: 生产环境若未通过环境变量覆盖，将使用默认弱凭证

**修复建议**:
```java
@Value("${scratch.minio.access-key:}")
private String accessKey;
@Value("${scratch.minio.secret-key:}")
private String secretKey;

@PostConstruct
public void validate() {
    Assert.hasText(accessKey, "MINIO_ROOT_USER 未配置");
    Assert.hasText(secretKey, "MINIO_ROOT_PASSWORD 未配置");
    Assert.isTrue(!accessKey.equals("minioadmin"), "请修改 MinIO 默认用户名");
    Assert.isTrue(!secretKey.equals("minioadmin"), "请修改 MinIO 默认密码");
}
```

**优先级**: 🟡 P1

---

### 8. 日志中可能记录敏感信息

**位置**: `backend/scratch-system/src/main/java/com/scratch/community/module/system/service/NotifyService.java:39`

```java
log.info("创建通知: userId={}, type={}, title={}", userId, type, title);
```

**风险**: 
- `title` 字段可能包含用户隐私（如 "张三 评论了你的作品"）
- 生产环境日志集中收集时可能泄露 PII

**修复建议**:
```java
log.info("创建通知: userId={}, type={}, titleHash={}", 
    userId, type, DigestUtils.md5DigestAsHex(title.getBytes()));
// 或记录标题长度而非内容
log.info("创建通知: userId={}, type={}, titleLength={}", userId, type, title.length());
```

**优先级**: 🟡 P1

---

## 🟡 中等问题（P2 - 建议优化）

### 9. 异常处理过于宽泛

**位置**: `backend/scratch-common-core/src/main/java/com/scratch/community/common/exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(Exception.class)
public R<Void> handleException(Exception e, HttpServletRequest request) {
    log.error("系统异常: {}", e.getMessage(), e);  // 仅记录消息，未记录堆栈
    return R.fail(500, "服务器内部错误");
}
```

**问题**: `log.error("...", e)` 才会打印堆栈，这里只传了消息字符串

**修复**:
```java
log.error("系统异常路径: {}", request.getRequestURI(), e);  // 第二个参数为 Throwable
```

---

### 10. Thread.sleep 阻塞线程池

**位置**: `backend/scratch-judge/src/main/java/com/scratch/community/module/judge/service/JudgeService.java:254`

```java
Thread.sleep(delayMs);  // 阻塞 judgeExecutor 线程
```

**问题**: 判题线程池核心 4 线程，若并发 16 任务且都 sleep，线程被阻塞，新任务排队

**修复**: 使用 `ScheduledExecutorService` 或 Redisson 延迟队列

```java
@Autowired
private ThreadPoolTaskExecutor judgeExecutor;

public void judgeWithDelay(JudgeTask task, long delayMs) {
    judgeExecutor.getThreadPoolExecutor().schedule(
        () -> judge(task), 
        delayMs, 
        TimeUnit.MILLISECONDS
    );
}
```

---

### 11. 缺少 API 请求合并

**场景**: 前端频繁请求用户信息、排行榜等数据

**现状**: 每个组件独立请求，可能导致相同数据多次查询

**建议**: 使用 DataLoader 模式或批量查询接口

```typescript
// request.ts 增加批量请求拦截器
api.interceptors.request.use(config => {
  if (config.batch) {
    config.batchId = generateBatchId()
  }
  return config
})
```

---

### 12. 未使用连接池监控

**现状**: HikariCP 默认配置，未暴露 metrics

**建议**: 启用 Spring Boot Actuator 的 `HikariCPMetrics`
```yaml
management:
  metrics:
    enable:
      hikari: true
```

---

### 13. 缺少分布式追踪

**现状**: 微服务间调用无 Trace ID

**建议**: 集成 OpenTelemetry 或 SkyWalking
```java
@Bean
public Tracer tracer() {
    return OpenTelemetry.getGlobalTracer("scratch-community");
}
```

---

### 14. 图片未压缩直接上传

**位置**: `FileUploadUtils.uploadAvatar()`

**现状**: 头像直接上传原图，未压缩/缩略图

**建议**: 上传前前端压缩，或后端使用 Thumbnailator 生成缩略图

---

### 15. 缺少文件病毒扫描

**现状**: 文件上传后直接存储到 MinIO

**风险**: 恶意文件（如伪装为 .jpg 的 .exe）可能被下载执行

**建议**: 集成 ClamAV 扫描
```java
public boolean scanVirus(File file) {
    Process p = Runtime.getRuntime.exec("clamscan " + file.getAbsolutePath());
    // 解析输出
}
```

---

## 🟢 次要问题（P3 - 可选优化）

### 16. 代码重复

- `UserService.followUser()` 和 `unfollowUser()` 逻辑对称，可提取公共方法
- `ProjectController` 和 `ProblemController` 中权限校验代码重复

---

### 17. 魔法数字

**位置**: 多处硬编码数字
```java
private static final long SB3_MAX_SIZE = 100 * 1024 * 1024; // ✅ 已有常量
// 但仍有
Thread.sleep(5000);  // ❌ 应改为常量
```

---

### 18. 日志级别不当

- `log.debug("限流器过期窗口清理完成，共清理 {} 个条目", totalCleaned);` - 大量 debug 可能影响性能
- `log.info("创建通知: userId={}, type={}, title={}", ...)` - info 级别可能过多

**建议**: 调整日志级别为 WARN 或 INFO（根据实际需要）

---

### 19. 前端 console.log 遗留

```typescript
frontend-vue/src/api/ai-review.ts:57:        console.log("Judge result:", result);
```

**建议**: 移除或改为 `if (import.meta.env.DEV)` 条件编译

---

### 20. 缺少 ESLint/Prettier 配置

**现状**: 前端代码风格不统一（观察不同文件缩进、引号风格）

**建议**: 添加 `.eslintrc.js` 和 `.prettierrc`

---

## 📈 性能优化建议

### 21. 数据库索引优化

**建议添加的索引**:
```sql
-- point_log 表：用户 + 时间范围查询频繁
CREATE INDEX idx_point_log_user_time ON point_log(user_id, created_at DESC);

-- project 表：创作者 + 状态查询
CREATE INDEX idx_project_creator_status ON project(creator_id, status);

-- comment 表：项目 + 层级查询
CREATE INDEX idx_comment_project_parent ON comment(project_id, parent_id);
```

---

### 22. Redis 热点 Key 预警

**场景**: 排行榜（`rank:like:weekly`）可能成为热点

**建议**: 使用本地缓存（Caffeine） + Redis 二级缓存

```java
@Cacheable(value = "rank:weekly", sync = true)
public List<RankVO> getWeeklyRank() {
    return redisTemplate.opsForZSet().rangeWithScores("rank:like:weekly", 0, 99);
}
```

---

### 23. 前端图片懒加载

**现状**: 所有图片同步加载

**建议**: 使用 `v-lazy` 或 IntersectionObserver

```vue
<img v-lazy="project.coverUrl" alt="封面" />
```

---

### 24. 代码分割不足

**现状**: 单一 `index-*.js` 845KB（gzipped 274KB）

**建议**: 
- 路由级懒加载（已配置 manualChunks，但可进一步优化）
- Element Plus 按需导入（确认是否已配置）

---

## 🔐 安全加固清单

| 问题 | 当前状态 | 建议措施 |
|------|----------|----------|
| JWT 默认密钥 | ⚠️ 危险 | 删除默认值，强制环境变量 |
| 密码加密 | ✅ BCrypt | 保持 |
| 文件上传类型 | ✅ 白名单 | 增加病毒扫描 |
| XSS 防护 | ✅ Vue 转义 | 避免使用 v-html（已检查无） |
| CSRF | ⚠️ 依赖 JWT | 如需双写 Cookie，启用 Spring Security CSRF |
| SQL 注入 | ✅ MyBatis-Plus | 保持参数化查询 |
| 限流 | ⚠️ 固定窗口 | 升级滑动窗口/令牌桶 |
| 日志脱敏 | ✅ 已实现 | 保持并扩展 |
| 敏感词过滤 | ✅ 21 个内置词 | 支持动态配置 |

---

## 📝 优化建议优先级

### Phase 1（本周）- 阻塞性安全问题
1. ✅ **JWT 密钥强制环境变量**（1 小时）
2. ✅ **MinIO 凭证校验**（30 分钟）
3. ✅ **修复事务缺失**（2-3 小时）
4. ✅ **修复 CI 测试**（1 小时）

### Phase 2（下周）- 架构优化
5. **升级限流器为滑动窗口**（3 小时）
6. **添加外键约束 + 数据迁移脚本**（4 小时）
7. **文件病毒扫描集成**（2 小时）
8. **分布式追踪（OpenTelemetry）**（3 小时）

### Phase 3（下月）- 体验优化
9. **前端图片懒加载 + 压缩**（2 小时）
10. **代码分割优化**（1 小时）
11. **添加单元测试（目标 >60%）**（8 小时）
12. **性能压测 + 慢查询优化**（4 小时）

---

## 🎯 最终建议

1. **立即行动**: 修复 P0 安全问题（JWT/MinIO 默认凭证）
2. **CI 优先**: 修复测试，让 GitHub Actions 全绿
3. **数据安全**: 添加事务和外键，防止数据不一致
4. **性能提升**: 升级限流器，添加索引
5. **长期维护**: 完善监控、日志、测试

**预计总工作量**: 32-40 小时（2-3 个工作日）

---

## 📋 审计方法说明

- 静态代码分析：grep + 手动审查
- 安全扫描：硬编码密码、XSS/SQL 注入模式
- 架构评估：模块依赖、数据流、部署拓扑
- 性能预判：N+1 查询、缓存策略、连接池配置

---

**报告生成**: 2026-04-29  
**审计工具**: 自定义脚本 + 人工审查  
**下次审计**: 建议 1 个月后（待上述问题修复后）
