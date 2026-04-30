# 🔍 Sprint 6 教室模块二次审计报告

> 审计时间：2026-04-23 18:53
> 审计范围：Sprint 6 classroom 模块全部 13 个文件

---

## 发现并修复的问题

### 🔴 严重（3 个）

#### S1. `grade()` 重复批改导致 `graded_count` 虚增
- **问题**: 同一提交被重新批改时，`graded_count` 再次 +1，但实际批改人数没变
- **修复**: 判断 `isRegrade = "graded".equals(submission.getStatus())`，仅首次批改时递增

#### S2. `toVOPage()` N+1 查询
- **问题**: 每个作业单独查 class 表获取 className，20 个作业 = 20 次查询
- **修复**: 改为批量查询 `SELECT id, name FROM class WHERE id IN (...)`，一次拿到所有班级名称

#### S3. Entity `JacksonTypeHandler` 在 String 类型上双重序列化
- **问题**: `Homework.problemIds` 和 `HomeworkSubmission.answers` 是 String 类型但标注了 `JacksonTypeHandler`，会导致双重序列化
- **修复**: 移除 `@TableField(typeHandler = JacksonTypeHandler.class)`，JSON 字符串直接存储

### 🟡 中等（2 个）

#### M1. `submit()` 未校验项目归属
- **问题**: 学生提交 Scratch 项目时，`projectId` 未验证是否属于该学生，可能提交他人项目
- **修复**: 添加 `SELECT COUNT(*) FROM project WHERE id = ? AND user_id = ?` 校验

#### M2. `mySubmissions()` offset 无负数保护
- **修复**: 添加 `if (offset < 0) offset = 0`

---

## 审计结论

| 级别 | 发现 | 修复 |
|------|------|------|
| 严重 | 3 | 3 |
| 中等 | 2 | 2 |

**Sprint 6 二次审计通过。**
