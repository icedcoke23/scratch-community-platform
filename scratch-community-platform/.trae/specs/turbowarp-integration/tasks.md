# Scratch 社区平台 - The Implementation Plan (Decomposed and Prioritized Task List)

## [ ] Task 1: 设置后端基础设施
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 创建 backend 目录结构
  - 初始化 Node.js + Express + TypeScript 项目
  - 配置基本的服务器和路由
- **Acceptance Criteria Addressed**: [AC-7]
- **Test Requirements**:
  - `programmatic` TR-1.1: 后端服务可以正常启动在 8080 端口
  - `programmatic` TR-1.2: 基本的健康检查端点返回 200
  - `human-judgement` TR-1.3: 目录结构清晰，配置合理
- **Notes**: 使用内存存储简化初始实现

## [ ] Task 2: 实现项目管理 API
- **Priority**: P0
- **Depends On**: [Task 1]
- **Description**: 
  - 实现项目 CRUD API
  - 实现 SB3 文件上传/下载
  - 添加项目发布功能
  - 配置正确的 CORS
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3, AC-4, AC-6, AC-7]
- **Test Requirements**:
  - `programmatic` TR-2.1: POST /api/v1/project 创建项目返回正确响应
  - `programmatic` TR-2.2: GET /api/v1/project/:id 返回正确的项目详情
  - `programmatic` TR-2.3: PUT /api/v1/project/:id 更新项目成功
  - `programmatic` TR-2.4: POST /api/v1/project/:id/publish 发布项目成功
  - `programmatic` TR-2.5: POST /api/v1/project/:id/sb3/upload 上传文件成功
  - `programmatic` TR-2.6: GET /api/v1/project/:id/sb3/download 可以下载文件
  - `human-judgement` TR-2.7: CORS 配置正确，前端可以正常访问
- **Notes**: SB3 文件可临时存放在 public 文件夹或内存中

## [ ] Task 3: 完善 TurboWarp 双向通信协议
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 完善 wrapper HTML 的消息转发机制
  - 实现 `project-loaded` 和其他重要消息
  - 改进 scratchBridge.ts
- **Acceptance Criteria Addressed**: [AC-1, AC-2]
- **Test Requirements**:
  - `programmatic` TR-3.1: iframe 发送 `iframe-ready` 消息
  - `programmatic` TR-3.2: project_url 参数正确传递给 TurboWarp
  - `human-judgement` TR-3.3: 消息转发机制清晰
- **Notes**: 无需复杂功能，保持轻量级和稳定性

## [ ] Task 4: 完善前端编辑器组件
- **Priority**: P0
- **Depends On**: [Task 2, Task 3]
- **Description**: 
  - 完善 ScratchEditorView.vue 的保存和加载逻辑
  - 实现项目标题编辑
  - 改进错误处理
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3]
- **Test Requirements**:
  - `programmatic` TR-4.1: 创建新项目流程正常工作
  - `programmatic` TR-4.2: 加载已有项目正常
  - `programmatic` TR-4.3: 保存按钮调用正确 API
  - `human-judgement` TR-4.4: 用户体验流畅，错误提示友好
- **Notes**: 集成后端 API

## [ ] Task 5: 完善项目预览组件
- **Priority**: P1
- **Depends On**: [Task 2]
- **Description**: 
  - 修复 ScratchPreview.vue 中的问题
  - 完善加载状态和错误处理
  - 验证预览功能
- **Acceptance Criteria Addressed**: [AC-5]
- **Test Requirements**:
  - `programmatic` TR-5.1: 预览组件正确渲染
  - `human-judgement` TR-5.2: 点击加载预览功能正常
  - `human-judgement` TR-5.3: 全屏功能正常
- **Notes**: 修复语法错误和逻辑问题

## [ ] Task 6: 实现项目列表页面
- **Priority**: P1
- **Depends On**: [Task 2]
- **Description**: 
  - 创建 ProjectList.vue 组件
  - 添加路由 /projects
  - 显示所有项目列表
  - 支持查看、编辑和删除项目
- **Acceptance Criteria Addressed**: [AC-7]
- **Test Requirements**:
  - `programmatic` TR-6.1: 项目列表 API 正常返回数据
  - `human-judgement` TR-6.2: 页面布局美观，交互流畅
- **Notes**: 简单的列表展示，无需复杂过滤排序

## [ ] Task 7: 添加类型安全和错误处理
- **Priority**: P1
- **Depends On**: [Task 1, Task 2]
- **Description**: 
  - 确保 TypeScript 类型完整
  - 添加前端和后端的错误处理
  - 提供友好的用户错误提示
- **Acceptance Criteria Addressed**: [AC-8]
- **Test Requirements**:
  - `programmatic` TR-7.1: `npm run build`（前端）和 `tsc --noEmit`（后端）不报错
  - `human-judgement` TR-7.2: 错误处理完善，边界情况考虑周全
- **Notes**: 检查所有类型定义和错误边界

## [ ] Task 8: 集成测试和调试
- **Priority**: P2
- **Depends On**: [Task 4, Task 5, Task 6]
- **Description**: 
  - 创建集成测试流程文档
  - 手动测试所有主要功能
  - 修复发现的问题
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3, AC-4, AC-5, AC-6]
- **Test Requirements**:
  - `human-judgement` TR-8.1: 端到端流程正常工作
  - `human-judgement` TR-8.2: 测试文档完整
- **Notes**: 先确保手动测试通过

## [ ] Task 9: 更新文档和部署说明
- **Priority**: P2
- **Depends On**: [Task 8]
- **Description**: 
  - 更新 README.md
  - 添加快速开始指南
  - 更新 TURBOWARP_INTEGRATION.md
- **Acceptance Criteria Addressed**: All
- **Test Requirements**:
  - `human-judgement` TR-9.1: 文档完整，易于理解
  - `human-judgement` TR-9.2: 快速开始步骤可操作
- **Notes**: 确保新用户能快速上手
