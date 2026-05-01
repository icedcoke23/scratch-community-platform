# Scratch 社区平台 v4.0 - 全面深度优化 - 验证清单

## ✅ Sprint 34: 类型安全与首页修复验证

### 验证检查点

- [ ] Checkpoint 34.1: HomeView TypeScript 类型错误修复
  - [ ] Slide 类型定义正确（包含 description）
  - [ ] API 调用使用正确的 socialApi
  - [ ] toast 使用方式正确
  - [ ] 首页可以正常渲染
  - [ ] 首页功能正常

- [ ] Checkpoint 34.2: 所有 TypeScript 编译错误修复
  - [ ] App.vue User 类型问题修复
  - [ ] composables.test.ts cancel 问题修复
  - [ ] integration.test.ts directive 问题修复
  - [ ] stores.test.ts afterEach 问题修复
  - [ ] api/collab.ts 参数问题修复
  - [ ] api/request.ts _isRetry 问题修复
  - [ ] JudgeDetail.vue type 问题修复
  - [ ] LoginDialog.vue setLogin 问题修复
  - [ ] OAuthButtons.vue useI18n 问题修复
  - [ ] useCollabWebSocket.ts 类型问题修复
  - [ ] useDebounce.ts cancel 问题修复
  - [ ] stores/notification.ts 类型问题修复
  - [ ] OAuthCallbackView.vue User 类型问题修复
  - [ ] SettingsView.vue User 类型问题修复
  - [ ] AdminClassesView.vue records/total 问题修复
  - [ ] AdminCompetitionsView.vue type 问题修复
  - [ ] AdminProjectsView.vue type 问题修复
  - [ ] AdminUsersView.vue email 问题修复
  - [ ] ProblemManageView.vue 类型比较问题修复
  - [ ] HomeworkDetailView.vue type 问题修复
  - [ ] CompetitionDetailView.vue type 问题修复
  - [ ] PointsView.vue checkedIn/earned 问题修复
  - [ ] ProjectDetailView.vue records 问题修复
  - [ ] vite.config.ts esbuild 问题修复

- [ ] Checkpoint 34.3: 编译检查
  - [ ] npm run build:check 完全通过，无类型错误
  - [ ] 所有测试 166/166 通过

## ✅ Sprint 35: Refresh Token 安全加固验证

- [ ] Checkpoint 35.1: Refresh Token httpOnly Cookie 实现
  - [ ] Cookie 设置为 HttpOnly
  - [ ] Cookie 设置为 Secure（生产环境）
  - [ ] Cookie 设置为 SameSite=Strict
  - [ ] Cookie Max-Age 设置为 7 天
  - [ ] Refresh Token 验证逻辑完善
  - [ ] CSRF 防护完整

- [ ] Checkpoint 35.2: 前端 Token 刷新适配
  - [ ] Access Token 内存存储（Pinia state）
  - [ ] Refresh Token Cookie 自动携带
  - [ ] Token 刷新流程正常工作
  - [ ] 浏览器无法访问 Refresh Token Cookie

## ✅ Sprint 36: 判题回调机制验证

- [ ] Checkpoint 36.1: 判题回调后端实现
  - [ ] POST /api/v1/judge/callback 接口
  - [ ] 回调签名验证
  - [ ] Redis Pub/Sub 备选方案
  - [ ] Submission 状态和结果更新
  - [ ] SSE 推送给前端

- [ ] Checkpoint 36.2: Sandbox 回调发送
  - [ ] Sandbox 发送 HTTP 回调
  - [ ] 重试机制（3 次
  - [ ] 超时处理

- [ ] Checkpoint 36.3: 轮询逻辑移除
  - [ ] 前端轮询逻辑移除
  - [ ] 后端轮询逻辑移除
  - [ ] 判题实时更新正常

## ✅ Sprint 37: Testcontainers 集成测试验证

- [ ] Checkpoint 37.1: Testcontainers 配置
  - [ ] Testcontainers 依赖添加
  - [ ] @Container 自动管理 Redis/MySQL
  - [ ] 测试应用上下文配置
  - [ ] 测试数据清理策略

- [ ] Checkpoint 37.2: 集成测试实现
  - [ ] User 模块集成测试
  - [ ] Project 模块集成测试
  - [ ] Social 模块集成测试
  - [ ] Judge 模块集成测试
  - [ ] CI 可以正常运行测试

## ✅ Sprint 38: 课程模块完善验证

- [ ] Checkpoint 38.1: 课程数据库设计
  - [ ] Flyway V20 迁移脚本
  - [ ] course 表
  - [ ] chapter 表
  - [ ] lesson 表
  - [ ] enrollment 表
  - [ ] progress 表
  - [ ] 迁移测试通过

