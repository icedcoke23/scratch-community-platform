# Scratch 社区平台 v4.0 - 全面深度优化 - 实现计划

## Sprint 34: 移动端 App 完整开发 - 项目初始化 (v3.9.0)
**优先级**：🔴 P0 | **预估**：3 小时 | **状态**：📋 待开始

### [ ] Task 34.1: 移动端技术栈配置
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - uni-app 项目初始化
  - TypeScript 配置
  - Vite 构建配置
  - ESLint + Prettier 配置
  - 目录结构设计
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `programmatic` TR-34.1.1：项目编译通过
  - `programmatic` TR-34.1.2：基础测试通过

### [ ] Task 34.2: 移动端 API 层封装
- **Priority**：P0
- **Depends On**：34.1
- **Description**：
  - API 请求封装
  - Token 管理
  - 错误处理
  - 拦截器配置
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `programmatic` TR-34.2.1：API 请求测试

## Sprint 35: 移动端 App 核心功能开发 (v3.9.1)
**优先级**：🔴 P0 | **预估**：5 小时 | **状态**：📋 待开始

### [ ] Task 35.1: 移动端用户认证模块
- **Priority**：P0
- **Depends On**：34.2
- **Description**：
  - 登录注册页面
  - Token 管理（内存存储）
  - 用户信息管理
  - 第三方登录（微信、QQ）
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-35.1.1：登录注册功能测试

### [ ] Task 35.2: 移动端社区浏览模块
- **Priority**：P0
- **Depends On**：35.1
- **Description**：
  - 项目列表（瀑布流）
  - 项目详情
  - 项目预览（内嵌播放器）
  - 点赞和收藏
  - 评论功能
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-35.2.1：社区浏览体验测试

### [ ] Task 35.3: 移动端个人主页模块
- **Priority**：P0
- **Depends On**：35.2
- **Description**：
  - 用户主页展示
  - 项目作品展示
  - 关注和粉丝
  - 用户设置
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-35.3.1：个人主页功能测试

## Sprint 36: 移动端高级功能开发 (v3.9.2)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 36.1: 移动端离线缓存和同步
- **Priority**：P1
- **Depends On**：35.3
- **Description**：
  - 本地存储配置
  - 离线数据缓存
  - 数据同步机制
  - 冲突处理
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `programmatic` TR-36.1.1：离线功能测试

### [ ] Task 36.2: 移动端推送通知
- **Priority**：P1
- **Depends On**：36.1
- **Description**：
  - 推送通知配置
  - 通知接收处理
  - 通知列表展示
  - 通知设置
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-36.2.1：推送通知测试

### [ ] Task 36.3: 移动端编辑器适配
- **Priority**：P1
- **Depends On**：36.2
- **Description**：
  - Scratch 移动端适配
  - 触摸交互优化
  - 性能优化
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-36.3.1：编辑器移动端测试

## Sprint 37: 大规模 AI 功能升级 - 架构设计 (v3.10.0)
**优先级**：🔴 P0 | **预估**：3 小时 | **状态**：📋 待开始

### [ ] Task 37.1: AI 多模型架构设计
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - AI 模型抽象层设计
  - 多模型支持（GPT-4、Claude、Gemini）
  - 模型切换策略
  - 负载均衡设计
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-37.1.1：架构设计评审

### [ ] Task 37.2: AI 模型集成实现
- **Priority**：P0
- **Depends On**：37.1
- **Description**：
  - OpenAI API 集成
  - Claude API 集成
  - Gemini API 集成
  - 统一调用接口
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-37.2.1：API 集成测试

## Sprint 38: 大规模 AI 功能升级 - 核心功能 (v3.10.1)
**优先级**：🔴 P0 | **预估**：5 小时 | **状态**：📋 待开始

### [ ] Task 38.1: 智能代码分析和优化建议
- **Priority**：P0
- **Depends On**：37.2
- **Description**：
  - 代码结构分析
  - 性能问题检测
  - 优化建议生成
  - 代码质量评估
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-38.1.1：代码分析功能测试
  - `human-judgment` TR-38.1.2：分析准确性评估

### [ ] Task 38.2: AI 助教智能答疑系统
- **Priority**：P0
- **Depends On**：38.1
- **Description**：
  - 对话式问答界面
  - 上下文理解
  - 知识库检索
  - 答案生成和推荐
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `human-judgment` TR-38.2.1：答疑功能体验测试
  - `programmatic` TR-38.2.2：问答准确性测试

### [ ] Task 38.3: 个性化学习路径推荐
- **Priority**：P1
- **Depends On**：38.2
- **Description**：
  - 用户行为分析
  - 学习进度跟踪
  - 课程推荐算法
  - 个性化内容生成
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `human-judgment` TR-38.3.1：推荐准确性评估

