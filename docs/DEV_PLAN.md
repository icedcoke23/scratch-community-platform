# 📋 开发计划总览

> 版本：v3.3.0 | 日期：2026-04-28
> 当前阶段：Phase 10.5 — 二次深度优化

---

## 一、项目现状

### 代码规模

| 指标 | 数值 |
|------|------|
| 后端 Java 文件 | 199 个，~16,700 行 |
| 前端 Vue/TS 文件 | 82 个，~11,100 行 |
| Maven 模块 | 10 个（6 业务 + 2 共享库 + 1 启动 + 1 父 POM） |
| 数据库表 | 14 张（Flyway V1-V14） |
| 测试文件 | 后端 16 个 + 前端 14 个 |
| 文档 | 35+ 个 Markdown |
| CI 状态 | ✅ 全部通过（Run #101） |

### 功能完成度

| 模块 | 功能 | 完成度 | 备注 |
|------|------|--------|------|
| 👤 用户系统 | 注册/登录/JWT/关注/班级 | 95% | Token Refresh 接口缺失 |
| 🎨 创作引擎 | 项目 CRUD/sb3 解析/Remix | 90% | 作者信息+点赞状态 TODO |
| 👥 社区互动 | 点赞/评论/Feed/排行榜/AI 点评 | 95% | 生产就绪 |
| 🏆 判题系统 | 选择题/Scratch 编程题/异步判题 | 85% | 沙箱隔离待加强 |
| 📚 教学管理 | 班级/作业/批改/学情分析 | 90% | 基本就绪 |
| 🏅 竞赛系统 | 创建/报名/答题/排名 | 85% | 基本就绪 |
| 💰 积分体系 | 5 种规则/等级/签到/排行榜 | 95% | 生产就绪 |
| 🔧 系统管理 | 审核/通知/配置 | 90% | 基本就绪 |
| 🖥️ 前端 | 14 页面/i18n/PWA/深色模式 | 85% | E2E 测试缺失 |
| 🏗️ 基础设施 | Docker/CI/Nginx | 80% | 部署未实际验证 |

**综合完成度: ~90%**

---

## 二、开发原则

| 原则 | 说明 |
|------|------|
| **先跑通再完善** | 每个 Sprint 首先保证链路跑通，再迭代优化 |
| **每 Sprint 可演示** | Sprint 结束时必须有可演示的增量功能 |
| **文档先行** | 模块开发前先完成该模块的 API 文档 + 数据表确认 |
| **测试伴随** | 核心逻辑必须有单测，接口必须有联调验证 |
| **代码评审** | 每个 PR 至少自审一次，对照 CODING_STANDARDS.md |
| **预留缓冲** | 每 Sprint 预留 1 天用于技术债 + 缓冲 |

---

## 三、已完成阶段

### Phase 1: MVP 核心闭环 ✅ (Sprint 1-7)

```
Sprint 1  ✅ 脚手架 + common 基础层
Sprint 2  ✅ user 模块（用户系统）
Sprint 3  ✅ sb3-parser + editor 模块（创作引擎）
Sprint 4  ✅ judge 模块 + sandbox（判题系统）
Sprint 5  ✅ social 模块（社区系统）
Sprint 6  ✅ classroom 模块（班级 + 作业）
Sprint 7  ✅ system 模块（审核 + 通知 + 管理后台）
```

### Phase 2: 教学闭环增强 ✅ (Sprint 8-10)

```
Sprint 8   ✅ AdminService + 前端骨架 + 联调脚本
Sprint 9   ✅ 前端完善（项目详情/作业/管理后台）+ Sandbox 优化
Sprint 10  ✅ 教师批改 UI + 学生提交 + 单元测试
```

### Phase 3: 增强体验 ✅ (Sprint 11-14)

```
Sprint 11  ✅ 异步判题 + 单元测试 + CI/CD + Flyway
Sprint 12  ✅ 积分体系 + Remix + 学情分析
Sprint 13  ✅ AI 点评系统（LLM + 规则引擎双模式）
Sprint 14  ✅ 移动端适配 + 竞赛系统
```

