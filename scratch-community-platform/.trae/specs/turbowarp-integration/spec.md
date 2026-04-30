# Scratch 社区平台 - TurboWarp 深度集成 - Product Requirement Document

## Overview
- **Summary**: 完善 TurboWarp 集成方案，实现完整的后端 API、项目管理、SB3 处理、双向通信协议和测试，打造一个可用的 Scratch 项目创建、编辑和分享平台。
- **Purpose**: 解决 TurboWarp 集成过程中的跨域问题、项目加载问题和功能不完整问题，提供稳定可靠的开发体验和用户体验。
- **Target Users**: 
  - Scratch 项目创作者
  - 开发者（使用此平台）
  - 项目管理员

## Goals
1. 实现完整的后端 API 支持项目管理
2. 完善 TurboWarp 双向通信协议
3. 提供稳定的 SB3 文件上传下载和预览
4. 添加测试保证代码质量
5. 完善项目管理功能（创建、保存、发布、删除）

## Non-Goals (Out of Scope)
- 完整的用户认证和权限系统（超出本次范围）
- 社区社交功能（评论、点赞等）
- 自定义 TurboWarp 扩展开发
- 完全自托管 TurboWarp（使用 CDN wrapper 方案）

## Background & Context
- 已完成轻量级 TurboWarp iframe wrapper 方案
- 缺少后端 API 实现
- 通信协议需要完善
- 需要全面的测试覆盖
- 架构采用 Vue 3 + Vite 前端，Express 后端

## Functional Requirements
- **FR-1**: 用户可以创建新的 Scratch 项目
- **FR-2**: 用户可以编辑已有的 Scratch 项目
- **FR-3**: 用户可以保存和发布项目
- **FR-4**: 用户可以上传和下载 SB3 文件
- **FR-5**: TurboWarp 编辑器正常加载并展示项目
- **FR-6**: 项目预览功能正常工作
- **FR-7**: 支持项目列表和管理

## Non-Functional Requirements
- **NFR-1**: 编辑器加载时间 < 5s（首次），< 2s（缓存后）
- **NFR-2**: 响应式设计，支持移动端和桌面端
- **NFR-3**: 完整的类型安全和错误处理
- **NFR-4**: 后端 API 响应时间 < 500ms
- **NFR-5**: 代码覆盖率 > 80%（核心逻辑）

## Constraints
- **Technical**: Node.js + Express 后端，Vue 3 + Vite 前端
- **Business**: 需在当前架构基础上实现，不进行大的重构
- **Dependencies**: TurboWarp 官方 CDN，项目依赖的 npm 包

## Assumptions
- 后端 API 将使用 Express + TypeScript
- 项目数据将存储在内存或简单 JSON 文件中（可后续替换为数据库）
- TurboWarp CDN 可正常访问
- 用户已安装 Node.js 18+

## Acceptance Criteria

### AC-1: 创建新项目
- **Given**: 用户访问首页
- **When**: 点击"创建新项目"按钮，进入编辑器页面
- **Then**: 编辑器正常加载，显示一个新项目
- **Verification**: `programmatic`
- **Notes**: 需验证 iframe ready 消息

### AC-2: 加载已有项目
- **Given**: 存在一个项目 ID 为 1 的项目
- **When**: 访问 `/editor/1`
- **Then**: TurboWarp 编辑器加载并显示该项目内容
- **Verification**: `programmatic`
- **Notes**: 验证项目数据正确加载

### AC-3: 保存项目
- **Given**: 用户在编辑器中修改了项目
- **When**: 点击"保存"按钮
- **Then**: 项目保存成功，显示成功消息
- **Verification**: `programmatic`

### AC-4: 上传 SB3 文件
- **Given**: 用户有一个 SB3 文件
- **When**: 选择文件上传
- **Then**: 文件成功保存，编辑器加载该项目
- **Verification**: `programmatic`

### AC-5: 项目预览
- **Given**: 存在一个项目
- **When**: 在首页或项目列表中预览该项目
- **Then**: ScratchPreview 组件正常加载并播放项目
- **Verification**: `human-judgment`
- **Notes**: 验证项目内容和交互功能

### AC-6: 发布项目
- **Given**: 用户有一个草稿项目
- **When**: 点击"发布"按钮
- **Then**: 项目状态变更为已发布
- **Verification**: `programmatic`

### AC-7: 后端 API 完整性
- **Given**: 后端服务正在运行
- **When**: 调用各个 API 端点
- **Then**: 返回正确的响应格式
- **Verification**: `programmatic`

### AC-8: 类型安全
- **Given**: 代码库使用 TypeScript
- **When**: 编译 TypeScript
- **Then**: 没有类型错误
- **Verification**: `programmatic`

## Open Questions
- [ ] 是否需要用户登录认证？（可选，当前非必须）
- [ ] SB3 文件存储位置？（当前可存在内存或 public 文件夹）
- [ ] 是否需要版本历史功能？（当前超出范围）