- [ ] Checkpoint 38.2: 课程后端 API 实现
  - [ ] CourseController 完整
  - [ ] ChapterController 完整
  - [ ] LessonController 完整
  - [ ] EnrollmentController 完整
  - [ ] ProgressController 完整
  - [ ] API 单元测试通过
  - [ ] 集成测试通过

- [ ] Checkpoint 38.3: 课程前端页面完善
  - [ ] CoursesView 课程列表
  - [ ] CourseDetailView 课程详情
  - [ ] 课程管理后台页面
  - [ ] 学习进度展示
  - [ ] TypeScript 类型安全
  - [ ] 功能测试通过

## ✅ Sprint 39: 首页与编辑器体验优化验证

- [ ] Checkpoint 39.1: 首页加载体验优化
  - [ ] 骨架屏优化
  - [ ] 错误边界完善
  - [ ] 空状态设计
  - [ ] 加载失败重试
  - [ ] LCP < 1.5s

- [ ] Checkpoint 39.2: 编辑器可靠性优化
  - [ ] Loading 状态完善
  - [ ] 超时兜底机制
  - [ ] 错误处理和重试
  - [ ] postMessage 安全加固
  - [ ] 编辑器加载测试通过

## ✅ Sprint 40: 通知中心与设置完善验证

- [ ] Checkpoint 40.1: 通知中心优化
  - [ ] 通知按类型分类
  - [ ] 批量已读功能
  - [ ] 通知删除
  - [ ] 通知实时更新

- [ ] Checkpoint 40.2: 用户设置完善
  - [ ] 账号安全设置
  - [ ] 通知偏好
  - [ ] 主题切换
  - [ ] 隐私设置

## ✅ Sprint 41: 判题队列与 notification 表分区验证

- [ ] Checkpoint 41.1: 判题队列 Redis Stream 实现
  - [ ] Redis Stream 配置
  - [ ] 判题任务生产
  - [ ] 判题任务消费
  - [ ] 消费组管理
  - [ ] 死信队列处理
  - [ ] 队列测试通过
  - [ ] 并发测试通过

- [ ] Checkpoint 41.2: notification 表分区
  - [ ] 分区方案设计（按月）
  - [ ] Flyway V21 迁移脚本
  - [ ] 查询优化
  - [ ] 归档策略
  - [ ] 分区测试通过
  - [ ] 性能测试通过

## ✅ Sprint 42: 功能完善与清理验证

- [ ] Checkpoint 42.1: Competition JSON 字段清理
  - [ ] problem_ids JSON 字段删除
  - [ ] problem_scores JSON 字段删除
  - [ ] competition_problem 关联表使用
  - [ ] 数据迁移脚本
  - [ ] 迁移测试通过

- [ ] Checkpoint 42.2: CrossModuleWriteRepository 拆分
  - [ ] ProjectCountWriter
  - [ ] PointWriter
  - [ ] 其他细粒度 Writer
  - [ ] 测试通过

- [ ] Checkpoint 42.3: OpenAPI 代码生成（可选）
  - [ ] openapi-typescript-codegen 引入
  - [ ] 前端 API 客户端生成
  - [ ] 类型安全

- [ ] Checkpoint 42.4: 推荐算法增强
  - [ ] 基于用户行为推荐
  - [ ] 协同过滤优化
  - [ ] 个性化首页推荐

## ✅ Sprint 43: 协作编辑完善与管理后台增强验证

- [ ] Checkpoint 43.1: 协作编辑完善
  - [ ] WebSocket 重连优化
  - [ ] 冲突检测和解决
  - [ ] 协作历史记录
  - [ ] 协作权限控制
  - [ ] 多人协作测试通过

- [ ] Checkpoint 43.2: 管理后台增强
  - [ ] 课程管理功能
  - [ ] 学生管理增强
  - [ ] 数据统计完善
  - [ ] 审核流程优化
  - [ ] 管理功能测试通过

## 📊 总体验证

- [ ] Checkpoint Final: v4.0 总体验证
  - [ ] 所有功能都通过
  - [ ] 所有测试都通过
  - [ ] 所有检查都通过
  - [ ] 文档更新同步
  - [ ] 可以发布生产

---

## 🔍 验证标准

### 程序验证（programmatic）:
- 运行自动化测试
- 检查 CI 流程
- 性能基准测试
- 安全扫描

### 人工验证（human-judgment）:
- 用户体验测试
- 界面美观度
- 操作流畅度
- 文档可读性
