# 🔍 代码审计报告

> 审计时间：2026-04-23 16:47
> 审计范围：Phase 1 脚手架全部代码 (57 个文件)

---

## 严重问题 (6 个)

### S1. application.yml 日志配置会拖垮生产环境
- **位置**: `scratch-app/src/main/resources/application.yml`
- **问题**: `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl` 硬编码，生产环境打印所有 SQL 到 stdout，性能极差
- **修复**: 改为环境变量控制，默认关闭

### S2. init.sql 管理员密码 BCrypt 哈希是伪造的
- **位置**: `docker/init.sql`
- **问题**: `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH` 不是 "admin123" 的真实 BCrypt 哈希，启动后无法登录
- **修复**: 生成真实哈希

### S3. AuthInterceptor 角色校验失败返回 401 而非 403
- **位置**: `AuthInterceptor.java`
- **问题**: `writeError` 硬编码 HTTP 401，角色不匹配应返回 403 Forbidden
- **修复**: 区分 401 和 403

### S4. scratch-user 不应依赖 scratch-sb3
- **位置**: `scratch-user/pom.xml`
- **问题**: user 模块完全不需要 sb3-parser，这是错误耦合
- **修复**: 移除依赖

### S5. Docker MySQL 环境变量无效
- **位置**: `docker/docker-compose.yml`
- **问题**: `MYSQL_CHARSET` 和 `MYSQL_COLLATION` 不是 MySQL 官方镜像支持的环境变量，实际不生效
- **修复**: 移除无效变量，通过 command 参数设置

### S6. SensitiveWordFilter 线程安全问题
- **位置**: `SensitiveWordFilter.java`
- **问题**: `sensitiveWords` 是普通 HashSet，`reload()` 替换引用时存在竞态条件
- **修复**: 使用 volatile + ConcurrentHashMap.newKeySet()

---

## 中等问题 (9 个)

### M1. server.servlet.context-path: / 多余
- 默认值就是 `/`，无需显式配置

### M2. docker-compose version 字段已废弃
- Docker Compose V2 不再需要 version 字段

### M3. hutool-all 依赖过大
- 引入整个 hutool 包含数百个工具类，应按需引入 hutool-core

### M4. JwtUtils 未使用的方法参数
- `generateToken` 接收 `Map` 参数但从未使用

### M5. docker-compose 端口暴露范围过大
- MySQL/Redis 端口暴露到宿主机，开发环境可以但应加注释说明

### M6. Dockerfile.backend 依赖缓存策略不完整
- 只拷贝 pom.xml 不拷贝 src，`dependency:go-offline` 对多模块可能不完整

### M7. LoginUser 的 @Data 可能影响 ThreadLocal
- Lombok @Data 生成的 equals/hashCode 包含 ThreadLocal 字段，应避免

### M8. sandbox judge-worker.js 变量类型判断错误
- `variable.type === ''` 不是判断 Scratch 变量的正确方式

### M9. UserService.searchUsers 返回空 Page
- 查询了数据库但转换为 VO 的步骤缺失，直接返回空 Page

---

*所有问题已修复，见下方实施记录。*