### Phase 4-8: 架构优化 + 前端迁移 + 全面加固 ✅ (v2.0-v2.8)

```
v2.0  ✅ 判题沙箱进程隔离 / DFA 敏感词 / Redisson 分布式锁 / 接口限流
v2.3  ✅ SSE Token / 幂等性保护 / API 版本管理 / 事件驱动解耦
v2.5  ✅ Scratch 在线编辑器 / PWA / 数据看板 / 全文搜索
v2.6  ✅ JSON→关联表 / Caffeine 缓存 / ShedLock / 性能索引
v2.7  ✅ 统一 Jackson / 乐观锁 / 安全加固 / 文件限制统一
v2.8  ✅ CI 测试修复 / 深度分析报告 / 文档整理
```

### Phase 3.5-3.8: Vue 3 前端全面建设 ✅ (Sprint 15-28)

```
Sprint 15-16  ✅ Vue 3 + TypeScript + Vite 初始化 / 组件提取
Sprint 17     ✅ AI LLM 接入（OpenAI 兼容 / 流式输出）
Sprint 18     ✅ Scratch 在线编辑器（TurboWarp iframe）
Sprint 19     ✅ 后端单测 + API 集成测试框架
Sprint 20     ✅ SSE / 全局状态 / 路由 Loading bar
Sprint 21     ✅ 用户主页 / 深色模式 / 分享对话框
Sprint 22     ✅ i18n / ScratchBridge / 图片懒加载 / PWA
Sprint 23     ✅ 组件集成 + 性能优化
Sprint 24     ✅ useDebounce / 设置 / 成就系统
Sprint 25     ✅ 组件库扩展（BreadcrumbNav / VirtualList）
Sprint 26     ✅ i18n 全覆盖 + Swagger 注解补全
Sprint 27     ✅ i18n 深化 + 后端文档完善
Sprint 28     ✅ i18n 全覆盖 + 后端验证增强
Sprint 29     ✅ CI 修复 + 深度分析 + 文档整理
```

---

## 四、当前阶段: Phase 9 — 质量加固 + 功能补全

### Sprint 30: 功能补全 + 质量保障 ✅

**目标**: 修复用户可感知的功能缺陷，补齐测试覆盖。

| # | 任务 | 优先级 | 预估 | 状态 |
|---|------|--------|------|------|
| 30.1 | ProjectService 补全作者信息 + 点赞状态 | 🔴 P0 | 3h | ✅ 完成 |
| 30.2 | 集成测试修复（Testcontainers 或 MockBean） | 🔴 P0 | 4h | ✅ 完成 |
| 30.3 | Token Refresh 后端接口 | 🟡 P1 | 3h | ✅ 完成 |
| 30.4 | 清理 optimize/v2.0 分支 | 🟡 P1 | 0.5h | ✅ 完成 |
| 30.5 | ZIP 路径遍历防护 | 🔴 P0 | 2h | ✅ 完成 |
| 30.6 | Docker 网络隔离 | 🟡 P1 | 1h | ✅ 完成 |
| 30.7 | scratch-common 模块拆分 | 🟡 P1 | 4h | ✅ 完成 |
| 30.8 | 文档同步更新 | 🟡 P1 | 2h | ✅ 完成 |

### Sprint 31: 生产就绪基础设施 (v3.1.0)

**目标**: 补齐生产环境部署和监控基础设施。

