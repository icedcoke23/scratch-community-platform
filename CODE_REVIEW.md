# Scratch Community Platform - 代码审查报告

> 审查时间: 2026-05-02
> 审查者: YYDS (AI Code Review)
> 版本: v0.1.0-SNAPSHOT (commit: 64089ab)

---

## Summary

该项目是一个面向少儿编程的 Scratch 社区平台，采用 Spring Boot 3.2 + Vue 3 + MySQL + Redis + MinIO 技术栈。整体架构清晰，模块划分合理，安全机制较为完善。但存在一些需要关注的问题，主要集中在异常处理、前端代码质量和部分潜在的安全风险。

**总体评价**: 🟡 **需要关注** - 架构良好，但有若干需要修复的问题

---

## Critical Issues (Blocking)

### 1. 【安全】生产服务器 GitHub Token 泄露

**严重程度**: 🔴 Blocking

**问题描述**:
生产服务器 `/root/scratch-community-platform` 下的 Git 配置中包含明文 GitHub Personal Access Token。

**风险**:
- Token 可被任何有权访问服务器的用户获取
- 攻击者可利用该 Token 访问仓库、修改代码、创建恶意 PR

**修复建议**:
1. 立即撤销该 Token（GitHub → Settings → Developer settings → Personal access tokens）
2. 使用 SSH Key 或 Git Credential Manager 替代 HTTPS Token
3. 或使用 `.git-credentials` 文件并添加到 `.gitignore`

---

### 2. 【安全】生产服务器数据库/MinIO 端口暴露公网

**严重程度**: 🔴 Blocking (已在之前修复)

**问题描述**:
MySQL 3306、MinIO 9000/9001 端口原本对公网开放。

**当前状态**: ✅ 已通过 firewall-cmd 规则限制为本地访问

---

## Required Changes

### 3. 【代码质量】异常处理过于宽泛

**严重程度**: 🟠 Required

**问题描述**:
大量使用 `catch (Exception e)` 捕获所有异常，可能掩盖具体错误：

```java
// scratch-common-core/.../FileUploadUtils.java - 8 处 catch (Exception e)
// scratch-common-redis/.../RedisRateLimiter.java - 4 处 catch (Exception e)
// scratch-classroom/.../HomeworkService.java - 3 处 catch (Exception e)
```

**风险**:
- 吞掉重要异常信息
- 难以定位问题根因
- 可能隐藏系统级错误

**修复建议**:
```java
// 错误示例
try {
    // ...
} catch (Exception e) {
    log.warn("操作失败: {}", e.getMessage());
}

// 正确做法
try {
    // ...
} catch (IOException e) {
    log.error("文件读写失败: path={}", path, e);
    throw new FileOperationException("文件上传失败", e);
} catch (SQLException e) {
    log.error("数据库操作失败: sql={}", sql, e);
    throw new DataAccessException("数据保存失败", e);
}
```

---

### 4. 【代码质量】前端组件过大

**严重程度**: 🟠 Required

**问题描述**:
多个 Vue 组件行数超过 500 行，违反单一职责原则：

| 文件 | 行数 | 问题 |
|------|------|------|
| `ScratchEditorView.vue` | 837 | 应拆分为编辑器、工具栏、预览等子组件 |
| `ProjectDetailView.vue` | 594 | 应拆分为头部、评论区、侧边栏 |
| `AdminLayout.vue` | 580 | 应拆分为侧边栏、顶部导航、面包屑 |
| `AdminUsersView.vue` | 572 | 表格、表单、筛选应拆分 |
| `UserProfileView.vue` | 555 | 应拆分为头部、作品列表、粉丝关注 |

**风险**:
- 维护困难
- 复用性差
- 性能问题（不必要的重渲染）

**修复建议**:
```vue
<!-- 拆分前: ScratchEditorView.vue (837行) -->
<template>
  <div>
    <!-- 工具栏逻辑 -->
    <!-- 画布逻辑 -->
    <!-- 积木区域逻辑 -->
    <!-- 预览逻辑 -->
  </div>
</template>

<!-- 拆分后 -->
<template>
  <div class="editor-layout">
    <EditorToolbar :project="project" @save="handleSave" />
    <EditorCanvas ref="canvas" :project="project" />
    <EditorBlocks :category="activeCategory" />
    <EditorPreview :project="project" />
  </div>
</template>
```

---

### 5. 【代码质量】TypeScript `any` 类型滥用

**严重程度**: 🟠 Required

**问题描述**:
测试文件中大量使用 `as any` 类型断言，虽然测试代码可以适当放宽，但仍需改进：

```typescript
// src/__tests__/stores.test.ts
store.currentProject = { id: 1 } as any
const projects = [{ id: 1 }, { id: 2 }] as any[]
```

