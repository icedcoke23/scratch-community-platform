# Scratch 社区平台 — Vue 3 前端

> Vue 3 + TypeScript + Vite + Element Plus + Pinia

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5+ | 核心框架（Composition API + `<script setup>`） |
| TypeScript | 6.0+ | 类型安全 |
| Vite | 8.x | 构建工具 |
| Element Plus | 2.13+ | UI 组件库（按需导入） |
| Pinia | 3.0+ | 状态管理 |
| Vue Router | 4.6+ | 路由管理 |
| Axios | 1.15+ | HTTP 客户端 |
| Vitest | - | 单元测试 |
| happy-dom | - | 测试环境 |

## 项目结构

```
src/
├── api/                    # API 客户端
│   └── index.ts           # Axios 实例 + 所有 API 方法
├── components/             # 通用组件
│   ├── CommentList.vue     # 评论列表
│   ├── EmptyState.vue      # 空状态占位
│   ├── ErrorBoundary.vue   # 错误边界
│   ├── JudgeDetail.vue     # 判题详情
│   ├── LoadingSkeleton.vue # 骨架屏
│   ├── LoginDialog.vue     # 登录弹窗
│   ├── ProjectCard.vue     # 项目卡片
│   └── ProjectPreview.vue  # 项目预览
├── composables/            # 组合式函数
│   ├── useLoading.ts       # 加载状态管理
│   ├── useSseStream.ts     # SSE 流式通信
│   └── index.ts
├── router/                 # 路由配置
│   └── index.ts            # 14 个路由 + 守卫 + Loading bar
├── stores/                 # Pinia 状态管理
│   ├── notification.ts     # 通知（未读数 + 轮询）
│   ├── project.ts          # 项目（详情 + 缓存）
│   └── user.ts             # 用户（认证 + 角色）
├── types/                  # TypeScript 类型定义
│   └── index.ts            # 20+ 接口
├── utils/                  # 工具函数
│   ├── errorHandler.ts     # 全局错误处理
│   └── index.ts            # 时间/格式化/枚举
├── views/                  # 页面组件
│   ├── admin/
│   │   └── AdminView.vue           # 管理后台
│   ├── classroom/
│   │   ├── AnalyticsView.vue       # 学情分析
│   │   ├── HomeworkDetailView.vue  # 作业详情
│   │   └── HomeworkView.vue        # 作业列表
│   ├── editor/
│   │   └── ScratchEditorView.vue   # Scratch 编辑器
│   ├── judge/
│   │   ├── CompetitionDetailView.vue # 竞赛详情
│   │   ├── CompetitionView.vue       # 竞赛列表
│   │   └── ProblemsView.vue          # 题库
│   ├── points/
│   │   └── PointsView.vue          # 积分中心
│   └── social/
│       ├── FeedView.vue             # 社区信息流
│       ├── ProjectDetailView.vue    # 项目详情
│       └── RankView.vue             # 排行榜
├── NotificationsView.vue   # 通知中心
├── App.vue                 # 根组件（导航 + 登录/注册）
├── main.ts                 # 入口（Pinia + Router + Loading bar）
└── auto-imports.d.ts       # Element Plus 自动导入类型
```

## 核心特性

### 状态管理
- **useUserStore**: 用户认证、角色判断、localStorage 持久化
- **useProjectStore**: 项目详情缓存、列表缓存管理
- **useNotificationStore**: 未读通知数、轮询管理、已读标记

### API 层
- 统一的请求/响应拦截（Token 注入、错误处理、时间同步）
- API 版本化：`/api/v1/` 前缀
- SSE 一次性 Token 机制（安全的流式通信）
- 429/401/500 统一错误处理

### 路由
- 14 个路由（全部懒加载）
- 路由守卫（认证 + 角色权限）
- 自定义 CSS Loading bar
- fade-slide 切换动画

### 移动端适配
- 768px / 480px 响应式断点
- 底部 Tab 导航（safe-area-inset 适配）
- Element Plus 组件移动端样式优化

## 开发

```bash
# 安装依赖
npm install

# 开发服务器（自动代理到后端 /api/v1）
npm run dev

# 类型检查
npx vue-tsc --noEmit

# 运行测试
npm test

# 测试覆盖率
npx vitest run --coverage

# 构建
npm run build
```

## 测试

```
src/__tests__/
├── api.test.ts         # API 模块结构测试 (6)
├── components.test.ts  # 组件测试 (18)
├── errorHandler.test.ts # 错误处理测试
├── router.test.ts      # 路由配置测试 (4)
├── sse.test.ts         # SSE 单元测试 (5)
├── stores.test.ts      # Store 测试 (22)
└── utils.test.ts       # 工具函数测试 (14)
```

## 代理配置

开发模式下 Vite 自动代理 `/api` 请求到后端：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true }
  }
}
```
