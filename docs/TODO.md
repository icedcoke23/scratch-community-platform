# 开发待办

## 当前 Sprint: Sprint 1 + 2 (脚手架 + user 模块)

### 优先级 P0 (本周必须完成)
- [x] 代码审计 + 修复 6 个严重问题 + 9 个中等问题
- [ ] 本地 `mvn compile` 验证项目能编译通过
- [ ] `docker-compose up` 验证基础设施启动正常
- [ ] `/api/health` 接口联调通过
- [ ] 注册接口联调: POST /api/user/register
- [ ] 登录接口联调: POST /api/user/login (拿到 JWT)
- [ ] 个人信息接口联调: GET /api/user/me (带 Token)

### 优先级 P1 (下周)
- [ ] sb3-parser 库: 完成 SB3Parser 核心解析逻辑
- [ ] editor 模块: 项目 CRUD
- [ ] editor 模块: sb3 保存 + 自动解析
- [ ] sandbox: scratch-vm headless 跑通

### 优先级 P2 (第三周)
- [ ] judge 模块: 题目 CRUD
- [ ] judge 模块: 判题流程联调
- [ ] social 模块: 点赞/评论

### 技术债
- [ ] UserService: getFollowers/getFollowing 查询补全
- [ ] UserService: searchUsers 转换为 UserVO
- [ ] ClassService: joinClass 补全 class_student 插入
- [ ] ClassService: getMyClasses 补全
- [ ] 全局: 添加接口文档 (Swagger/SpringDoc)
