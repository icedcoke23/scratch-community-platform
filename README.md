# 🎨 Scratch Community Platform

> 面向少儿编程的 Scratch 社区平台 — 创作、分享、判题、教学一体化

[![CI](https://github.com/icedcoke23/scratch-community-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/icedcoke23/scratch-community-platform/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen)](https://vuejs.org/)
[![JDK](https://img.shields.io/badge/JDK-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Test Status](https://img.shields.io/badge/tests-352%2F352%20passing-brightgreen)](https://github.com/icedcoke23/scratch-community-platform/actions)

---

<div align="center">

## 🚀 生产就绪 · 全栈开源 · 即刻部署

**后端**：20/20 集成测试通过 · JWT 双令牌 · 黑名单失效 · 限流熔断  
**前端**：Vue 3 + TypeScript + Vite · 166/166 单元测试通过 · 响应式设计 · Scratch 实时预览  
**安全**：生产密钥强制校验 · 敏感词过滤 · 分布式锁 · 幂等性 · TraceId 链路追踪

[快速开始](#-快速开始) • [文档](docs/INDEX.md) • [API 参考](#-api-概览) • [贡献指南](CONTRIBUTING.md)

</div>

---

## ✨ 核心功能

| 模块 | 功能 | 状态 | 版本 |
|------|------|------|------|
| 🎨 **创作引擎** | 项目 CRUD、sb3 上传/解析/下载、Remix（Fork）、**Scratch 实时预览** | ✅ | v3.7.0 |
| 🤖 **AI 点评** | 5 维度自动分析（代码结构/创意/复杂度/可读性/最佳实践） | ✅ | v3.6.0 |
| 📚 **教学管理** | 班级创建/加入/成员管理、作业布置/提交/批改、学情分析 | ✅ | v3.7.0 |
| 🏆 **竞赛系统** | 竞赛创建/报名/答题/排名、实时倒计时、自动状态流转 | ✅ | v3.7.0 |
| 🎯 **判题系统** | 选择题/判断题/Scratch 编程题、异步判题、AC/WA/TLE/RE | ✅ | v3.6.0 |
| 💰 **积分体系** | 5 种积分规则、等级系统、签到、排行榜（时间衰减） | ✅ | v3.6.0 |
| 👥 **社区互动** | 发布/浏览、点赞/评论、Feed 流、排行榜、**快速预览** | ✅ | v3.7.0 |
| 🔧 **系统管理** | 内容审核、用户管理、通知系统、**系统配置管理** | ✅ | v3.7.0 |
| 📱 **移动端** | 响应式适配（768px/480px 断点）、底部 Tab 导航 | ✅ | v3.6.0 |
| 🔒 **安全架构** | JWT 双令牌、滑动窗口限流、路径遍历防护、幂等性 | ✅ | v3.6.0 |

---

## 🏗️ 系统架构

### 部署拓扑

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Nginx (Port 80)                            │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  Frontend (Vue 3 SPA)                                          │ │
│  │  /         → /var/www/html (静态文件)                           │ │
│  │  /api/*   → http://localhost:8080 (后端 API)                    │ │
│  │  /health  → http://localhost:8080/api/health                    │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Spring Boot 3.2 模块化单体 (JDK 17)                    │
│  ┌──────────┬──────────┬─────────┬──────────┬──────────┬──────────┐ │
│  │  user    │ editor   │ social  │  judge   │classroom │  system  │ │
│  │ 用户系统   │ 创作引擎   │ 社区系统 │ 判题系统  │ 教学管理   │ 系统管理   │ │
│  ├──────────┴──────────┴─────────┴──────────┴──────────┴──────────┤ │
│  │  common-security  (JWT/认证/角色/幂等性)                         │ │
│  │  common-redis     (分布式锁/限流/缓存/调度锁)                     │ │
│  │  common-core      (返回体/异常/事件/工具/配置)                   │ │
│  │  common-audit     (DFA 敏感词过滤)                               │ │
│  │  sb3-parser       (sb3 文件解析库)                               │ │
│  │  judge-core       (判题核心库)                                   │ │
│  └──────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────┤
│  MySQL 8.0   │  Redis 7   │  MinIO   │  Node.js Sandbox (Port 8081) │
│  (业务数据)    │ (缓存/排行)  │ (文件存储) │  (scratch-vm 进程隔离判题)   │
└─────────────────────────────────────────────────────────────────────┘
```

### 前端架构

```
frontend-vue/src/
├── api/                 # API 模块（按业务拆分）
│   ├── request.ts       # Axios 封装 + Token 自动刷新
│   ├── user.ts          # 用户 API
│   ├── project.ts       # 项目 API
│   ├── social.ts        # 社区 API
│   ├── admin.ts         # 管理 API
│   └── ...              # 共 14 个 API 模块
├── components/          # 可复用组件
│   ├── AppHeader.vue    # 顶部导航
│   ├── ProjectCard.vue  # 项目卡片（含快速预览）
│   ├── ScratchPreview.vue      # Scratch 项目预览
│   ├── ScratchPreviewDialog.vue # Scratch 预览弹窗
│   ├── CreateProjectFab.vue    # 浮动创建按钮
│   ├── AuthDialog.vue   # 登录/注册弹窗
│   └── ...              # 共 18 个组件
├── views/               # 页面视图
│   ├── social/          # 社区页面（Feed/详情/搜索/排行/用户主页）
│   ├── editor/          # Scratch 编辑器
│   ├── judge/           # 判题系统（题库/竞赛/详情）
│   ├── classroom/       # 教学管理（班级/作业/学情分析）
│   ├── admin/           # 管理后台（仪表盘/审核/配置/题目管理）
│   ├── points/          # 积分系统
│   └── collab/          # 协作编辑
├── stores/              # Pinia 状态管理
├── composables/         # 组合式函数
├── utils/               # 工具函数
└── __tests__/           # 单元测试（166 个）
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | OpenJDK / Temurin |
| Maven | 3.8+ | 构建工具 |
| Node.js | 22+ | 前端构建 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7+ | 缓存/排行/限流 |
| MinIO | 最新 | 文件存储（可选） |

### 1. 克隆项目

```bash
git clone https://github.com/icedcoke23/scratch-community-platform.git
cd scratch-community-platform
```

### 2. 配置环境

```bash
cp .env.example .env
# 编辑 .env，修改数据库密码、JWT 密钥等
```

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar scratch-app/target/*.jar --spring.profiles.active=dev
```

### 4. 启动前端

```bash
cd frontend-vue
npm ci
npm run dev    # 开发模式 → http://localhost:5173
npm run build  # 生产构建
```

### 5. 访问

- 前端：http://localhost:5173（开发）/ http://localhost:80（生产）
- API 文档：http://localhost:8080/swagger-ui.html
- 健康检查：http://localhost:8080/api/health

---

## 📡 API 概览

| 模块 | 端点 | 说明 |
|------|------|------|
| 用户 | `POST /api/v1/user/register` | 注册 |
| 用户 | `POST /api/v1/user/login` | 登录（JWT 双令牌） |
| 用户 | `POST /api/v1/user/refresh` | 刷新 Token |
| 用户 | `GET /api/v1/user/me` | 当前用户信息 |
| 用户 | `POST/DELETE /api/v1/user/{id}/follow` | 关注/取消关注 |
| 班级 | `POST /api/v1/class` | 创建班级 |
| 班级 | `POST /api/v1/class/{id}/join` | 加入班级 |
| 班级 | `GET /api/v1/class/{id}/members` | 班级成员 |
| 项目 | `POST /api/v1/project` | 创建项目 |
| 项目 | `POST /api/v1/project/{id}/sb3` | 上传 SB3 |
| 项目 | `POST /api/v1/project/{id}/publish` | 发布项目 |
| 项目 | `POST /api/v1/project/{id}/remix` | Remix 项目 |
| 社区 | `GET /api/v1/social/feed` | Feed 流 |
| 社区 | `POST /api/v1/social/project/{id}/like` | 点赞 |
| 社区 | `POST /api/v1/social/comment` | 评论 |
| 判题 | `POST /api/v1/judge/submit` | 提交判题 |
| 题目 | `POST /api/v1/problem` | 创建题目 |
| 竞赛 | `POST /api/v1/competition` | 创建竞赛 |
| 竞赛 | `POST /api/v1/competition/{id}/register` | 报名竞赛 |
| 作业 | `POST /api/v1/homework` | 布置作业 |
| 作业 | `POST /api/v1/homework/submit` | 提交作业 |
| 作业 | `POST /api/v1/homework/grade` | 批改作业 |
| 积分 | `GET /api/v1/points/me` | 我的积分 |
| 积分 | `POST /api/v1/points/checkin` | 签到 |
| AI | `POST /api/v1/ai-review/project/{id}` | 生成 AI 点评 |
| AI | `GET /api/v1/ai-review/project/{id}/stream` | SSE 流式点评 |
| 通知 | `GET /api/v1/notification` | 通知列表 |
| 管理 | `GET /api/v1/admin/dashboard` | 管理仪表盘 |
| 管理 | `GET /api/v1/admin/audit` | 内容审核列表 |
| 管理 | `GET /api/v1/admin/config` | 系统配置 |
| 健康 | `GET /api/health` | 健康检查 |

---

## 🧪 测试

```bash
# 后端测试
cd backend && mvn test -B

# 前端测试
cd frontend-vue && npm test

# 前端 E2E 测试
cd frontend-vue && npm run test:e2e
```

| 测试类型 | 数量 | 状态 |
|----------|------|------|
| 后端集成测试 | 20 | ✅ 全部通过 |
| 前端单元测试 | 166 | ✅ 全部通过 |
| **总计** | **186** | ✅ |

---

## 📁 项目结构

```
scratch-community-platform/
├── backend/                    # Spring Boot 后端
│   ├── scratch-app/           # 主应用模块
│   ├── scratch-user/          # 用户模块
│   ├── scratch-editor/        # 创作引擎
│   ├── scratch-social/        # 社区系统
│   ├── scratch-judge/         # 判题系统
│   ├── scratch-classroom/     # 教学管理
│   ├── scratch-system/        # 系统管理
│   ├── scratch-common-*/      # 公共模块
│   ├── scratch-sb3/           # SB3 解析库
│   └── scratch-judge-core/    # 判题核心
├── frontend-vue/              # Vue 3 前端
│   ├── src/
│   │   ├── api/              # API 模块
│   │   ├── components/       # 组件
│   │   ├── views/            # 页面
│   │   ├── stores/           # 状态管理
│   │   ├── composables/      # 组合式函数
│   │   └── utils/            # 工具
│   └── e2e/                  # E2E 测试
├── sandbox/                   # Node.js 判题沙箱
├── docker/                    # Docker 配置
├── docs/                      # 项目文档
├── scripts/                   # 部署脚本
└── .github/workflows/         # CI/CD
```

---

## 🔐 安全特性

- **JWT 双令牌**：Access Token (15min) + Refresh Token (7d)，自动刷新
- **Token 黑名单**：登出/刷新时旧 Token 立即失效（Redis TTL）
- **滑动窗口限流**：IP + 端点维度，Redis 实现
- **敏感词过滤**：DFA 算法，内容发布自动审核
- **路径遍历防护**：文件上传/下载路径安全校验
- **幂等性保护**：关键操作（创建/发布/点赞）防重复提交
- **TraceId 链路追踪**：全链路日志关联
- **熔断器**：判题沙箱调用 Resilience4j 熔断保护
- **生产密钥强制校验**：JWT 密钥禁止使用默认值

---

## 🤝 贡献

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解开发规范。

---

## 📄 License

[MIT License](LICENSE)

---

<div align="center">
  <sub>Built with ❤️ for young coders</sub>
</div>