**生产代码问题**:
```typescript
// src/composables/useCollabWebSocket.ts
const conflictData = ref<any>(null)  // 应定义明确类型
```

**修复建议**:
```typescript
// 定义明确的接口
interface ConflictData {
  type: 'update' | 'delete'
  field: string
  localValue: unknown
  remoteValue: unknown
}

const conflictData = ref<ConflictData | null>(null)
```

---

### 6. 【安全】敏感信息日志输出

**严重程度**: 🟡 Required

**问题描述**:
OAuth 模块中存在 Token 相关的日志输出：

```java
// QQOAuthProvider.java
log.error("QQ 获取 token 失败: {}", errMsg);

// WeChatOAuthProvider.java
log.error("微信获取 token 失败: {}", errMsg);
log.error("刷新微信 Token 异常", e);
```

**风险**:
- Token 可能被记录到日志文件
- 日志文件可能被非授权人员访问

**修复建议**:
```java
// 错误示例
log.error("微信获取 token 失败: {}", token);  // 不要记录 token

// 正确做法
log.error("微信 OAuth 认证失败: error={}", errorMsg);
```

---

### 7. 【性能】前端 Promise 未处理拒绝

**严重程度**: 🟡 Required

**问题描述**:
部分 Promise 没有 catch 处理：

```typescript
// src/views/social/FeedView.vue
.then(res => {
  // 没有 catch
})

// src/components/ErrorBoundary.vue
navigator.clipboard.writeText(text).then(
  // 没有 catch
)
```

**风险**:
- 未捕获的 Promise 拒绝会导致控制台警告
- 可能导致用户操作失败无反馈

**修复建议**:
```typescript
// 添加错误处理
api.getFeed()
  .then(res => {
    // 处理成功
  })
  .catch(err => {
    console.error('获取 Feed 失败:', err)
    ElMessage.error('加载失败，请重试')
  })
```

---

## Suggestions

### 8. 【建议】SQL 拼接改用参数化查询

**严重程度**: 🔵 Suggestion

**问题描述**:
`HomeworkService.java` 中使用字符串拼接构建 IN 子句：

```java
String inClause = classIds.stream().map(id -> "?").collect(Collectors.joining(","));
jdbcTemplate.query(
    "SELECT id, name FROM class WHERE id IN (" + inClause + ") AND deleted = 0",
    // ...
);
```

**当前评估**: ✅ 代码已正确使用 `?` 占位符，参数通过 `classIds.toArray()` 传入，不存在 SQL 注入风险。

**建议**: 可考虑使用 MyBatis Plus 的 `in()` 方法替代 JdbcTemplate，保持代码风格一致性。

---

### 9. 【建议】统一错误码体系

**严重程度**: 🔵 Suggestion

**问题描述**:
错误码分散在多处：
- `ErrorCode` 枚举
- 硬编码错误消息
- HTTP 状态码混用

**建议**:
```java
// 建立统一的错误码体系
public enum ErrorCode {
    // 用户模块 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    
    // 项目模块 2xxx
    PROJECT_NOT_FOUND(2001, "项目不存在"),
    PROJECT_NO_PERMISSION(2002, "无权访问"),
    
    // 判题模块 3xxx
    PROBLEM_NOT_FOUND(3001, "题目不存在"),
    SUBMISSION_DUPLICATE(3002, "请勿重复提交");
    
    private final int code;
    private final String message;
}
```

---

### 10. 【建议】前端请求取消优化

**严重程度**: 🔵 Suggestion

**优点**: 已实现请求取消机制 (`getAbortController`, `cancelAllRequests`)，这是良好实践。

**建议**: 在路由切换时自动取消未完成请求：

```typescript
// router/index.ts
router.beforeEach((to, from, next) => {
  cancelAllRequests()
  next()
})
```

---

## Verdict

**需要关注 (Needs Discussion)**

项目整体架构合理，安全机制（JWT 双令牌、熔断器、限流、幂等性）较为完善。主要问题：

1. **必须修复**: GitHub Token 泄露、生产环境敏感端口暴露（已修复）
2. **建议修复**: 异常处理过于宽泛、前端组件过大、Promise 错误处理
3. **可优化**: TypeScript 类型定义、错误码体系、日志脱敏

---

## Next Steps

1. **立即撤销泄露的 GitHub Token**
2. **修复异常处理模式** - 将 `catch (Exception e)` 改为具体异常类型
3. **拆分大组件** - 从 `ScratchEditorView.vue` 开始，逐步重构
4. **添加前端错误边界** - 确保所有 Promise 都有错误处理
5. **代码规范检查** - 引入 ESLint/SonarQube 进行持续检查

---

*Review generated by YYDS・神人 (AI Code Reviewer)*