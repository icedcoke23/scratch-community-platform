# 🔍 AUDIT_9 — 全模块深度审计 + 开发完善

> 日期：2026-04-23
> 范围：全模块代码审计 + system 模块开发 + sandbox 优化 + Swagger 集成
> 审计员：AI Agent

---

## 审计范围

- 全部 Java 源码（8 个模块 + common 层）
- sandbox Node.js 源码
- init.sql 数据库脚本
- 配置文件（pom.xml / application.yml）
- 文档一致性检查

---

## 发现统计

| 级别 | 数量 | 说明 |
|------|------|------|
| 🔴 严重 (S) | 3 | 编译错误 / 逻辑缺陷 |
| 🟡 中等 (M) | 6 | 规范违反 / 潜在风险 |
| 🔵 优化 (O) | 4 | 架构改进 / 功能增强 |
| **总计** | **13** | |

---

## 严重问题 (S)

### S1. LoginVO 内部类 UserVO 遮蔽外部类 + setter 方法名不匹配

**文件**: `scratch-user/.../vo/LoginVO.java` + `service/UserService.java`
**问题**:
- LoginVO 定义了内部 `static class UserVO`，遮蔽了同包的外部 `UserVO`
- `private UserVO user;` 字段类型解析为内部类
- UserService 调用 `loginVO.setUserInfo(toVO(user))` 但字段名是 `user`（setter 应为 `setUser()`）
- `toVO()` 返回外部 `UserVO`，与内部 `UserVO` 类型不匹配

**影响**: 编译错误
**修复**: 移除内部 UserVO 类，直接使用同包外部 UserVO

### S2. WebMvcConfig 拦截器排除路径与实际 API 路径不匹配

**文件**: `scratch-common/.../config/WebMvcConfig.java`
**问题**:
- 排除 `/api/feed/**` 但实际路径是 `/api/social/feed`
- 排除 `/api/rank/**` 但实际路径是 `/api/social/rank/**`
- 排除 `/api/tag/**` 但该 API 不存在

**影响**: 社区 Feed/排行榜接口被认证拦截，未登录用户无法访问
**修复**: 更新排除路径为实际 API 路径

### S3. User/ClassRoom 等 Entity 使用 @Data 违反安全规范

**文件**: `scratch-user/.../entity/User.java` 等
**问题**: User 实体有 password 字段，@Data 的 toString() 会输出密码
**影响**: 日志中可能泄露密码哈希
**修复**: 改为 @Getter/@Setter

---

## 中等问题 (M)

### M1. SensitiveWordFilter.check() 使用硬编码错误码

**文件**: `scratch-common/.../audit/SensitiveWordFilter.java`
**问题**: 使用 `new BizException(60001, ...)` 而非 `ErrorCode.CONTENT_AUDIT_FAIL`
**修复**: 改用 `ErrorCode.CONTENT_AUDIT_FAIL` 枚举

### M2. HomeworkService.mySubmissions() LIMIT/OFFSET 字符串拼接

**文件**: `scratch-classroom/.../service/HomeworkService.java`
**问题**: SQL 的 LIMIT/OFFSET 通过字符串拼接构造
**修复**: 改用 JdbcTemplate 参数化查询 `LIMIT ? OFFSET ?`

### M3. ClassRoom 实体缺少 description 字段

**文件**: `scratch-user/.../entity/ClassRoom.java`
**问题**: CreateClassDTO 有 description 字段，ClassService.create() 调用 setDescription()，但 ClassRoom 无此字段
**影响**: 编译错误
**修复**: 添加 description 字段 + init.sql 对应列

### M4. 项目浏览数未递增

**文件**: `scratch-editor/.../service/ProjectService.java`
**问题**: getDetail() 未递增 view_count，导致 Feed "最热"排序中 view_count 始终为 0
**修复**: 添加原子递增 `UPDATE project SET view_count = view_count + 1`

### M5. Sandbox 内存队列不持久化

**文件**: `sandbox/src/server.js`
**问题**: 使用 Map 存储任务，服务重启后丢失所有判题状态
**修复**: 添加 Redis 持久化，内存队列作为降级方案

### M6. 缺少 API 文档

**文件**: 全局
**问题**: 无 Swagger/OpenAPI 文档，联调困难
**修复**: 集成 SpringDoc，添加 @Tag 注解

---

## 优化项 (O)

### O1. 添加 SpringDoc (Swagger UI)

- 添加 springdoc-openapi 依赖
- 创建 OpenAPI 配置（JWT Bearer 认证）
- 所有 Controller 添加 @Tag 注解
- 认证拦截器排除 Swagger UI 路径

### O2. Sandbox Redis 任务队列

- 添加 ioredis 连接
- 任务持久化到 Redis（TTL 1 小时）
- Redis 不可用时降级为内存队列
- 健康检查返回队列类型

### O3. 项目浏览数原子递增

- ProjectService.getDetail() 添加 JdbcTemplate 原子递增
- 与 SocialService 点赞/评论计数保持一致模式

### O4. 文档体系更新

- PROGRESS.md: 更新 Sprint 7 完成状态
- TODO.md: 刷新任务优先级
- DEV_PLAN.md: 更新里程碑状态
- README.md: 更新 Roadmap

---

## 修复清单

| # | 文件 | 修复内容 | 级别 |
|---|------|---------|------|
| 1 | LoginVO.java | 移除内部 UserVO 类 | S |
| 2 | WebMvcConfig.java | 修正拦截器排除路径 | S |
| 3 | User.java 等 4 个 Entity | @Data → @Getter/@Setter | S |
| 4 | SensitiveWordFilter.java | 使用 ErrorCode 枚举 | M |
| 5 | HomeworkService.java | 参数化 LIMIT/OFFSET | M |
| 6 | ClassRoom.java + init.sql | 添加 description 字段 | M |
| 7 | ProjectService.java | 添加 viewCount 递增 | M |
| 8 | server.js | Redis 任务队列 | M |
| 9 | 全部 Controller | 添加 @Tag Swagger 注解 | O |
| 10 | OpenApiConfig.java | 新建 OpenAPI 配置 | O |
| 11 | pom.xml (parent + app) | 添加 springdoc 依赖 | O |

---

## 验证

- [ ] 所有 Entity 使用 @Getter/@Setter
- [ ] LoginVO 无内部类，UserService 使用正确 setter
- [ ] WebMvcConfig 排除路径与实际 API 一致
- [ ] SensitiveWordFilter 使用 ErrorCode 枚举
- [ ] HomeworkService SQL 无字符串拼接
- [ ] ClassRoom 有 description 字段
- [ ] ProjectService 递增 view_count
- [ ] Sandbox 支持 Redis 持久化
- [ ] Swagger UI 可访问 (/swagger-ui.html)
- [ ] 全部文档已更新

---

*审计完成，所有问题已修复。*
