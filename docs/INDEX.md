# 📚 文档导航

> Scratch Community Platform 项目文档索引
> 最后更新：2026-05-01 | 当前版本：v3.8.1

---

## 🚀 快速入口

| 文档 | 说明 | 适合谁 |
|------|------|--------|
| [项目 README](../README.md) | 项目概览、快速启动、技术架构、API 概览 | 所有人 |
| [踩坑记录](PITFALLS.md) | 104 条坑 + 129 条经验总结 | 所有开发者 |
| [部署指南](DEPLOYMENT.md) | Docker Compose 一键部署 + 环境变量配置 | 运维/后端 |

---

## 📐 架构与设计

| 文档 | 说明 |
|------|------|
| [架构决策记录 (ADR)](ADR.md) | 关键架构决策的上下文、方案对比与权衡 |
| [ER 图](ER_DIAGRAM.md) | 23 张表的完整实体关系图（Mermaid 格式） |
| [沙箱安全设计](SANDBOX_SECURITY.md) | 判题沙箱的进程隔离 + 资源限制 + 安全策略 |
| [协作编辑设计](COLLABORATIVE_EDITING.md) | WebSocket 协作编辑的 CRDT/OT 架构 |
| [移动端计划](MOBILE_APP_PLAN.md) | uni-app 移动端规划（iOS/Android） |

---

## 📋 开发规范

| 文档 | 说明 |
|------|------|
| [编码规范](CODING_STANDARDS.md) | Java / Node.js / TypeScript / 数据库 / API / Git / 安全规范 |
| [模块开发指南](MODULE_DEV_GUIDE.md) | 从 0 到 1 的标准开发流程（含代码模板） |
| [质量检查清单](QA_CHECKLIST.md) | PR 自检 + Sprint 审计模板 |
| [开发计划](DEV_PLAN.md) | Phase 规划 + Sprint 任务 + 里程碑 + 风险管理 |

---

## 🔧 运维与部署

| 文档 | 说明 |
|------|------|
| [部署指南](DEPLOYMENT.md) | Docker Compose 部署 + 手动部署 |
| [生产部署](PRODUCTION_DEPLOYMENT.md) | 生产环境部署检查清单 |
| [服务器部署](SERVER_DEPLOYMENT.md) | 服务器环境配置 |
| [部署进度](DEPLOYMENT_PROGRESS.md) | 部署状态跟踪 |

---

## 📂 归档文档

| 目录 | 说明 |
|------|------|
| [archive/audits/](archive/audits/) | 历次审计报告（8 份） — 已归档，仅作历史参考 |
| [archive/planning/](archive/planning/) | 前期设计文档：需求 / 架构 / 技术选型 / 竞品分析 |
| [archive/legacy/](archive/legacy/) | 过时文件 |

> 归档文档保留作为历史参考，当前架构以 `ADR.md` 和 README 为准。

---

## 🗂️ 文档维护规则

1. **每个主题只保留最新版本**，旧版本移入 `archive/`
2. **新增文档必须更新此索引**
3. **踩坑记录持续更新** — 每次遇到新坑都记录到 `PITFALLS.md`
4. **审计报告归档** — 新审计完成后移入 `archive/audits/`
