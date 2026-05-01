# Scratch 社区平台 v4.0 - 全面深度优化 - 产品需求文档

## Overview

### Summary

本项目对 Scratch 社区平台进行**前后端全面深度优化**，基于现有 v3.8.2 版本进行功能完善、性能提升、安全性加固和用户体验优化。优化覆盖：前端页面完善、课程模块实现、后端性能提升、TypeScript 类型安全、测试覆盖等多个维度，不包含 PWA 支持。

### Purpose

**解决的核心问题**：
1. 修复现有 TypeScript 类型错误，保证代码质量
2. 完善新增的课程模块功能，包括后端 API 和前端页面
3. 优化现有功能的用户体验，如首页展示、编辑器加载
4. 提升后端性能和稳定性，修复已知安全隐患
5. 实现 TODO.md 中的高优先级任务
6. 参考 teaching-open 的优秀功能进行增强

### Target Users

- **Scratch 学习者**：少儿编程入门学习者
- **编程教师**：使用平台进行教学管理
- **社区用户**：浏览作品、互动交流
- **管理员**：管理内容和用户
- **平台运维者**：确保系统稳定运行

---

## Goals

### 🎯 Phase 18: 课程模块完整实现 (v3.9.0)
- 课程管理后端 API（CRUD）
- 课程详情、课程列表页面功能完善
- 课程章节和课时管理
- 课程报名和学习进度跟踪

### 🎯 Phase 19: TypeScript 类型安全与前端质量提升 (v3.10.0)
- 修复所有 TypeScript 编译错误
- 完善 API 类型定义
- 增强组件 Props 类型安全
- 优化类型安全工具函数

### 🎯 Phase 20: 前端深度体验优化 (v3.11.0)
- 首页优化（加载状态、空状态、动画）
- 编辑器加载优化和错误处理
- 通知中心优化
- 用户设置页面完善
- 移动端体验优化

### 🎯 Phase 21: 后端性能与稳定性 (v3.12.0)
- Refresh Token httpOnly Cookie 实现
- Testcontainers 集成测试
- 判题回调机制
- 性能优化和缓存增强

### 🎯 Phase 22: 功能完善与系统加固 (v4.0.0)
- 判题队列 Redis Stream
- notification 表分区
- Competition JSON 字段清理
- 推荐算法增强
- 安全加固与监控增强

---

## Non-Goals (Out of Scope)

### ❌ 明确排除的功能
1. **PWA 支持** - Service Worker、离线使用等
2. **微服务架构** - 当前阶段保持单体应用架构
3. **移动端 App 完全开发** - 已有骨架即可，不继续深入
4. **CDN 加速部署** - 不包含在本次优化范围内
5. **大规模 AI 功能升级** - 保持现有 AI 点评功能即可
6. **支付系统** - 不引入商业支付功能
7. **多语言完整覆盖** - 保持现有 i18n 框架即可

---

## Background & Context

### 📊 项目现状

| 维度 | 现状 | 备注 |
|------|------|------|
| **版本** | v3.8.2 | 稳定生产版本 |
| **Java 文件** | 199 个 | ~16,700 行 |
| **前端文件** | 82 个 | ~11,100 行 |
| **数据库表** | 14 张 | Flyway V1-V19 |
| **测试文件** | 后端 16 + 前端 14 | 166 个前端测试 |
| **功能模块** | 用户/创作/社区/判题/教学/竞赛/积分/系统/协作 | 95%+ 完成 |
| **后端测试** | ✅ 7/7 通过 | 单元测试覆盖 |
| **前端测试** | ✅ 166/166 通过 | Vitest 测试 |
| **TypeScript** | ⚠️ 存在类型错误 | 需要修复 |
| **部署** | Docker Compose + Nginx + Grafana + Loki | 生产就绪 |

### 📋 已有规划任务（TODO.md）

