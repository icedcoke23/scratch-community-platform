# Scratch Community Platform

> 面向少儿编程的 Scratch 编程社区平台 — 创作、分享、判题、教学一体化

[![CI](https://github.com/icedcoke23/scratch-community-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/icedcoke23/scratch-community-platform/actions/workflows/ci.yml)
[![JDK](https://img.shields.io/badge/JDK-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## ✨ 核心功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 🎨 **创作引擎** | 项目 CRUD、sb3 上传/解析/下载、Remix（Fork） | ✅ |
| 🤖 **AI 点评** | 5 维度自动分析（代码结构/创意/复杂度/可读性/最佳实践） | ✅ |
| 📚 **教学管理** | 班级创建、作业布置/提交/批改、学情分析 | ✅ |
| 🏆 **竞赛系统** | 竞赛创建/报名/答题/排名、自动状态流转 | ✅ |
| 🎯 **判题系统** | 选择题/判断题/Scratch 编程题、异步判题、AC/WA/TLE/RE | ✅ |
| 💰 **积分体系** | 5 种积分规则、等级系统、签到、排行榜 | ✅ |
| 👥 **社区互动** | 发布/浏览、点赞/评论、最新/最热 Feed（时间衰减排序）、排行榜 | ✅ |
| 🔧 **系统管理** | 内容审核（敏感词）、用户管理、通知系统、配置管理 | ✅ |
| 📱 **移动端** | 响应式适配（768px/480px 断点）、底部 Tab 导航 | ✅ |
| 🔒 **安全架构** | JWT 双令牌、@RateLimit 自定义限流、路径遍历防护、幂等性 | ✅ |

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx (前端静态)                       │
├─────────────────────────────────────────────────────────┤
│              Spring Boot 3.2 模块化单体 (JDK 17)         │
│  ┌─────────┬──────────┬─────────┬─────────┬──────────┐  │
│  │  user   │  editor  │ social  │  judge  │classroom │  │
│  │ 用户系统 │ 创作引擎  │ 社区系统 │ 判题系统 │ 教学管理  │  │
│  ├─────────┴──────────┴─────────┴─────────┴──────────┤  │
│  │  common-security (JWT/认证/角色/幂等性)              │  │
│  │  common-redis (分布式锁/限流/缓存/调度锁)            │  │
│  │  common-core (返回体/异常/事件/工具/配置)            │  │
│  │  common-audit (敏感词过滤)                          │  │
│  │         sb3-parser / judge-core (共享库)             │  │
│  └────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  MySQL 8.0  │  Redis 7  │  MinIO  │  Node.js Sandbox    │
│  (业务数据)   │  (缓存/排行) │ (文件存储) │ (进程隔离判题)     │
└─────────────────────────────────────────────────────────┘
```

**部署单元**：2 个（Spring Boot 单体 + Node.js 沙箱）

### 🔒 v2.0 安全架构

```
请求 → Nginx → [TraceId] → [限流拦截器] → [CORS 校验] → [JWT 认证] → [角色校验] → Controller
                                                        ↓
                                              [DFA 敏感词过滤]
                                                        ↓
                                              [Redisson 分布式锁]（积分等并发场景）
                                                        ↓
                                              [Resilience4j 熔断器]（沙箱调用保护）
                                                        ↓
                                              [子进程隔离判题]（sandbox）
```

---

## 📸 技术栈

| 层级 | 选型 |
|------|------|
| 后端 | Spring Boot 3.2 + JDK 17 + MyBatis-Plus 3.5 |
| 前端 | Vue 3 + TypeScript + Vite 8 + Element Plus + Pinia |
| 判题沙箱 | Node.js 22 + scratch-vm headless（进程隔离） |
| 数据库 | MySQL 8.0 + Redis 7 + MinIO |
| 分布式锁 | Redisson 3.28 |
| 接口限流 | 自研滑动窗口限流器（IP 维度 + @RateLimit 注解） |
| 熔断器 | Resilience4j（沙箱调用保护，滑动窗口 + 半开探测） |
| 内容审核 | DFA 敏感词自动机 |
| 链路追踪 | TraceId（MDC + X-Trace-Id 请求头） |
| 线程池监控 | Micrometer 指标暴露（active/pool/queue/completed） |
| 异步线程池 | 3 独立线程池（判题/SSE/事件监听）+ 队列健康检查 |
| 乐观锁 | MyBatis-Plus @Version（project/user/homework/ranking） |
| 本地缓存 | Caffeine（1000 条 / 10 分钟过期） |
| 分布式调度 | ShedLock + Redis |
| 部署 | Docker + Docker Compose + GitHub Actions CI/CD |
| 数据库迁移 | Flyway（V1-V19，19 个版本化迁移） |
| E2E 测试 | Playwright | 最新 |
| 性能压测 | k6 | 最新 |
| 监控 | Prometheus + Grafana | 最新 |

---

## 🚀 快速启动

### 前置条件

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 22+

### 1. 启动基础设施

```bash
cd docker/compose
docker-compose up -d mysql redis minio
```

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar scratch-app/target/*.jar
```

或开发模式：

```bash
cd backend
mvn spring-boot:run -pl scratch-app
```

### 3. 启动判题沙箱

```bash
cd sandbox
npm install
npm start
```

### 4. 验证

```bash
# 健康检查
curl http://localhost:8080/api/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

### 5. 一键启动（Docker Compose）

```bash
# 配置环境变量
cp .env.example .env
# 修改 JWT_SECRET 等敏感配置

cd docker/compose
docker-compose up -d
# 前端：http://localhost:3000
# 后端 API：http://localhost:8080
# Swagger UI：http://localhost:8080/swagger-ui.html
# MinIO 控制台：http://localhost:9001
# 沙箱健康检查：http://localhost:8081/health
```

---

## 📁 项目结构

```
scratch-community-platform/
├── backend/                    Spring Boot 3 多模块 (JDK 17)
│   ├── scratch-app/            启动模块
│   ├── scratch-common-core/    公共核心层 (返回体/异常/事件/工具/配置)
│   ├── scratch-common-redis/   公共 Redis 层 (分布式锁/限流/缓存/调度锁)
│   ├── scratch-common-security/公共安全层 (JWT/认证/角色/幂等性)
│   ├── scratch-common-audit/   公共审核层 (敏感词过滤)
│   ├── scratch-user/           用户系统
│   ├── scratch-editor/         创作引擎
│   ├── scratch-social/         社区系统
│   ├── scratch-judge/          判题系统
│   ├── scratch-classroom/      教室管理
│   ├── scratch-system/         系统管理
│   ├── scratch-sb3/            sb3 解析库 (共享库)
│   └── scratch-judge-core/     判题核心库 (共享库)
├── frontend-vue/               Vue 3 前端 (TypeScript + Vite + Element Plus)
├── sandbox/                    Node.js 判题沙箱 (scratch-vm headless)
├── docker/                     Docker Compose + Dockerfile + 初始化 SQL
├── docs/                       开发文档 + 设计文档 + 归档文档
└── scripts/                    联调测试脚本
```

---

## 📖 文档

> 📌 完整文档索引请查看 [docs/INDEX.md](docs/INDEX.md)

### 核心文档

| 文档 | 说明 |
|------|------|
| [🕳️ 踩坑记录](docs/PITFALLS.md) | **104 条坑 + 129 条经验总结** — 必读 |
| [📋 开发计划](docs/DEV_PLAN.md) | Phase 规划 + Sprint 任务 + 里程碑 |
| [📐 编码规范](docs/CODING_STANDARDS.md) | Java / TypeScript / 数据库 / API / Git / 安全规范 |
| [🧩 模块开发指南](docs/MODULE_DEV_GUIDE.md) | 从 0 到 1 的标准开发流程 |
| [✅ 质量检查清单](docs/QA_CHECKLIST.md) | PR 自检 + Sprint 审计模板 |
| [🚢 部署指南](docs/DEPLOYMENT.md) | Docker Compose 部署 + 环境变量配置 |

### 设计文档

| 文档 | 说明 |
|------|------|
| [📊 ER 图](docs/ER_DIAGRAM.md) | 21 张表的完整实体关系图（Mermaid） |
| [📐 架构决策记录](docs/ADR.md) | 6 个关键架构决策的上下文与权衡 |
| [📊 综合分析报告](docs/COMPREHENSIVE_ANALYSIS.md) | 架构 / CI / 代码 / 安全 / 数据库 / API 全面分析 |
| [📁 归档文档](docs/archive/) | 历次审计报告 + 前期设计文档 |

---

## 🗺️ 开发路线

### ✅ 已完成 (Phase 1-8, Sprint 1-29)

```
Phase 1  ✅ MVP 核心闭环 (Sprint 1-7)  — 用户/创作/判题/社区/教学/竞赛/积分/管理
Phase 2  ✅ 教学闭环增强 (Sprint 8-10) — AdminService + 前端 + 批改 + 单测
Phase 3  ✅ 增强体验 (Sprint 11-14)   — AI点评/积分/Remix/学情/移动端
Phase 4  ✅ 架构优化 (v2.0)           — 沙箱隔离/敏感词/分布式锁/限流
Phase 5  ✅ 安全与架构 (v2.3)         — SSE Token/幂等性/事件驱动解耦
Phase 6  ✅ 体验增强 (v2.5)           — Scratch编辑器/PWA/数据看板/搜索
Phase 7  ✅ 架构深度优化 (v2.6-v2.7)  — 关联表/缓存/乐观锁/安全加固
Phase 8  ✅ Vue 3 前端全面建设 (Sprint 15-28) — 14页面/i18n/PWA/深色模式
```

### ✅ Phase 9: 深度优化 (v2.9.0)

```
v2.9.0  ✅ 深度优化 — ProjectService修复/滑动窗口限流/Token Refresh修复/App.vue拆分
```

### ✅ Phase 9.5: Sprint 30 质量加固 (v3.0.0)

```
30.1  ✅ ProjectService 补全作者信息 + 点赞状态 (v2.9.0)
30.2  ✅ 集成测试修复 — TestRedisMockConfig + CI 更新
30.3  ✅ Token Refresh 后端接口 (v2.9.0)
30.4  ✅ 清理过时分支 (feature/optimization-v3 + optimize/v2.0)
30.5  📋 E2E 测试框架 (Playwright) — 延后到 Sprint 31
30.6  ✅ ScratchEditorView i18n 全覆盖 (35 个翻译键)
```

### ✅ Phase 10: 生产就绪 + 安全加固 (Sprint 31-32, v3.1.0-v3.2.1)

```
v3.1.0  ✅ 生产就绪基础设施 — E2E/压测/Docker Compose Prod/Nginx/Grafana
v3.2.0  ✅ 安全与性能深度优化 — JWT双令牌/分页限制/索引补充/Token验证
v3.2.1  ✅ CI 修复 + init.sql 同步 — Dockerfile模块引用/init.sql V8-V18/代码清理
```

### ✅ Phase 10.5: 二次深度优化 (v3.3.0)

```
v3.3.0  ✅ 二次优化 — Role枚举/Redis限流修复/AI缓存/WebSocket重连/日志统一
```

### ✅ Phase 11: 全面架构优化 (v3.4.0)

```
v3.4.0  ✅ 架构优化 — V19索引/Token竞态修复/路径遍历防护/SseToken清理/路由组件化/RateLimit注解
```

### ✅ Phase 12: 深度优化 (v3.5.0)

```
v3.5.0  ✅ 深度优化 — Feed时间衰减/Caffeine缓存/ErrorBoundary增强/类型安全/Token计算简化
```

### ✅ Phase 13: 可观测性与韧性 (v3.6.0)

```
v3.6.0  ✅ P0/P1 优化 — TraceId链路追踪/线程池Micrometer监控/Resilience4j熔断器/幂等性扩展/ErrorCode增强
```

### 📋 未来: Phase 14-15

```
Phase 14  📋 测试加固        — Testcontainers集成测试/ProjectService/FeedService/CollabService 测试覆盖
Phase 15  📋 体验优化        — 组件拆分/i18n字典外置/PWA完善/通知归档/OpenAPI代码生成
```

详细计划请查看 [docs/DEV_PLAN.md](docs/DEV_PLAN.md)

---

## 🔧 API 概览

启动后访问 Swagger UI 查看完整 API 文档：`http://localhost:8080/swagger-ui.html`

| 模块 | API 数量 | 核心接口 |
|------|---------|---------|
| user | 10+ | 注册/登录/关注/班级管理 |
| editor | 11 | 项目 CRUD/sb3 上传/发布/Remix |
| social | 12 | 点赞/评论/Feed/排行榜 |
| judge | 9 | 题目管理/判题/提交记录 |
| classroom | 12 | 作业布置/提交/批改/学情分析 |
| system | 7 | 通知/审核/配置 |
| admin | 5 | 用户管理/数据面板 |
| point | 5 | 积分/签到/排行榜 |
| competition | 7 | 竞赛/报名/排名 |
| ai-review | 3 | AI 点评生成/查询 |

---

## 🧪 测试

```bash
# 后端单元测试（CI 跑的模块）
cd backend && mvn test -B -pl scratch-common-core,scratch-common-redis,scratch-common-security,scratch-common-audit,scratch-user,scratch-editor,scratch-judge,scratch-social,scratch-classroom,scratch-system,scratch-sb3,scratch-app

# 后端全量测试（含集成测试，需要 Redis）
cd backend && mvn test -B

# 前端测试
cd frontend-vue && npm test

# 联调测试脚本
bash scripts/api-test.sh
```

| 测试类型 | 数量 | 覆盖范围 |
|---------|------|---------|
| 后端单元测试 | 7 个测试 (通过 CI) | Service 层核心逻辑 |
| 前端单元测试 | 14 个文件 / 166 个测试 | API / 组件 / 路由 / Store / Composable |
| API 集成测试 | 3 个测试文件 | Controller 层（需 Redis） |
| E2E 测试 | 4 个测试文件 | Playwright 核心流程覆盖 |
| 性能压测 | 4 个场景 | k6 脚本（健康检查/Feed/排行榜/登录） |

---

## 📊 质量报告

| 报告 | 说明 |
|------|------|
| [第三轮深度审计](docs/THIRD_DEEP_ANALYSIS.md) | v3.5.0 全面审计：编译/测试/API/架构/安全/数据库/CI + P0/P1 优化实施 |
| [综合分析报告](docs/COMPREHENSIVE_ANALYSIS.md) | 全栈深度分析：架构 / CI / 安全 / 数据库 / API / 沙箱 |
| [全面优化报告](docs/FULL_OPTIMIZATION_REPORT.md) | v3.4.0 优化分析：后端/前端/数据库/安全/测试 |
| [二次审计报告](docs/SECOND_OPTIMIZATION_AUDIT.md) | v3.4.0 优化后审计：修改清单 + 风险评估 |
| [踩坑记录](docs/PITFALLS.md) | **104 条坑 + 129 条经验总结** — 必读 |
| [审计历史总结](docs/archive/audits/README.md) | 6 轮审计的关键发现和改进历程 |

---

## 🤝 贡献

欢迎贡献！请阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解开发流程和规范。

---

## 📄 License

[MIT License](LICENSE)

---

## 📞 联系

如有问题，请通过 [GitHub Issues](https://github.com/icedcoke23/scratch-community-platform/issues) 反馈。
