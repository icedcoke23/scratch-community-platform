# Scratch Community Platform - 深度分析报告

> 分析日期: 2026-04-30 | 分析人: AI Assistant

---

## 一、项目概览

**项目名称**: Scratch Community Platform  
**定位**: 面向少儿编程的 Scratch 社区平台 — 创作、分享、判题、教学一体化  
**技术栈**: Spring Boot 3.2 + Vue 3 + TypeScript + MySQL 8 + Redis 7 + MinIO  

---

## 二、系统架构分析

### 2.1 整体架构

采用**模块化单体**架构（Modular Monolith），后端拆分为 13 个 Maven 模块：

```
模块化单体 (Spring Boot 3.2)
├── scratch-app          → 主应用入口
├── scratch-user         → 用户系统
├── scratch-editor       → 创作引擎（项目 CRUD/SB3 上传）
├── scratch-social       → 社区系统（Feed/点赞/评论）
├── scratch-judge        → 判题系统
├── scratch-classroom    → 教学管理
├── scratch-system       → 系统管理
├── scratch-common-core  → 公共基础（返回体/异常/事件/工具）
├── scratch-common-redis → Redis 封装（分布式锁/限流/缓存）
├── scratch-common-security → JWT/认证/角色/幂等性
├── scratch-common-audit → DFA 敏感词过滤
├── scratch-sb3          → SB3 文件解析库
└── scratch-judge-core   → 判题核心库
```

**评价**: ✅ 优秀的模块化设计，关注点分离清晰，各模块通过事件机制解耦。

### 2.2 部署架构

```
Nginx (Port 80)
├── /         → Vue 3 SPA (静态文件)
├── /api/*    → Spring Boot (Port 8080)
├── /ws/      → WebSocket (协作编辑)
└── /health   → 健康检查
    ↓
Spring Boot 3.2 (JDK 17)
├── MySQL 8.0  → 业务数据
├── Redis 7    → 缓存/排行/限流
├── MinIO      → 文件存储 (sb3/头像/素材)
└── Node.js Sandbox (Port 8081) → scratch-vm 判题沙箱
```

---

## 三、后端架构分析

### 3.1 API 设计

RESTful 风格，版本化 (`/api/v1/`)，共 50+ 端点：

| 模块 | 端点数 | 关键特性 |
|------|--------|----------|
| 用户 | 8+ | JWT 双令牌、OAuth (微信/QQ)、关注系统 |
| 项目 | 12+ | CRUD、SB3 上传/下载、Remix 链、自动保存 |
| 社区 | 5+ | Feed 流、点赞、评论（楼中楼） |
| 判题 | 5+ | 多题型提交、异步判题、结果查询 |
| 教学 | 8+ | 班级管理、作业布置/提交/批改 |
| 管理 | 15+ | 用户/作品/评论/审核/配置管理 |
| AI | 2+ | AI 点评（SSE 流式） |

### 3.2 安全架构

- **JWT 双令牌**: Access Token (15min) + Refresh Token (7d)
- **Token 黑名单**: Redis TTL 实现即时失效
- **滑动窗口限流**: IP + 端点维度
- **幂等性保护**: 关键操作防重复提交
- **敏感词过滤**: DFA 算法
- **路径遍历防护**: 文件上传安全校验
- **XSS 防护**: Jackson XSS Filter + XSS Filter Config
- **TraceId**: 全链路日志关联

### 3.3 数据库设计

18 张核心表，覆盖：
- 用户系统 (4 表): user, user_follow, class, class_student
- 创作系统 (4 表): project, project_like, project_comment, sb3_parse_result
- 题库+判题 (5 表): problem, submission, homework, homework_submission
- 积分+通知+审核 (3 表): points_log, notification, content_audit_log
- 课程系统 (3 表): course, course_unit, learning_progress
- 素材库 (1 表): asset

**评价**: ✅ 表设计规范，索引合理，预留了竞赛系统字段。

---

## 四、前端架构分析

### 4.1 技术栈

- **Vue 3.5** + TypeScript + Vite
- **Element Plus** UI 组件库
- **Pinia** 状态管理
- **Vue Router** 路由
- **Axios** HTTP 客户端 (自动 Token 刷新)

### 4.2 模块划分