| # | 任务 | 优先级 | 状态 | 说明 |
|---|------|--------|------|------|
| 31.1 | E2E 测试框架 (Playwright) | P1 | ✅ | 4 个测试文件覆盖核心流程 |
| 31.2 | 性能压测脚本 (k6) | P2 | ✅ | 健康检查/Feed/排行榜/登录 |
| 31.3 | 生产 Docker Compose | P0 | ✅ | 含 Prometheus + Grafana 监控 |
| 31.4 | Nginx 生产配置 | P0 | ✅ | HTTPS/CSP/SSE/监控代理 |
| 31.5 | Grafana 仪表板 | P1 | ✅ | QPS/延迟/内存/状态码/线程 |
| 31.6 | 健康检查增强 | P1 | ✅ | JVM/OS/内存信息 |

### Sprint 32: 日志聚合 + CI/CD + OAuth2 (v3.2.0)

**目标**: 补齐生产运维和第三方登录能力。

| # | 任务 | 优先级 | 状态 | 说明 |
|---|------|--------|------|------|
| 32.1 | 日志聚合 (Loki + Promtail) | P1 | ✅ | 集中日志查询 + Grafana 面板 |
| 32.2 | CI/CD 完整部署流水线 | P1 | ✅ | GitHub Actions 自动部署 + 回滚 |
| 32.3 | OAuth2 第三方登录 (微信/QQ) | P1 | ✅ | 后端服务 + 前端组件 + DB 迁移 |
| 32.4 | 沙箱安全加固文档 | P1 | ✅ | cgroup/seccomp/gVisor 方案 |
| 32.5 | 推荐算法服务 | P2 | ✅ | 协同过滤 + 热度 + 时间衰减 |
| 32.6 | 教师学情看板增强 | P2 | ✅ | 活跃度/作业/进度/预警 |
| 32.7 | 协作编辑设计文档 | P2 | ✅ | WebSocket + 乐观锁方案 |
| 32.8 | 移动端 App 方案 | P3 | ✅ | uni-app 技术选型 + 架构设计 |

### Sprint 33: 协作编辑实现 + 移动端 App 骨架 (v3.3.0)

**目标**: 实现协作编辑核心功能和移动端 App 基础框架。

| # | 任务 | 优先级 | 状态 | 说明 |
|---|------|--------|------|------|
| 33.1 | WebSocket 配置 | P0 | ✅ | Spring STOMP + SockJS 端点 |
| 33.2 | 协作会话管理 | P0 | ✅ | 创建/加入/退出/关闭 + DB 迁移 |
| 33.3 | 编辑操作同步 | P0 | ✅ | 乐观锁 + 版本控制 + 冲突检测 |
| 33.4 | 光标同步 + 聊天 | P1 | ✅ | 实时光标位置 + 协作聊天室 |
| 33.5 | 前端协作组件 | P1 | ✅ | CollabToolbar/Cursors/Chat/WebSocket composable |
| 33.6 | 移动端 App 骨架 | P2 | ✅ | uni-app + Feed/详情/排行/个人中心 |
| 33.7 | 移动端 API 封装 | P2 | ✅ | 复用后端 REST API |

### Sprint 30 完成标志

- [x] ProjectDetailVO 返回正确的作者信息（nickname/avatarUrl）
- [x] ProjectDetailVO 返回正确的 isLiked 状态
- [x] scratch-app 集成测试可在 CI 运行
- [x] POST /api/v1/user/refresh 接口可用
- [x] optimize/v2.0 分支已删除
- [x] ZIP 路径遍历防护已添加
- [x] Docker 沙箱网络隔离已配置
- [x] scratch-common 拆分为 4 个细粒度模块
- [x] 文档已同步更新

---

## 五、未来路线图

### Phase 10: 生产就绪 (v3.0) ✅