**高优先级（🔴 P0）**：
1. Testcontainers 集成测试
2. Refresh Token httpOnly Cookie
3. 判题回调机制

**中优先级（🟡 P1）**：
4. OpenAPI 代码生成
5. notification 表分区
6. 判题队列 Redis Stream
7. competition JSON 清理
8. CrossModuleWriteRepository 拆分

### 📚 参考项目：teaching-open

从 teaching-open 项目可以吸收的优秀功能：
- 更完善的课程管理功能
- 教室和学员管理
- 作业和考试系统
- 学习进度追踪
- 数据统计和分析

---

## Functional Requirements

### FR-1: 课程模块完整实现
- 课程 CRUD（创建、查询、更新、删除）
- 课程章节管理
- 课时内容管理
- 课程报名和学习进度
- 课程分类和标签系统
- 课程评论和评分
- 课程详情页面功能完善
- 课程列表页面筛选和搜索

### FR-2: 首页功能优化
- 优化首页加载体验
- 完善精选作品推荐
- 热门作品排行榜展示
- 推荐课程展示
- 通知公告展示
- 加载状态和空状态优化

### FR-3: 编辑器体验优化
- 优化 Scratch 编辑器加载流程
- 完善错误处理和重试机制
- 自动保存功能优化
- 编辑器状态管理改进
- 协作编辑体验优化

### FR-4: 通知中心优化
- 通知按类型分类展示
- 通知已读/未读状态管理
- 批量已读功能
- 通知删除功能
- 通知实时更新

### FR-5: 用户设置完善
- 账号安全设置
- 通知偏好设置
- 主题和外观设置
- 隐私设置
- 数据导出和删除请求

### FR-6: 判题系统优化
- 判题队列 Redis Stream 实现
- 判题回调机制（替代轮询）
- 判题状态实时更新
- 判题历史和结果展示

### FR-7: 协作编辑完善
- WebSocket 重连机制优化
- 冲突检测和解决策略
- 协作历史记录
- 协作权限控制

### FR-8: 管理后台增强
- 课程管理功能
- 学生管理增强
- 数据统计完善
- 审核流程优化

---

## Non-Functional Requirements

### NFR-1: TypeScript 类型安全
- 编译错误 0 个
- 类型覆盖率 95%+
- any 类型使用占比 <5%
- 完整的类型测试

### NFR-2: 性能要求
- API 响应时间 P99 < 500ms
- 页面加载时间 < 2s
- 首页首次加载 < 1.5s
- 支持 1000 QPS

### NFR-3: 安全性要求
- Refresh Token httpOnly + Secure + SameSite=Strict
- SQL 注入防护 100%
- XSS 防护 100%
- CSRF 防护完整
- 路径遍历防护

### NFR-4: 可靠性要求
- 系统可用性 99.9%
- 错误处理覆盖 90%+
- 告警机制完善
- 自动故障恢复

### NFR-5: 可维护性要求
- 代码审查覆盖率 100%
- 新增代码有单元测试
- 文档同步更新
- 统一的代码规范

---

## Constraints

### Technical
- **后端**：Spring Boot 3.x + Java 17+
- **前端**：Vue 3.5.x + TypeScript 6.x + Vite 8.x
- **数据库**：MySQL 8.x + Redis 7.x
- **测试**：Vitest + Playwright + Testcontainers
- **部署**：Docker Compose + Nginx

### Business
- **时间**：2026 Q2（6-8 周）
- **范围**：不包含架构级变更
- **团队**：保持现有开发模式
- **合规**：遵守数据隐私和安全法规

### Dependencies
- 依赖现有架构（不进行架构重构）
- 依赖现有数据库设计（仅增量变更）
- 依赖现有 CI/CD 流程
- 不引入新的核心技术栈

---

## Assumptions