## Sprint 39: 大规模 AI 功能升级 - 高级功能 (v3.10.2)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 39.1: AI 生成内容审核
- **Priority**：P1
- **Depends On**：38.3
- **Description**：
  - 内容安全检测
  - 敏感词过滤
  - 违规内容处理
  - 审核日志记录
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-39.1.1：审核功能测试

### [ ] Task 39.2: AI 对话式学习助手
- **Priority**：P1
- **Depends On**：39.1
- **Description**：
  - 多轮对话支持
  - 学习引导
  - 进度追踪
  - 激励系统
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `human-judgment` TR-39.2.1：学习助手体验测试

### [ ] Task 39.3: 项目代码智能诊断
- **Priority**：P1
- **Depends On**：39.2
- **Description**：
  - 代码问题诊断
  - 错误原因分析
  - 修复建议
  - 自动化修复
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-39.3.1：诊断准确性测试

## Sprint 40: TypeScript 类型安全与首页修复 (v3.11.0)
**优先级**：🔴 P0 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 40.1: 修复 HomeView TypeScript 类型错误
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 修复 Slide 类型定义（添加 description 字段）
  - 修复 API 调用类型问题（使用正确的 socialApi）
  - 修复 toast 使用方式
  - 确保所有类型安全
- **Acceptance Criteria Addressed**：AC-3
- **Test Requirements**：
  - `programmatic` TR-40.1.1：`npm run build:check` 通过，无类型错误
  - `human-judgment` TR-40.1.2：首页可以正常渲染和交互

### [ ] Task 40.2: 修复其他 TypeScript 编译错误
- **Priority**：P0
- **Depends On**：40.1
- **Description**：
  - 修复所有 Vue 组件类型错误
  - 修复所有 API 层类型错误
  - 修复所有 composables 类型错误
  - 修复所有 stores 类型错误
  - 修复 vite.config.ts 配置问题
- **Acceptance Criteria Addressed**：AC-3
- **Test Requirements**：
  - `programmatic` TR-40.2.1：TypeScript 编译完全通过
  - `programmatic` TR-40.2.2：所有测试通过（166/166）

## Sprint 41: 前端深度体验优化 (v3.12.0)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 41.1: 首页加载体验优化
- **Priority**：P1
- **Depends On**：40.2
- **Description**：
  - 骨架屏优化
  - 错误边界完善
  - 空状态设计
  - 加载失败重试
- **Acceptance Criteria Addressed**：AC-7, AC-11
- **Test Requirements**：
  - `human-judgment` TR-41.1.1：用户体验测试
  - `programmatic` TR-41.1.2：性能测试 LCP < 1.5s

### [ ] Task 41.2: 编辑器可靠性优化
- **Priority**：P1
- **Depends On**：41.1
- **Description**：
  - 完善 loading 状态
  - 超时兜底机制
  - 错误处理和重试
  - postMessage 安全加固
  - 参考 v3.8.1 经验
- **Acceptance Criteria Addressed**：AC-8
- **Test Requirements**：
  - `human-judgment` TR-41.2.1：编辑器加载测试
  - `programmatic` TR-41.2.2：错误处理测试

### [ ] Task 41.3: 通知中心与用户设置完善
- **Priority**：P1
- **Depends On**：41.2
- **Description**：
  - 通知中心优化
  - 用户设置完善
  - 主题切换
  - 隐私设置
- **Acceptance Criteria Addressed**：FR-5
- **Test Requirements**：
  - `human-judgment` TR-41.3.1：功能测试

## Sprint 42: Refresh Token 安全加固 (v3.13.0)
**优先级**：🔴 P0 | **预估**：3 小时 | **状态**：📋 待开始

### [ ] Task 42.1: 后端 Refresh Token httpOnly Cookie 实现
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 修改 LoginController.refresh 接口
  - 设置 Cookie 属性：HttpOnly + Secure + SameSite=Strict
  - Cookie Path: /
  - Max-Age: 7 天
  - 完善 Refresh Token 验证逻辑
  - 防止 CSRF 攻击
- **Acceptance Criteria Addressed**：AC-4
- **Test Requirements**：
  - `programmatic` TR-42.1.1：单元测试覆盖
  - `programmatic` TR-42.1.2：集成测试验证 Cookie 属性
  - `human-judgment` TR-42.1.3：浏览器验证无法访问 Cookie

### [ ] Task 42.2: 前端 Token 刷新逻辑适配
- **Priority**：P0
- **Depends On**：42.1
- **Description**：
  - 修改 useAuth composable
  - Access Token 改为内存存储（Pinia state）
  - Refresh Token 自动通过 Cookie 携带
  - 刷新逻辑简化
- **Acceptance Criteria Addressed**：AC-4
- **Test Requirements**：
  - `programmatic` TR-42.2.1：前端测试通过
  - `human-judgment` TR-42.2.2：手动测试 Token 刷新流程

