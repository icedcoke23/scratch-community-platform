# 🔍 深度优化 + Sprint 4 开发审计报告

> 审计时间：2026-04-23 18:20
> 审计范围：深度优化 (Sprint 2 遗留修复 + 安全加固) + Sprint 4 judge 模块新开发代码
> 审计方式：代码静态分析 + API 链路验证 + 规范一致性检查

---

## 一、深度优化审计

### 1.1 Sprint 2 遗留 TODO 修复 ✅

| 问题 | 修复方案 | 状态 |
|------|---------|------|
| `getFollowers/getFollowing` 缺失 | 新增 `UserFollowMapper.selectFollowing/selectFollowers` 自定义 SQL JOIN | ✅ 已修复 |
| `ClassService.joinClass` 缺 class_student 插入 | 实现完整的班级查找 + 重复检查 + 插入逻辑 | ✅ 已修复 |
| `ClassService.getMyClasses` 缺失 | 实现教师创建 + 学生加入合并查询 | ✅ 已修复 |
| `FileUploadUtils` 无大小校验 | 新增 `upload(file, business, maxSize, allowedTypes)` 通用方法 + `uploadAvatar`/`uploadSb3` 专用方法 | ✅ 已修复 |

### 1.2 安全加固审计

| 检查项 | 结果 | 说明 |
|--------|------|------|
| FileUploadUtils 文件大小校验 | ✅ | 默认 50MB，sb3 100MB，avatar 5MB |
| FileUploadUtils 类型白名单 | ✅ | 图片: jpeg/png/gif/webp，sb3: zip/octet-stream |
| FileUploadUtils 空文件检查 | ✅ | `file.isEmpty()` 校验 |
| UserService.follow 自己关注自己 | ✅ | 抛出 PARAM_ERROR |
| ClassService.removeMember 权限检查 | ✅ | 校验 teacherId 是否为班级创建者 |

---

## 二、Sprint 4 新代码审计

### 2.1 Entity 审计 ✅

| 检查项 | Problem | Submission |
|--------|---------|------------|
| 使用 @Getter/@Setter（非 @Data） | ✅ | ✅ |
| @TableName(autoResultMap=true) | ✅ | ✅ |
| JSON 字段 JacksonTypeHandler | ✅ (options, expectedOutput) | ✅ (judgeDetail) |
| @TableLogic 逻辑删除 | ✅ | ✅ |
| @TableField(fill) 自动填充 | ✅ | ✅ |

### 2.2 Service 审计

#### ProblemService ✅
- 6 个方法: create / listProblems / getDetail / update / publish / delete
- 权限校验: `getAndCheckOwner` 检查 creatorId
- 分页查询: 支持 type + difficulty 筛选
- 选项序列化: 使用 Jackson ObjectMapper（非 fastjson2）

#### JudgeService ✅
- 即时判题: 选择题/判断题直接比对答案（忽略大小写）
- 异步判题: 调用 sandbox REST API
- 题目统计: submitCount / acceptCount 原子更新
- 错误处理: PENDING → AC/WA/TLE/RE 完整状态机

### 2.3 Controller 审计 ✅

| API | 方法 | 路径 | 权限 |
|-----|------|------|------|
| 创建题目 | POST | /api/problem | @RequireRole(TEACHER, ADMIN) |
| 题目列表 | GET | /api/problem | 公开 |
| 题目详情 | GET | /api/problem/{id} | 公开 |
| 更新题目 | PUT | /api/problem/{id} | @RequireRole(TEACHER, ADMIN) |
| 发布题目 | POST | /api/problem/{id}/publish | @RequireRole(TEACHER, ADMIN) |
| 删除题目 | DELETE | /api/problem/{id} | @RequireRole(TEACHER, ADMIN) |
| 提交答案 | POST | /api/judge/submit | 登录用户 |
| 我的提交 | GET | /api/judge/submissions | 登录用户 |
| 判题结果 | GET | /api/judge/result/{id} | 登录用户（仅自己） |

### 2.4 代码规范一致性 ✅

| 检查项 | 结果 |
|--------|------|
| VO 不继承（避免 Lombok 陷阱） | ✅ ProblemDetailVO 独立定义 |
| JSON 库使用 Jackson（非 fastjson2） | ✅ JudgeService 使用 ObjectMapper |
| ErrorCode 使用已有常量 | ✅ PROBLEM_NOT_FOUND, USER_NO_PERMISSION 等 |
| 分页处理一致性 | ✅ 与 user/editor 模块相同模式 |
| 事务注解 | ✅ 写操作 @Transactional，读操作 @Transactional(readOnly=true) |

---

## 三、文档一致性审计

| 检查项 | 结果 |
|--------|------|
| TODO.md 与 PROGRESS.md 同步 | ✅ 已更新到 Sprint 4 |
| DEV_PLAN.md Sprint 4/5 工时 | ✅ 已补充（Sprint 4 ~25h，Sprint 5 ~16h） |
| DEV_PLAN.md 燃尽表 | ✅ Sprint 4 燃尽表已填写 |
| PROGRESS.md 遗留 TODO 标记 | ✅ Sprint 2 遗留项标记为已修复 |
| PITFALLS.md 踩坑记录 | ✅ 10 个坑完整记录 |

---

## 四、已知待办项

| 项目 | 优先级 | 说明 |
|------|--------|------|
| sandbox judge-worker.js 完善 | P0 | Sprint 4 Day 4 |
| sandbox Redis 队列 | P0 | Sprint 4 Day 5 |
| 联调验证 | P0 | Sprint 4 Day 6 |
| Swagger/SpringDoc 接口文档 | P1 | 下个 Sprint |
| 单元测试 | P1 | 下个 Sprint |

---

## 五、审计结论

| 级别 | 数量 | 状态 |
|------|------|------|
| 严重 | 0 | — |
| 中等 | 0 | — |
| 优化 | 2 | 待办（Swagger + 单测） |

**深度优化 + Sprint 4 开发代码审计通过。** 所有 Sprint 2 遗留技术债已清零，安全加固已落地，judge 模块代码符合编码规范。