| 任务 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| 监控告警（Prometheus + Grafana） | P1 | ✅ | 仪表板已配置 |
| HTTPS + 域名配置 | P0 | ✅ | Nginx + Let's Encrypt 配置完成 |
| 数据库备份策略 | P0 | ✅ | 自动备份 + 恢复演练脚本 |
| 生产部署指南 | P0 | ✅ | 完整部署流程文档 |
| E2E 测试框架（Playwright） | P1 | ✅ | 4 个测试文件覆盖核心流程 |
| 性能压测脚本（k6） | P2 | ✅ | 健康检查/Feed/排行榜/登录 |
| 生产部署配置 | P0 | ✅ | docker-compose.prod.yml + nginx.prod.conf |
| 健康检查增强 | P1 | ✅ | JVM/OS/内存信息 |
| 日志聚合（Loki + Promtail） | P1 | ✅ | 集中日志查询 + Grafana 面板 |
| CI/CD 完整部署流水线 | P1 | ✅ | GitHub Actions 自动部署 + 回滚 |

### Phase 11: 功能扩展

| 任务 | 优先级 | 状态 | 说明 |
|------|--------|------|------|
| OAuth2 第三方登录（微信/QQ） | P1 | ✅ | 后端服务 + 前端组件 + DB 迁移 |
| 协作编辑（多人 Scratch 项目） | P2 | ✅ | WebSocket + STOMP + 乐观锁 + 前端组件 |
| 推荐算法（基于用户行为） | P2 | ✅ | 协同过滤 + 热度 + 时间衰减 |
| 沙箱安全加固（gVisor / cgroup） | P1 | ✅ | 文档 + seccomp 配置 |
| 数据看板增强（班级/学校维度） | P2 | ✅ | 活跃度/作业/进度/预警分析 |
| 移动端 App（uni-app / Flutter） | P3 | ✅ | uni-app 骨架 + Feed/详情/排行/个人中心 |

### Phase 12: 规模化

| 任务 | 优先级 | 说明 |
|------|--------|------|
| 微服务拆分（按模块） | P3 | 用户量 >10K 时考虑 |
| 读写分离（MySQL 主从） | P3 | 读多写少场景 |
| CDN 加速（静态资源） | P2 | 全国访问加速 |
| 消息队列（RabbitMQ / Kafka） | P3 | 异步解耦 |

---

## 六、里程碑

| 里程碑 | Sprint | 状态 | 标志 |
|--------|--------|------|------|
| M1: 脚手架 | 1-2 | ✅ | user 模块可用 |
| M2: 创作引擎 | 3 | ✅ | sb3 上传 + 解析 + 项目 CRUD |
| M3: 判题系统 | 4 | ✅ | 提交 → 判题 → 结果 |
| M4: 社区闭环 | 5 | ✅ | 发布 → 浏览 → 点赞 → 评论 |
| M5: 教学闭环 | 6-7 | ✅ | 班级 + 作业 + 审核 |
| M6: Phase 2 | 8-10 | ✅ | AdminService + 前端 + 批改 |
| M7: 增强体验 | 11-14 | ✅ | AI/积分/Remix/移动端 |
| M8: Vue 3 前端 | 15-28 | ✅ | 14 页面 + i18n + PWA |
| M9: 质量加固 | 29-30 | ✅ | CI 修复 + 功能补全 + 测试 + 模块拆分 |
| M10: 生产就绪 | 31-32 | ✅ | E2E/压测/生产Docker/Nginx/Grafana/健康检查/日志聚合/CI-CD/OAuth2 |
| M11: 功能扩展 | 33+ | ✅ | 协作编辑/移动端 App/OAuth2/推荐算法 |

---

## 七、风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 集成测试缺失 | API 层 bug 无法自动发现 | Sprint 30 引入 Testcontainers |
| 沙箱安全不足 | 恶意代码可能逃逸 | 添加 cgroup 限制 + gVisor |
| Token Refresh 缺失 | 用户体验差（频繁登录） | Sprint 30 实现后端接口 |
| 单人开发精力有限 | Sprint 延期 | 严格控制范围，P2/P3 可顺延 |
| 数据量增长 | notification/point_log 表膨胀 | 按时间分区 + 定期归档 |
| 无实际部署验证 | Docker Compose 可能有配置问题 | 在 VPS 上做一次完整部署 |

---

*本文档随开发进展持续更新。每个 Sprint 开始时回顾并调整后续计划。*
