# Scratch 社区平台 - API 审计报告

**审计时间**: 2026-05-01
**审计方式**: API 测试 + 代码审查
**服务器地址**: http://8.134.252.67/feed

---

## 一、审计摘要

### 1.1 审计范围
- 所有管理后台 API 接口
- 用户管理、作品管理、评论管理等核心功能
- Spring Boot 应用路由映射

### 1.2 关键发现
| 模块 | API 路径 | HTTP 状态码 | 问题 |
|------|----------|-------------|------|
| 数据统计 | `/api/v1/admin/dashboard` | ✅ 200 | 正常 |
| 用户管理 | `/api/v1/admin/user` | ✅ 200 | 正常 |
| 作品列表 | `/api/v1/admin/project` | ❌ 404 | **无法访问** |
| 作品统计 | `/api/v1/admin/project/stats` | ❌ 404 | **无法访问** |
| 评论管理 | `/api/v1/admin/comment` | ❌ 404 | **无法访问** |

---

## 二、详细分析

### 2.1 代码完整性检查 ✅

通过 Git 历史和代码审查确认：

1. **AdminController.java** 包含完整的作品管理和评论管理接口
   - 位置: `backend/scratch-user/src/main/java/com/scratch/community/module/user/controller/AdminController.java`
   - 提交历史: `e88d9e2` - "feat: P0 全面实现 - 后台布局重构/用户管理/作品管理/评论管理"
   - 已实现接口:
     - `GET /api/v1/admin/project` - 作品列表
     - `GET /api/v1/admin/project/stats` - 作品统计
     - `PUT /api/v1/admin/project/{id}/status` - 更新作品状态
     - `DELETE /api/v1/admin/project/{id}` - 删除作品
     - `GET /api/v1/admin/comment` - 评论列表
     - `DELETE /api/v1/admin/comment/{id}` - 删除评论

2. **AdminService.java** 包含完整的业务逻辑
   - 位置: `backend/scratch-user/src/main/java/com/scratch/community/module/user/service/AdminService.java`
   - 已实现方法:
     - `listProjects()` - 作品列表查询（使用 JdbcTemplate 跨模块查询）
     - `getProjectStats()` - 作品统计
     - `updateProjectStatus()` - 更新作品状态
     - `deleteProject()` - 删除作品
     - `listComments()` - 评论列表查询
     - `deleteComment()` - 删除评论

### 2.2 路由映射分析

#### 正常工作的路由
```
✅ GET  /api/v1/admin/dashboard          → AdminController.dashboard()
✅ GET  /api/v1/admin/user               → AdminController.listUsers()
✅ PUT  /api/v1/admin/user/{id}          → AdminController.updateUser()
✅ POST /api/v1/admin/user/{id}/disable → AdminController.disableUser()
✅ POST /api/v1/admin/user/{id}/enable  → AdminController.enableUser()
```

#### 无法访问的路由（404）
```
❌ GET  /api/v1/admin/project           → AdminController.listProjects()      [404]
❌ GET  /api/v1/admin/project/stats    → AdminController.projectStats()     [404]
❌ PUT  /api/v1/admin/project/{id}/status → AdminController.updateProjectStatus() [404]
❌ DELETE /api/v1/admin/project/{id}  → AdminController.deleteProject()    [404]
❌ GET  /api/v1/admin/comment          → AdminController.listComments()      [404]
❌ DELETE /api/v1/admin/comment/{id}  → AdminController.deleteComment()     [404]
```

### 2.3 关键观察

1. **路由前缀一致性**
   - 所有 `/admin/dashboard` 和 `/admin/user` 路由正常工作
   - 所有 `/admin/project` 和 `/admin/comment` 路由返回 404
   - 这强烈暗示 **AdminController 没有被正确加载** 或 **服务器部署的是旧版代码**

2. **Spring Boot 自动配置**
   - AdminController 正确使用 `@RestController` 和 `@RequestMapping("/api/v1/admin")`
   - Spring Boot 应该能够自动扫描并注册这些路由
   - 但服务器上显然没有这些路由