| 模块 | 组件数 | 路由数 |
|------|--------|--------|
| 社区 | 5 页面 | /feed, /project/:id, /user/:id, /rank, /search |
| 编辑器 | 1 页面 | /editor, /editor/:id |
| 判题 | 3 页面 | /problems, /competition, /competition/:id |
| 教学 | 5 页面 | /class, /homework, /homework/create, /homework/:id, /analytics |
| 管理 | 10 页面 | /admin/* (AdminLayout + 9 子页面) |
| 其他 | 6 页面 | /points, /collab, /notifications, /settings, /achievements, 404 |

### 4.3 Scratch 编辑器集成（当前实现）

**当前方案**: 通过 iframe 嵌入 turbowarp.org

```
ScratchEditorView.vue
├── 构建 URL: https://turbowarp.org/editor?project_url=<backend_sb3_url>
├── iframe 嵌入
├── ScratchBridge (postMessage 通信)
│   ├── project-changed → 标记 dirty
│   ├── project-save → 自动保存 sb3
│   ├── enter-editor / enter-player → 模式切换
│   └── exportProject → 请求导出
└── 自动保存 → POST /api/v1/project/{id}/sb3/auto-save
```

**问题**:
1. ❌ 依赖外部服务 turbowarp.org
2. ❌ CORS 限制（需 Nginx 代理加头）
3. ❌ 无法深度定制编辑器
4. ❌ 创建项目进入的是预览界面，非编辑器
5. ❌ iframe 通信受跨域限制

---

## 五、CI/CD 分析

### 5.1 CI 流水线 (.github/workflows/ci.yml)

```
Jobs:
├── backend    → JDK 17, mvn compile/test/package
├── frontend   → Node 22, npm ci/test/build
├── sandbox    → Node 18, npm install/test
├── docker     → Docker build verify
├── lint       → Checkstyle + compile check
└── deploy     → 仅 main 分支触发
```

### 5.2 CD 流水线 (.github/workflows/deploy.yml)

```
Jobs:
├── version        → 生成版本号
├── build-backend  → Docker build + push to GHCR
├── build-frontend → Docker build + push to GHCR
├── build-sandbox  → Docker build + push to GHCR
├── deploy         → SSH 部署到服务器
├── verify         → 健康检查 + 冒烟测试
└── release        → 创建 GitHub Release
```

**评价**: ✅ CI/CD 设计完善，有回滚机制、健康检查、自动备份。

---

## 六、判题沙箱

Node.js 进程隔离，使用 scratch-vm 运行 Scratch 项目：
- seccomp 安全策略
- Docker 容器化
- 异步判题 + Resilience4j 熔断

---

## 七、文档

- README.md: 完善，包含架构图、API 概览、快速开始
- docs/ADR.md: 架构决策记录
- docs/QA_CHECKLIST.md: QA 检查清单
- docs/archive/planning/: 详细的架构设计、竞品分析、数据库设计文档
- CONTRIBUTING.md: 贡献指南
- CHANGELOG.md: 变更日志

**评价**: ✅ 文档非常完善，设计文档有多个迭代版本。

---

## 八、总结与建议

### 优势
1. 模块化架构清晰，扩展性好
2. 安全设计全面（JWT双令牌/限流/幂等性/XSS防护）
3. CI/CD 完善，有自动化部署和回滚
4. 文档详尽，有设计迭代记录
5. 测试覆盖合理（352 个测试）

### 需改进
1. **Scratch 编辑器**: 当前依赖外部 turbowarp.org iframe，需自托管
2. **创建项目流程**: 应直接进入编辑器，而非预览页
3. **编辑器通信**: postMessage 跨域通信受限，自托管后可深度集成

---

## 九、TurboWarp 自托管方案

### 方案: 本地构建 TurboWarp scratch-gui

1. 克隆 TurboWarp/scratch-gui
2. 本地构建 (npm install + npm run build)
3. 将构建产物放入 frontend-vue/public/turbowarp/
4. 修改编辑器组件指向本地路径
5. Nginx 配置 CORS 头支持 sb3 加载
6. 修改 ScratchBridge 适配本地 TurboWarp 通信协议

### 优势
- ✅ 完全自主可控
- ✅ 无外部依赖
- ✅ 可深度定制
- ✅ 无 CORS 问题（同域）
- ✅ 通信更可靠（可直接访问 VM）
