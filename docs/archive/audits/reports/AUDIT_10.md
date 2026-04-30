# 🔍 AUDIT_10 — Sprint 8 二次审计

> 日期：2026-04-23
> 范围：Sprint 8 新增代码（AdminService/前端/联调脚本/Docker）
> 审计类型：二次审计（开发后立即审计）

---

## 发现统计

| 级别 | 数量 | 说明 |
|------|------|------|
| 🔴 严重 (S) | 3 | 安全漏洞 / 运行时错误 |
| 🟡 中等 (M) | 6 | 健壮性 / 完整性 |
| 🔵 优化 (O) | 3 | UX / 性能 |
| **总计** | **12** | |

---

## 严重问题 (S)

### S1. AdminUpdateUserDTO 无角色/状态枚举校验

**文件**: `scratch-user/.../dto/AdminUpdateUserDTO.java`
**问题**: role 和 status 字段无任何校验注解，管理员可设置任意字符串如 "HACKER" 或状态值 99
**修复**: 添加 `@Pattern` 校验，role 限制为 STUDENT/TEACHER/ADMIN，status 限制为 0/1

### S2. AdminService.disableUser() 无自保护

**文件**: `scratch-user/.../service/AdminService.java`
**问题**: 管理员可以禁用自己的账号，导致系统无管理员
**修复**: 添加 currentAdminId 参数，禁止禁用自己

### S3. 前端 navigate() 引用不存在的 renderResult

**文件**: `frontend/index.html`
**问题**: routes 对象包含 `result: renderResult` 但该函数未定义，触发 ReferenceError
**修复**: 从 routes 中移除 result 路由

---

## 中等问题 (M)

### M1. AdminUpdateUserDTO 导入未使用的注解

**文件**: `scratch-user/.../dto/AdminUpdateUserDTO.java`
**问题**: 导入 `@NotBlank` 和 `@NotNull` 但未使用
**修复**: 移除未使用的导入，改用 `@Pattern`

### M2. AdminService.queryCount() 日志级别不当

**文件**: `scratch-user/.../service/AdminService.java`
**问题**: 数据库查询失败只 warn 级别，运维难以发现
**修复**: 改为 error 级别并输出 SQL

### M3. 前端 renderHomework() 空壳

**文件**: `frontend/index.html`
**问题**: 未调用 API，直接显示静态文本
**修复**: 改为根据用户角色显示不同内容

### M4. api-test.sh 依赖 python3

**文件**: `scripts/api-test.sh`
**问题**: 使用 python3 解析 JSON，部分环境无 python3
**修复**: 全部改为 grep/sed 实现，零外部依赖

### M5. nginx.conf 缺少 WebSocket 支持

**文件**: `docker/nginx.conf`
**问题**: 未配置 WebSocket 升级头，实时通知功能受限
**修复**: 添加 `proxy_http_version 1.1` + `Upgrade` + `Connection` 头

### M6. 前端提交按钮无防重复点击

**文件**: `frontend/index.html`
**问题**: 登录/注册/提交答案按钮可重复点击导致多次请求
**修复**: 添加 `disabled` 防抖 + loading 状态文字

---

## 修复清单

| # | 文件 | 修复 | 级别 |
|---|------|------|------|
| 1 | AdminUpdateUserDTO.java | 添加 @Pattern 校验 role/status | S |
| 2 | AdminService.java | disableUser 添加自保护 | S |
| 3 | AdminController.java | 传递 currentAdminId | S |
| 4 | index.html | 移除 renderResult 路由 | S |
| 5 | AdminUpdateUserDTO.java | 移除未使用导入 | M |
| 6 | AdminService.java | queryCount 日志改 error | M |
| 7 | index.html | renderHomework 实现 | M |
| 8 | api-test.sh | 移除 python3 依赖 | M |
| 9 | nginx.conf | 添加 WebSocket 支持 | M |
| 10 | index.html | 按钮防抖 | M |

---

*审计完成，全部问题已修复。*
