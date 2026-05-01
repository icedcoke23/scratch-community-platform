# Scratch 社区平台 v4.0 - 全面深度优化 - 实现计划

## Sprint 34: 类型安全与首页修复 (v3.8.3)
**优先级**：🔴 P0 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 34.1: 修复 HomeView TypeScript 类型错误
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 修复 Slide 类型定义（添加 description 字段）
  - 修复 API 调用类型问题（使用正确的 socialApi）
  - 修复 toast 使用方式
  - 确保所有类型安全
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-34.1.1：`npm run build:check` 通过，无类型错误
  - `human-judgment` TR-34.1.2：首页可以正常渲染和交互
- **Notes**：从之前的 build:check 错误中逐一定位和修复

### [ ] Task 34.2: 修复其他 TypeScript 编译错误
- **Priority**：P0
- **Depends On**：34.1
- **Description**：
  - 修复 App.vue 中的 User 类型问题
  - 修复 composables.test.ts 中的 cancel 问题
  - 修复 integration.test.ts 中的 directive mounted/updated 问题
  - 修复 stores.test.ts 中的 afterEach 问题
  - 修复 api/collab.ts 参数问题
  - 修复 api/request.ts 的 _isRetry 问题
  - 修复 JudgeDetail.vue type 类型问题
  - 修复 LoginDialog.vue setLogin 问题
  - 修复 OAuthButtons.vue useI18n 问题
  - 修复 useCollabWebSocket.ts 类型问题
  - 修复 useDebounce.ts cancel 问题
  - 修复 stores/notification.ts 类型转换问题
  - 修复 OAuthCallbackView.vue User 类型问题
  - 修复 SettingsView.vue User 类型问题
  - 修复 AdminClassesView.vue records/total 问题
  - 修复 AdminCompetitionsView.vue type 问题
  - 修复 AdminProjectsView.vue type 问题
  - 修复 AdminUsersView.vue email 问题
  - 修复 ProblemManageView.vue 类型比较问题
  - 修复 HomeworkDetailView.vue type 问题
  - 修复 CompetitionDetailView.vue type 问题
  - 修复 PointsView.vue checkedIn/earned 问题
  - 修复 ProjectDetailView.vue records 问题
  - 修复 vite.config.ts esbuild 问题
- **Acceptance Criteria Addressed**：AC-2
- **Test Requirements**：
  - `programmatic` TR-34.2.1：TypeScript 编译完全通过
  - `programmatic` TR-34.2.2：所有测试通过（166/166）

## Sprint 35: Refresh Token 安全加固 (v3.9.0)
**优先级**：🔴 P0 | **预估**：3 小时 | **状态**：📋 待开始

### [ ] Task 35.1: 后端 Refresh Token httpOnly Cookie 实现
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 修改 LoginController.refresh 接口
  - 设置 Cookie 属性：HttpOnly + Secure + SameSite=Strict
  - Cookie Path: /
  - Max-Age: 7 天
  - 完善 Refresh Token 验证逻辑
  - 防止 CSRF 攻击
- **Acceptance Criteria Addressed**：AC-3
- **Test Requirements**：
  - `programmatic` TR-35.1.1：单元测试覆盖
  - `programmatic` TR-35.1.2：集成测试验证 Cookie 属性
  - `human-judgment` TR-35.1.3：浏览器验证无法访问 Cookie

### [ ] Task 35.2: 前端 Token 刷新逻辑适配
- **Priority**：P0
- **Depends On**：35.1
- **Description**：
  - 修改 useAuth composable
  - Access Token 改为内存存储（Pinia state）
  - Refresh Token 自动通过 Cookie 携带
  - 刷新逻辑简化
- **Acceptance Criteria Addressed**：AC-3
- **Test Requirements**：
  - `programmatic` TR-35.2.1：前端测试通过
  - `human-judgment` TR-35.2.2：手动测试 Token 刷新流程

## Sprint 36: 判题回调机制 (v3.10.0)
**优先级**：🔴 P0 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 36.1: 判题回调后端实现
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 实现回调接口 POST /api/v1/judge/callback
  - 验证回调签名防止伪造
  - Redis Pub/Sub 作为备选方案
  - 更新 submission 状态和结果
  - 通过 SSE 推送给前端
- **Acceptance Criteria Addressed**：AC-5
- **Test Requirements**：
  - `programmatic` TR-36.1.1：单元测试覆盖回调验证
  - `programmatic` TR-36.1.2：集成测试完整流程

### [ ] Task 36.2: Sandbox 回调发送
- **Priority**：P0
- **Depends On**：36.1
- **Description**：
  - 修改 sandbox server.js
  - 判题完成后发送 HTTP 回调
  - 重试机制（失败重发 3 次）
  - 超时处理
