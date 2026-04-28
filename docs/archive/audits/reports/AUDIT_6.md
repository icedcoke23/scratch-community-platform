# 🔍 Sprint 5 社区模块二次审计报告

> 审计时间：2026-04-23 18:45
> 审计范围：Sprint 5 social 模块全部 14 个文件

---

## 发现并修复的问题

### 🔴 严重（3 个）

#### S1. SocialService.like() — 点赞数更新代码完全无效
- **问题**: 原代码有 `projectLikeMapper.update(null, ...)` 和未使用的 `SqlSession session = null`，实际上从未更新 project 表的 like_count
- **修复**: 改用 `jdbcTemplate.update("UPDATE project SET like_count = like_count + 1")` 原子递增；unlike 同理用 `GREATEST(like_count - 1, 0)` 防止负数

#### S2. SocialController.like()/unlike() — 幂等性导致排行榜分数虚增
- **问题**: 即使 `socialService.like()` 幂等返回（已点赞），Controller 仍调用 `rankService.incrementLikeScore(userId, 1)`，导致排行榜分数只增不减
- **修复**: `like()` 改为返回 `boolean`（true=新点赞），Controller 仅在 `created=true` 时更新排行榜；`unlike()` 同理

#### S3. FeedService.getFeed() — N+1 查询问题
- **问题**: 每个项目单独查询 `countByUserAndProject`，20 个项目 = 20 次额外查询
- **修复**: 改为批量查询 `SELECT project_id FROM project_like WHERE user_id = ? AND project_id IN (...)`，一次拿到所有已点赞的 projectId，然后在内存中标记

### 🟡 中等（4 个）

#### M1. SocialService.addComment() — 缺少项目存在性校验
- **修复**: 添加 `projectExists()` 检查

#### M2. FeedService.getFeed() — SQL 拼接中 page/size 无边界保护
- **修复**: offset 计算加 `if (offset < 0) offset = 0` 保护

#### M3. SocialController.feed() — 未登录时调用 LoginUser.getUserId() 会抛异常
- **修复**: 用 try-catch 包裹，未登录时 userId=null，Feed 正常返回（isLiked=false）

#### M4. Controller 分页参数无校验
- **修复**: 所有分页参数添加 `@Min(1)` `@Max(100)` 注解

### 🟢 优化（2 个）

#### O1. FeedService — JOIN 查询缺少 user.deleted 条件
- **修复**: `JOIN user u ON p.user_id = u.id AND u.deleted = 0`

#### O2. SocialService — getCommentById 未查询用户信息
- **修复**: 用 JdbcTemplate 查询 username/nickname/avatar_url

---

## 修复文件清单

| 文件 | 修复 |
|------|------|
| `SocialService.java` | 项目存在性校验 + 点赞/评论数原子更新 + 返回 boolean + 评论用户信息查询 |
| `FeedService.java` | 批量查询点赞状态（解决 N+1）+ offset 保护 + JOIN 条件补全 |
| `SocialController.java` | like/unlike 返回值判断 + feed 未登录处理 + 分页参数校验 |

---

## 审计结论

| 级别 | 发现 | 修复 |
|------|------|------|
| 严重 | 3 | 3 |
| 中等 | 4 | 4 |
| 优化 | 2 | 2 |

**Sprint 5 二次审计通过。**
