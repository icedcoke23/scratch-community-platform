# 🔍 深度分析报告 — Scratch Community Platform (2026 Q2)

> 分析时间：2026-04-30 | 分析范围：全栈架构、代码质量、安全、性能、CI/CD

---

## 📊 项目概览

| 维度 | 数据 |
|------|------|
| 前端代码量 | ~21,800 行 (Vue/TS) |
| 后端代码量 | ~21,400 行 (Java) |
| 前端测试 | 166 个 (14 文件) ✅ 全部通过 |
| 后端测试 | 20 个集成测试 ✅ |
| Maven 模块 | 14 个 |
| Vue 组件 | 18 个 |
| API 端点 | 50+ |
| 数据库迁移 | 19 个版本 (Flyway) |

---

## 🏗️ 架构分析

### 后端架构（Spring Boot 3.2 模块化单体）

**模块依赖关系：**
```
scratch-app (主入口)
├── scratch-user (用户/认证)
├── scratch-editor (项目CRUD/sb3管理)
├── scratch-social (社区互动/AI点评/协作)
├── scratch-judge (判题系统)
├── scratch-classroom (教学管理)
├── scratch-system (系统管理/通知)
├── scratch-common-security (JWT/拦截器)
├── scratch-common-redis (缓存/分布式锁)
├── scratch-common-core (通用工具/返回体)
├── scratch-common-audit (敏感词过滤)
├── scratch-sb3 (SB3文件解析)
└── scratch-judge-core (判题核心引擎)
```

**✅ 优点：**
- 清晰的模块边界，按业务域拆分
- 公共模块抽取合理（security/redis/core/audit）
- Flyway 数据库版本管理（19个迁移脚本）
- JWT 双令牌 + 黑名单机制
- 幂等性保护、限流、敏感词过滤
- 事件驱动架构（积分/浏览量异步处理）

**⚠️ 潜在问题：**
- `CrossModuleQueryRepository` / `CrossModuleWriteRepository` 存在跨模块直接数据库查询，耦合度较高
- 部分 Service 类较长（ProjectService ~350行），可考虑拆分
- 缺少全局异常处理器对特定业务异常的细化处理

### 前端架构（Vue 3 + TypeScript + Vite）

**目录结构：**
```
src/
├── api/ (14个API模块，统一Axios封装 + Token自动刷新)
├── components/ (18个可复用组件)
├── views/ (按业务域组织：social/editor/judge/classroom/admin/collab)
├── stores/ (Pinia: user/project/notification)
├── composables/ (11个组合式函数)
├── utils/ (错误处理/日志/ScratchBridge)
├── directives/ (图片懒加载)
└── __tests__/ (14个测试文件)
```

**✅ 优点：**
- TypeScript 类型安全
- Axios 封装完善（Token 自动刷新、防并发、请求取消）
- i18n 双语支持（zh-CN / en）
- 深色模式完整支持
- 响应式设计（768px / 480px 断点）
- 组件设计合理（ScratchPreview / ScratchPreviewDialog / ProjectCard）

**⚠️ 已修复问题：**
1. **ScratchPreview CORS 错误** — TurboWarp 无法从后端拉取 sb3 文件
   - 根因：CORS 仅允许 `localhost:*`，TurboWarp 从 `turbowarp.org` 发起请求被拒绝
   - 修复：新增 presigned URL 公开端点，TurboWarp 直接从 MinIO 拉取，绕过 CORS
2. **ScratchEditorView 嵌入错误** — 使用 `turbowarp.org/editor` 不支持 iframe 嵌入
   - 根因：`/editor` 检测到 iframe 后重定向到 embed 验证页面
   - 修复：改用 `turbowarp.org/embed`（专为 iframe 设计，支持完整编辑器功能）
3. **Service Worker 重复注册** — main.ts 中存在两个相同的注册块
   - 修复：删除重复代码

---

## 🔒 安全分析

### ✅ 已实施的安全措施
- JWT 双令牌（15min + 7d）+ Redis 黑名单
- BCrypt 密码哈希
- 滑动窗口限流（IP + 端点维度）
- DFA 敏感词过滤
- 路径遍历防护（文件上传）
- 幂等性保护（防重复提交）
- 生产密钥强制校验
- CORS 限制（仅允许指定源）
- SQL 注入防护（MyBatis-Plus 参数化查询）
- XSS 防护（Vue 模板自动转义）

### ⚠️ 安全建议
1. **CORS 配置**：`allowCredentials(true)` + `allowedOriginPatterns("*")` 在 Spring 中会抛异常，建议明确列出允许的源
2. **文件上传**：建议增加文件魔数（magic number）校验，不仅依赖 Content-Type
3. **Rate Limiting**：建议对 sb3 上传和 AI 点评接口增加更严格的限流
4. **WebSocket**：协作编辑的 WebSocket 建议增加消息大小限制

---

## 📡 API 设计分析