- **Acceptance Criteria Addressed**：AC-5
- **Test Requirements**：
  - `programmatic` TR-36.2.1：Sandbox 测试通过

### [ ] Task 36.3: 移除轮询逻辑
- **Priority**：P1
- **Depends On**：36.2
- **Description**：
  - 前端移除轮询
  - 后端移除轮询相关逻辑
  - 清理不再需要的代码
- **Acceptance Criteria Addressed**：AC-5
- **Test Requirements**：
  - `human-judgment` TR-36.3.1：测试判题实时更新

## Sprint 37: Testcontainers 集成测试 (v3.11.0)
**优先级**：🔴 P0 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 37.1: Testcontainers 配置
- **Priority**：P0
- **Depends On**：None
- **Description**：
  - 添加 Testcontainers 依赖
  - 配置 @Container 自动管理 Redis/MySQL
  - 配置测试应用上下文
  - 清理测试数据策略
- **Acceptance Criteria Addressed**：AC-4
- **Test Requirements**：
  - `programmatic` TR-37.1.1：测试环境配置正确

### [ ] Task 37.2: 集成测试实现
- **Priority**：P0
- **Depends On**：37.1
- **Description**：
  - User 模块集成测试
  - Project 模块集成测试
  - Social 模块集成测试
  - Judge 模块集成测试
  - CI 集成配置
- **Acceptance Criteria Addressed**：AC-4
- **Test Requirements**：
  - `programmatic` TR-37.2.1：所有集成测试通过
  - `programmatic` TR-37.2.2：CI 可以正常运行测试

## Sprint 38: 课程模块完善 (v3.12.0)
**优先级**：🟡 P1 | **预估**：6 小时 | **状态**：📋 待开始

### [ ] Task 38.1: 课程数据库设计
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - Flyway V20 迁移脚本
  - course 表（课程）
  - chapter 表（章节）
  - lesson 表（课时）
  - enrollment 表（报名）
  - progress 表（学习进度）
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `programmatic` TR-38.1.1：数据库迁移测试

### [ ] Task 38.2: 课程后端 API 实现
- **Priority**：P1
- **Depends On**：38.1
- **Description**：
  - CourseController（课程 CRUD）
  - ChapterController（章节管理）
  - LessonController（课时管理）
  - EnrollmentController（报名）
  - ProgressController（进度追踪）
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `programmatic` TR-38.2.1：API 单元测试
  - `programmatic` TR-38.2.2：集成测试

### [ ] Task 38.3: 课程前端页面完善
- **Priority**：P1
- **Depends On**：38.2
- **Description**：
  - 完善 CoursesView 课程列表
  - 完善 CourseDetailView 课程详情
  - 添加课程管理后台页面
  - 添加学习进度展示
- **Acceptance Criteria Addressed**：AC-1
- **Test Requirements**：
  - `human-judgment` TR-38.3.1：功能测试
  - `programmatic` TR-38.3.2：TypeScript 类型安全

## Sprint 39: 首页与编辑器体验优化 (v3.13.0)
**优先级**：🟡 P1 | **预估**：5 小时 | **状态**：📋 待开始

### [ ] Task 39.1: 首页加载体验优化
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 骨架屏优化
  - 错误边界完善
  - 空状态设计
  - 加载失败重试
- **Acceptance Criteria Addressed**：AC-6, AC-10
- **Test Requirements**：
  - `human-judgment` TR-39.1.1：用户体验测试
  - `programmatic` TR-39.1.2：性能测试 LCP < 1.5s

### [ ] Task 39.2: 编辑器可靠性优化
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 完善 loading 状态
  - 超时兜底机制
  - 错误处理和重试
  - postMessage 安全加固
  - 参考 v3.8.1 经验
- **Acceptance Criteria Addressed**：AC-7
- **Test Requirements**：
  - `human-judgment` TR-39.2.1：编辑器加载测试
  - `programmatic` TR-39.2.2：错误处理测试

## Sprint 40: 通知中心与设置完善 (v3.14.0)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 40.1: 通知中心优化
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 通知按类型分类
  - 批量已读功能
  - 通知删除
  - 实时更新
- **Acceptance Criteria Addressed**：FR-4
- **Test Requirements**：
  - `human-judgment` TR-40.1.1：功能测试

### [ ] Task 40.2: 用户设置完善
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 账号安全设置
  - 通知偏好
  - 主题切换
  - 隐私设置
- **Acceptance Criteria Addressed**：FR-5
- **Test Requirements**：
  - `human-judgment` TR-40.2.1：功能测试

