# 📚 文档导航

> Scratch Community Platform 项目文档索引
> 最后更新：2026-04-29 | 当前版本：v3.6.1

---

## 🚀 快速入口

| 文档 | 说明 | 适合谁 |
|------|------|--------|
| [项目 README](../README.md) | 项目概览、快速启动、技术架构、API 概览 | 所有人 |
| [踩坑记录](PITFALLS.md) | **104 条坑 + 129 条经验总结** | 所有开发者 |
| [部署指南](DEPLOYMENT.md) | Docker Compose 一键部署 + 环境变量配置 | 运维/后端 |

---

## 📐 架构与设计

| 文档 | 说明 |
|------|------|
| [架构决策记录 (ADR)](ADR.md) | 6 个关键架构决策的上下文、方案对比与权衡 |
| [ER 图](ER_DIAGRAM.md) | 21 张表的完整实体关系图（Mermaid 格式） |
| [协作编辑设计](COLLABORATIVE_EDITING.md) | WebSocket 协作编辑的 CRDT/OT 架构 |
| [沙箱安全设计](SANDBOX_SECURITY.md) | 判题沙箱的进程隔离 + 资源限制 + 安全策略 |
| [Scratch 集成分析](SCRATCH_INTEGRATION_ANALYSIS.md) | Scratch VM headless 集成方案与 TurboWarp 嵌入 |
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

## 📊 分析与优化报告

| 文档 | 日期 | 说明 |
|------|------|------|
| [第三轮深度审计](THIRD_DEEP_ANALYSIS.md) | 2026-04-29 | v3.5.0 全面审计 + P0/P1 优化：TraceId/熔断器/线程池监控/幂等性/ErrorCode |
| [综合分析报告](COMPREHENSIVE_ANALYSIS.md) | 2026-04-25 | 全栈深度分析：架构 / CI / 代码 / 安全 / 数据库 / API / 沙箱 |
| [全面优化报告](FULL_OPTIMIZATION_REPORT.md) | 2026-04-28 | v3.4.0 优化分析：后端/前端/数据库/安全/测试/文档 |
| [二次审计报告](SECOND_OPTIMIZATION_AUDIT.md) | 2026-04-28 | v3.4.0 优化后审计：12 项修改清单 + 风险评估 + 兼容性检查 |

---

## 📂 归档文档

| 目录 | 说明 |
|------|------|
| [archive/audits/](archive/audits/) | 历次审计报告（6 轮审计，28 份报告）— 已归档，仅作历史参考 |
| [archive/planning/](archive/planning/) | 前期设计文档：需求 / 架构 / 技术选型 / 竞品分析 / 头脑风暴 |
| [archive/legacy/](archive/legacy/) | 过时文件（如 sprint6_tables.sql） |

> 归档文档保留作为历史参考，当前架构以 `ADR.md` 和 `COMPREHENSIVE_ANALYSIS.md` 为准。

---

## 🗂️ 文档维护规则

1. **每个主题只保留最新版本**，旧版本移入 `archive/`
2. **新增文档必须更新此索引**
3. **踩坑记录持续更新** — 每次遇到新坑都记录到 `PITFALLS.md`
4. **Sprint 文档不单独保留** — 重要结论汇入综合文档，Sprint 细节归档
5. **审计报告归档** — 新审计完成后更新 `archive/audits/README.md` 总结