1. **用户规模**：预期用户量在 10K-100K 范围
2. **技术栈**：现有技术栈成熟稳定，不需要替换
3. **开发资源**：保持现有的开发资源和节奏
4. **部署环境**：生产环境与测试环境配置一致
5. **第三方服务**：外部 API（如 LLM）可用性在 99%+
6. **参考项目**：teaching-open 的功能可以借鉴但不完全照搬

---

## Acceptance Criteria

### AC-1: 课程模块完整实现
- **Given**：已有课程数据模型和基础组件
- **When**：教师创建课程，学生浏览和学习
- **Then**：课程可正常发布、章节可管理、学习进度可追踪
- **Verification**：programmatic + human-judgment
- **Notes**：参考 teaching-open 的课程功能设计

### AC-2: TypeScript 零编译错误
- **Given**：前端项目代码
- **When**：运行 `npm run build:check`
- **Then**：TypeScript 编译通过，无类型错误
- **Verification**：programmatic
- **Notes**：vue-tsc 检查必须完全通过

### AC-3: Refresh Token 安全存储
- **Given**：用户已登录
- **When**：JWT Token 刷新时
- **Then**：Refresh Token 存储在 httpOnly Cookie 中，前端无法访问
- **Verification**：programmatic + human-judgment
- **Notes**：防止 XSS 窃取 Token

### AC-4: Testcontainers 集成测试
- **Given**：后端测试环境
- **When**：运行集成测试
- **Then**：Testcontainers 自动管理 Redis/MySQL，测试可在 CI 运行
- **Verification**：programmatic
- **Notes**：集成测试覆盖率 >80%

### AC-5: 判题回调机制
- **Given**：判题请求已提交
- **When**：判题完成时
- **Then**：sandbox 通过 HTTP 回调或 Redis Pub/Sub 通知后端，而非轮询
- **Verification**：programmatic
- **Notes**：减少服务器资源浪费，提升实时性

### AC-6: 首页加载体验优化
- **Given**：用户访问首页
- **When**：网络正常或网络较差时
- **Then**：页面展示合适的加载状态，失败时有优雅降级方案
- **Verification**：human-judgment
- **Notes**：Core Web Vitals LCP < 1.5s

### AC-7: 编辑器可靠性提升
- **Given**：用户打开编辑器
- **When**：编辑器加载或操作中
- **Then**：有完整的 loading 状态、超时兜底、错误处理
- **Verification**：human-judgment + programmatic
- **Notes**：参考 v3.8.1 的编辑器修复经验

### AC-8: notification 表分区
- **Given**：notification 表数据快速增长
- **When**：系统运行时间越长数据越多
- **Then**：表按时间分区或定期归档，查询性能不受影响
- **Verification**：programmatic
- **Notes**：按月分区或归档历史数据

### AC-9: 判题队列实现
- **Given**：大量判题请求涌入
- **When**：并发提交判题任务
- **Then**：使用 Redis Stream 实现判题排队，避免线程池耗尽
- **Verification**：programmatic
- **Notes**：保证系统稳定性

### AC-10: 前端错误边界完善
- **Given**：前端运行中发生错误
- **When**：组件渲染或交互出错
- **Then**：ErrorBoundary 捕获并展示友好错误页面，不影响整体应用
- **Verification**：human-judgment + programmatic

---

## Open Questions

| # | 问题 | 优先级 | 备注 |
|---|------|--------|------|
| 1 | 课程模块的定价和收费策略？ | 🟡 | 当前阶段可以免费，后续再考虑 |
| 2 | notification 分区方案选择（按月分区 vs 归档）？ | 🟡 | 需要评估数据量和查询模式 |
| 3 | OpenAPI 代码生成的具体工具选型？ | 🟡 | openapi-typescript-codegen vs 其他 |
| 4 | 是否需要完整实现 teaching-open 的所有功能？ | 🟢 | 有选择地吸收优秀设计 |
| 5 | Redis Stream 的消费组架构设计？ | 🟡 | 需要确认判题 worker 的部署模式 |

---