## Sprint 43: Testcontainers 与判题回调 (v3.13.1)
**优先级**：🔴 P0 | **预估**：5 小时 | **状态**：📋 待开始

### [ ] Task 43.1: Testcontainers 集成测试
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - Testcontainers 配置
  - @Container 自动管理 Redis/MySQL
  - User/Project/Social/Judge 模块集成测试
  - CI 集成配置
- **Acceptance Criteria Addressed**：AC-5
- **Test Requirements**：
  - `programmatic` TR-43.1.1：所有集成测试通过
  - `programmatic` TR-43.1.2：CI 可以正常运行测试

### [ ] Task 43.2: 判题回调机制
- **Priority**：P0
- **Depends On**：43.1
- **Description**：
  - 判题回调接口 POST /api/v1/judge/callback
  - Sandbox 回调发送
  - SSE 推送给前端
  - 移除轮询逻辑
- **Acceptance Criteria Addressed**：AC-6
- **Test Requirements**：
  - `programmatic` TR-43.2.1：回调功能测试
  - `human-judgment` TR-43.2.2：实时更新测试

## Sprint 44: 判题队列与通知表分区 (v3.14.0)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 44.1: 判题队列 Redis Stream 实现
- **Priority**：P1
- **Depends On**：43.2
- **Description**：
  - Redis Stream 配置
  - 判题任务生产
  - 判题任务消费
  - 消费组管理
  - 死信队列处理
- **Acceptance Criteria Addressed**：AC-10
- **Test Requirements**：
  - `programmatic` TR-44.1.1：队列测试
  - `programmatic` TR-44.1.2：并发测试

### [ ] Task 44.2: notification 表分区
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 分区方案设计（按月）
  - Flyway V21 迁移脚本
  - 查询优化
  - 归档策略
- **Acceptance Criteria Addressed**：AC-9
- **Test Requirements**：
  - `programmatic` TR-44.2.1：分区测试
  - `programmatic` TR-44.2.2：性能测试

## Sprint 45: 功能完善与清理 (v4.0.0)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 45.1: Competition JSON 字段清理
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 删除 problem_ids JSON 字段
  - 删除 problem_scores JSON 字段
  - 使用 competition_problem 关联表
  - 数据迁移脚本
- **Test Requirements**：
  - `programmatic` TR-45.1.1：迁移测试

### [ ] Task 45.2: CrossModuleWriteRepository 拆分
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - ProjectCountWriter
  - PointWriter
  - 其他细粒度 Writer
- **Test Requirements**：
  - `programmatic` TR-45.2.1：测试通过

### [ ] Task 45.3: 协作编辑完善
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - WebSocket 重连优化
  - 冲突检测和解决
  - 协作历史记录
  - 权限控制
- **Test Requirements**：
  - `human-judgment` TR-45.3.1：多人协作测试

### [ ] Task 45.4: OpenAPI 代码生成（可选）
- **Priority**：P2
- **Depends On**：None
- **Description**：
  - 引入 openapi-typescript-codegen
  - 生成前端 API 客户端
  - 类型安全保证
- **Test Requirements**：
  - `programmatic` TR-45.4.1：生成测试

---

## 📊 Sprint 优先级排序

| # | Sprint | 优先级 | 依赖 | 预估 |
|---|--------|--------|------|------|
| 1 | Sprint 34 | 🔴 P0 | None | 3h |
| 2 | Sprint 35 | 🔴 P0 | 34 | 5h |
| 3 | Sprint 36 | 🟡 P1 | 35 | 4h |
| 4 | Sprint 37 | 🔴 P0 | None | 3h |
| 5 | Sprint 38 | 🔴 P0 | 37 | 5h |
| 6 | Sprint 39 | 🟡 P1 | 38 | 4h |
| 7 | Sprint 40 | 🔴 P0 | None | 4h |
| 8 | Sprint 41 | 🟡 P1 | 40 | 4h |
| 9 | Sprint 42 | 🔴 P0 | None | 3h |
| 10 | Sprint 43 | 🔴 P0 | 42 | 5h |
| 11 | Sprint 44 | 🟡 P1 | 43 | 4h |
| 12 | Sprint 45 | 🟡 P1 | None | 4h |

**总计**：10 周，44 小时

---

## 📋 实施原则

1. **移动优先**：先完成移动端 App 核心功能
2. **AI 升级**：大规模升级 AI 功能，提供智能化体验
3. **类型安全**：修复 TypeScript 错误
4. **安全性**：Refresh Token 安全加固
5. **测试保障**：每个 Sprint 都要有测试
6. **小步迭代**：每个 Sprint 可独立发布
7. **文档同步**：每个 Sprint 同步更新文档

---