### RESTful 规范
- ✅ 使用 `/api/v1/` 版本前缀
- ✅ 资源命名规范（`/project/{id}/sb3`）
- ✅ HTTP 方法语义正确（GET/POST/PUT/DELETE）
- ✅ 分页参数统一（`page` + `size`）
- ✅ 统一返回体 `R<T>`（code/msg/data/timestamp）

### 端点分类
| 模块 | 公开端点 | 需认证端点 |
|------|----------|-----------|
| 用户 | 注册/登录/刷新Token | 个人信息/关注 |
| 项目 | 详情/Feed/sb3下载 | CRUD/上传/发布 |
| 社区 | Feed/评论列表/排行 | 点赞/评论/AI点评 |
| 判题 | 题目列表/竞赛列表 | 提交/创建 |
| 班级 | - | 全部需认证 |
| 管理 | - | 全部需管理员 |

---

## 🗄️ 数据库设计分析

### 表结构（19个核心表）
- `user` / `user_follow` — 用户与社交关系
- `project` / `project_like` / `project_comment` — 项目与互动
- `problem` / `submission` — 题目与提交
- `homework` / `homework_submission` / `homework_problem` — 作业系统
- `class` / `class_student` — 班级管理
- `competition` / `competition_problem` / `competition_registration` / `competition_ranking` — 竞赛系统
- `notification` / `content_audit_log` / `system_config` — 系统管理
- `collab_session` / `collab_participant` — 协作编辑

### ✅ 设计优点
- 合理的索引设计（主键/唯一/普通/全文索引）
- 逻辑删除（`deleted` 字段）
- 乐观锁（V14 迁移）
- JSON 字段灵活存储（options/parse_result/problem_ids）

### ⚠️ 优化建议
1. `project` 表的 `like_count` / `comment_count` / `view_count` 使用计数校准调度器（已有 `CountCalibrationScheduler`），建议定期执行
2. 考虑对 `submission` 表按时间分区（数据量大时）
3. `notification` 表建议增加 TTL 自动清理机制

---

## 🧪 测试覆盖

### 前端测试（166个）
| 测试文件 | 数量 | 覆盖范围 |
|----------|------|----------|
| components.test.ts | 30 | 核心组件渲染 |
| components-v2.test.ts | 20 | 组件交互 |
| composables.test.ts | 15 | 组合式函数 |
| stores.test.ts | 14 | Pinia Store |
| utils.test.ts | 17 | 工具函数 |
| api.test.ts | 7 | API 导出 |
| router.test.ts | 4 | 路由配置 |
| integration.test.ts | 10 | 集成流程 |
| errorHandler.test.ts | 10 | 错误处理 |
| sse.test.ts | 5 | SSE 流 |
| theme.test.ts | 7 | 主题切换 |
| audit.test.ts | 12 | 审计工具 |
| infrastructure.test.ts | 16 | 基础设施 |
| features.test.ts | 9 | 功能特性 |

### 后端测试（20个集成测试）
- 用户 API、社交 API、健康检查、限流
- 点赞集成、敏感词过滤
- JWT 工具、Auth 拦截器
- 通知服务、判题服务、竞赛服务
- AI 点评服务

---

## 🚀 CI/CD 分析

### CI 流水线（`.github/workflows/ci.yml`）
```
Backend Build & Test → Frontend Build & Test → Sandbox Build & Test
                    ↓
          Docker Build Verify → Code Quality → Deploy (main only)
```

### CD 流水线（`.github/workflows/deploy.yml`）
```
CI 通过 → 版本生成 → 构建镜像 (backend/frontend/sandbox)
       → SSH 部署 → 健康检查 → 冒烟测试 → GitHub Release
       → 失败自动回滚
```

**✅ 优点：**
- 完整的 CI/CD 流水线
- Docker 镜像构建 + GHCR 推送
- 自动数据库备份
- 健康检查 + 回滚机制
- GitHub Release 自动创建

---

## 📋 已完成修复清单

| # | 问题 | 严重程度 | 状态 |
|---|------|----------|------|
| 1 | ScratchPreview CORS 错误 (Failed to fetch) | 🔴 Critical | ✅ 已修复 |
| 2 | ScratchEditorView 嵌入错误 (Invalid TurboWarp Embed) | 🔴 Critical | ✅ 已修复 |
| 3 | Service Worker 重复注册 | 🟡 Minor | ✅ 已修复 |
| 4 | 新增 presigned URL 公开端点 | 🟢 Enhancement | ✅ 已完成 |
| 5 | WebMvcConfig 排除新端点认证 | 🟢 Enhancement | ✅ 已完成 |

---

## 🔮 后续优化方向

1. **性能优化**：实施虚拟滚动（大列表）、图片 CDN、API 响应缓存
2. **监控增强**：Prometheus + Grafana 仪表盘优化、告警规则细化
3. **测试补充**：增加 E2E 测试（Playwright）、后端单元测试覆盖率提升
4. **文档完善**：API 文档自动生成（Swagger）、部署文档更新
5. **功能扩展**：项目版本历史、批量操作、数据导出

---

*分析完成于 2026-04-30 18:30 CST*