## Sprint 41: 判题队列与 notification 表分区 (v3.15.0)
**优先级**：🟡 P1 | **预估**：5 小时 | **状态**：📋 待开始

### [ ] Task 41.1: 判题队列 Redis Stream 实现
- **Priority**：P1
- **Depends On**：36.3
- **Description**：
  - Redis Stream 配置
  - 判题任务生产
  - 判题任务消费（Worker）
  - 消费组管理
  - 死信队列处理
- **Acceptance Criteria Addressed**：AC-9
- **Test Requirements**：
  - `programmatic` TR-41.1.1：队列测试
  - `programmatic` TR-41.1.2：并发测试

### [ ] Task 41.2: notification 表分区
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 分区方案设计（按月）
  - Flyway V21 迁移脚本
  - 查询优化
  - 归档策略
- **Acceptance Criteria Addressed**：AC-8
- **Test Requirements**：
  - `programmatic` TR-41.2.1：分区测试
  - `programmatic` TR-41.2.2：性能测试

## Sprint 42: 功能完善与清理 (v4.0.0)
**优先级**：🟡 P1 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 42.1: Competition JSON 字段清理
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - 删除 problem_ids JSON 字段
  - 删除 problem_scores JSON 字段
  - 使用 competition_problem 关联表
  - 数据迁移脚本
- **Acceptance Criteria Addressed**：TODO 7
- **Test Requirements**：
  - `programmatic` TR-42.1.1：迁移测试

### [ ] Task 42.2: CrossModuleWriteRepository 拆分
- **Priority**：P1
- **Depends On**：None
- **Description**：
  - ProjectCountWriter
  - PointWriter
  - 其他细粒度 Writer
- **Acceptance Criteria Addressed**：TODO 8
- **Test Requirements**：
  - `programmatic` TR-42.2.1：测试通过

### [ ] Task 42.3: OpenAPI 代码生成（可选）
- **Priority**：P2
- **Depends On**：None
- **Description**：
  - 引入 openapi-typescript-codegen
  - 生成前端 API 客户端
  - 类型安全保证
- **Acceptance Criteria Addressed**：TODO 4
- **Test Requirements**：
  - `programmatic` TR-42.3.1：生成测试

### [ ] Task 42.4: 推荐算法增强
- **Priority**：P2
- **Depends On**：None
- **Description**：
  - 基于用户行为推荐
  - 协同过滤优化
  - 个性化首页推荐
- **Acceptance Criteria Addressed**：FR-2
- **Test Requirements**：
  - `human-judgment` TR-42.4.1：A/B 测试（可选）

## Sprint 43: 协作编辑完善与管理后台增强 (v4.1.0)
**优先级**：🟢 P2 | **预估**：4 小时 | **状态**：📋 待开始

### [ ] Task 43.1: 协作编辑完善
- **Priority**：P2
- **Depends On**：None
- **Description**：
  - WebSocket 重连优化
  - 冲突检测和解决
  - 协作历史
  - 权限控制
- **Acceptance Criteria Addressed**：FR-7
- **Test Requirements**：
  - `human-judgment` TR-43.1.1：多人协作测试

### [ ] Task 43.2: 管理后台增强
- **Priority**：P2
- **Depends On**：38.3
- **Description**：
  - 课程管理功能
  - 学生管理增强
  - 数据统计完善
  - 审核流程优化
- **Acceptance Criteria Addressed**：FR-8
- **Test Requirements**：
  - `human-judgment` TR-43.2.1：管理功能测试

---

## 📊 Sprint 优先级排序

| # | Sprint | 优先级 | 依赖 | 预估 |
|---|--------|--------|------|------|
| 1 | Sprint 34 | 🔴 P0 | None | 4h |
| 2 | Sprint 35 | 🔴 P0 | None | 3h |
| 3 | Sprint 36 | 🔴 P0 | None | 4h |
| 4 | Sprint 37 | 🔴 P0 | None | 4h |
| 5 | Sprint 38 | 🟡 P1 | 34 | 6h |
| 6 | Sprint 39 | 🟡 P1 | 34 | 5h |
| 7 | Sprint 40 | 🟡 P1 | None | 4h |
| 8 | Sprint 41 | 🟡 P1 | 36 | 5h |
| 9 | Sprint 42 | 🟡 P1 | None | 4h |
| 10 | Sprint 43 | 🟢 P2 | 38, 42 | 4h |

**总计**：6 周，39 小时

---

## 📋 实施原则

1. **类型安全优先**：先修复 TypeScript 错误
2. **安全性优先**：Refresh Token 安全加固
3. **测试保障**：每个 Sprint 都要有测试
4. **小步迭代**：每个 Sprint 可独立发布
5. **文档同步**：每个 Sprint 同步更新文档

---