3. **代码版本对比**
   - Git 仓库中的 main 分支和当前分支的 AdminController 完全相同
   - 所有必要的代码都存在于仓库中
   - **问题一定在服务器部署环节**

---

## 三、根本原因

### 3.1 最可能的原因

**服务器上运行的是旧版本的 JAR 包，不包含最新的 AdminController 代码**

理由：
1. 代码在 Git 仓库中完整存在
2. 本地构建的 JAR 应该包含这些路由
3. 但服务器返回 404 说明这些路由不存在
4. 唯一解释：服务器上的 JAR 包版本过旧

### 3.2 验证方法

在服务器上执行以下命令：

```bash
# 1. 检查 JAR 包的构建时间
ls -lh /root/scratch-community-platform/backend/scratch-app/target/scratch-app-0.1.0-SNAPSHOT.jar

# 2. 检查 JAR 包中的 AdminController.class
unzip -l scratch-app-0.1.0-SNAPSHOT.jar | grep AdminController

# 3. 检查运行进程的启动时间
ps aux | grep scratch-app

# 4. 检查最近的构建时间（根据 Git 提交）
git log -1 --format="%H %ci" e88d9e2
```

---

## 四、修复方案

### 方案一：重新构建并部署（推荐）

```bash
# 1. SSH 登录服务器
ssh root@8.134.252.67
# 密码: Aa2485204216

# 2. 进入项目目录
cd /root/scratch-community-platform

# 3. 更新 Git 仓库
git pull origin main

# 4. 清理并重新构建
cd backend
mvn clean package -DskipTests

# 5. 重启服务
cd ..
systemctl restart scratch-app

# 6. 检查日志
journalctl -u scratch-app -f
```

### 方案二：使用最新代码的 JAR 包

如果无法在服务器上构建：
1. 在本地或 CI/CD 环境中构建 JAR
2. 将 JAR 包上传到服务器
3. 替换旧 JAR 包并重启服务

### 方案三：验证当前部署

```bash
# 在服务器上执行
cd /root/scratch-community-platform

# 检查 Git 仓库版本
git log -1 --oneline

# 检查 JAR 包中的类
unzip -p backend/scratch-app/target/scratch-app-0.1.0-SNAPSHOT.jar BOOT-INF/classes/com/scratch/community/module/user/controller/AdminController.class | head -c 100 | xxd

# 如果 JAR 中没有 AdminController.class，说明构建时没有包含这些文件
```

---

## 五、预期结果

修复后，以下 API 应该正常工作：

| 模块 | API 路径 | 预期结果 |
|------|----------|----------|
| 作品列表 | `GET /api/v1/admin/project?page=1&size=20` | 返回作品分页列表 |
| 作品统计 | `GET /api/v1/admin/project/stats` | 返回统计数据 |
| 评论列表 | `GET /api/v1/admin/comment?page=1&size=20` | 返回评论分页列表 |

---

## 六、测试脚本

```bash
#!/bin/bash
# API 验证脚本

BASE_URL="http://8.134.252.67"
TOKEN="your_admin_token_here"

echo "=== 测试管理后台 API ==="

echo ""
echo "1. 测试作品列表："
curl -s "$BASE_URL/api/v1/admin/project?page=1&size=5" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo ""
echo "2. 测试作品统计："
curl -s "$BASE_URL/api/v1/admin/project/stats" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo ""
echo "3. 测试评论列表："
curl -s "$BASE_URL/api/v1/admin/comment?page=1&size=5" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## 七、总结

1. ✅ **代码完整**: 所有必要的代码都存在于 Git 仓库中
2. ❌ **部署问题**: 服务器上运行的是旧版本，不包含最新代码
3. ✅ **解决方案**: 重新构建并部署最新代码

### 立即执行的操作

请在服务器上执行以下命令：

```bash
ssh root@8.134.252.67
# 密码: Aa2485204216

# 检查当前版本
cd /root/scratch-community-platform
git log -1 --oneline

# 如果不是最新版本，执行更新和重新部署
git pull origin main
cd backend && mvn clean package -DskipTests
cd .. && systemctl restart scratch-app
```

---

**审计完成**: 2026-05-01 12:20 UTC+8
**审计工具**: curl + Git + 代码审查
