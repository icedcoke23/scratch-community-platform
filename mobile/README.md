# 📱 移动端（uni-app）

> 状态：🟡 骨架阶段 | 最后更新：2026-04-28

---

## 概述

基于 uni-app 的移动端适配方案，目前包含 4 个核心页面的骨架代码。

## 页面列表

| 页面 | 文件 | 说明 | 状态 |
|------|------|------|------|
| 首页 | `pages/index/index.vue` | 社区 Feed 流 | 🟡 骨架 |
| 项目详情 | `pages/project/detail.vue` | 项目查看 + 点赞/评论 | 🟡 骨架 |
| 排行榜 | `pages/rank/rank.vue` | 积分排行榜 | 🟡 骨架 |
| 用户主页 | `pages/user/profile.vue` | 用户信息 + 项目列表 | 🟡 骨架 |

## 待完成

- [ ] 添加 `package.json` 和构建配置
- [ ] 对接后端 API（复用 `frontend-vue/src/api/` 类型定义）
- [ ] 补充剩余页面（登录/注册/编辑器/作业/竞赛等）
- [ ] 条件编译适配微信小程序 / H5 / App
- [ ] 提交到 uni-app 插件市场或独立构建

## 相关文档

- [移动端计划](../docs/MOBILE_APP_PLAN.md) — 详细的移动端开发规划
