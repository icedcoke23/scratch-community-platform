# Scratch 社区平台 - Verification Checklist

## 后端基础设施验证
- [ ] 后端项目结构完整，包含 package.json、tsconfig.json、src/ 目录
- [ ] 后端服务可以正常启动在 8080 端口
- [ ] 健康检查端点返回 200 OK
- [ ] 基本路由配置正确

## 项目管理 API 验证
- [ ] POST /api/v1/project 可以创建新项目
- [ ] GET /api/v1/project/:id 可以获取项目详情
- [ ] PUT /api/v1/project/:id 可以更新项目
- [ ] POST /api/v1/project/:id/publish 可以发布项目
- [ ] POST /api/v1/project/:id/sb3/upload 可以上传 SB3 文件
- [ ] GET /api/v1/project/:id/sb3/download 可以下载 SB3 文件
- [ ] CORS 配置正确，前端可正常访问

## TurboWarp 集成验证
- [ ] wrapper HTML 文件存在（editor.html、player.html）
- [ ] iframe 发送 iframe-ready 消息
- [ ] project_url 参数正确传递
- [ ] TurboWarp 编辑器正常加载
- [ ] TurboWarp 播放器正常工作

## 前端编辑器验证
- [ ] 创建新项目流程正常
- [ ] 加载已有项目正常
- [ ] 项目标题可编辑
- [ ] 保存按钮功能正常
- [ ] 发布按钮功能正常
- [ ] 错误处理完善

## 预览组件验证
- [ ] 预览组件渲染正常
- [ ] 点击加载预览功能正常
- [ ] 全屏功能正常
- [ ] 错误处理完善

## 项目列表验证
- [ ] 项目列表页面存在
- [ ] 路由 /projects 正常工作
- [ ] 项目列表 API 正常返回数据
- [ ] 列表展示美观，交互正常

## 代码质量验证
- [ ] TypeScript 编译无错误
- [ ] 类型定义完整
- [ ] 错误处理完善
- [ ] 代码结构清晰

## 端到端测试验证
- [ ] 完整创建项目流程通过
- [ ] 编辑和保存项目流程通过
- [ ] 上传和下载 SB3 文件通过
- [ ] 预览项目流程通过
- [ ] 发布项目流程通过

## 文档验证
- [ ] README.md 更新完整
- [ ] 快速开始指南可操作
- [ ] 集成文档更新及时
