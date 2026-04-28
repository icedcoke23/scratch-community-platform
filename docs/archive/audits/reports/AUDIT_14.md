# 🔍 AUDIT_14 — Sprint 11-14 二次审计

> 日期：2026-04-23
> 范围：Sprint 11-14 全部新增代码
> 审计类型：二次审计（开发后立即审计）

---

## 发现统计

| 级别 | 数量 | 说明 |
|------|------|------|
| 🔴 严重 (S) | 4 | 编译错误 / 数据竞争 / 逻辑缺陷 |
| 🟡 中等 (M) | 4 | 健壮性 / 测试覆盖 |
| 🔵 优化 (O) | 2 | 性能 / 代码风格 |
| **总计** | **10** |

---

## 严重问题 (S)

### S1. PointService.addPoints() 变量遮蔽 — 编译错误

**文件**: `PointService.java`
**问题**: `PointLog log = new PointLog()` 遮蔽了 `@Slf4j` 的 `log` 字段，`log.info(...)` 调用会编译失败
**修复**: 重命名局部变量为 `pointLog`

### S2. PointService.addPoints() 数据竞争

**文件**: `PointService.java`
**问题**: 先读再写的非原子操作，并发请求会覆盖积分
**修复**: 改为原子 SQL `UPDATE user SET points = points + ?`

### S3. CompetitionService.updateSingleRanking() lambda 修改非 final 变量 — 编译错误

**文件**: `CompetitionService.java`
**问题**: `totalScore`/`solvedCount`/`penalty` 在 lambda 中修改，Java 要求 effectively final
**修复**: 改为 `int[]` 数组或 `AtomicInteger`

### S4. CompetitionService.submitAnswer() 未实现

**文件**: `CompetitionService.java`
**问题**: 只有 log，没有实际创建 submission 或调用 JudgeService
**修复**: 调用 JudgeService.submit() 完成实际判题

---

## 中等问题 (M)

### M1. JudgeService.submit() 缺少 @Transactional

**文件**: `JudgeService.java`
**问题**: submit() 调用多个 @Transactional 方法但自身无事务，stats 更新失败会导致不一致
**修复**: 添加 @Transactional

### M2. SocialServiceTest 缺少 ApplicationEventPublisher mock

**文件**: `SocialServiceTest.java`
**问题**: SocialService 新增 eventPublisher 字段，测试未 mock，NPE 被 try-catch 吞掉
**修复**: 添加 @Mock ApplicationEventPublisher

### M3. AiReviewService.generateReview() 事务范围过大

**文件**: `AiReviewService.java`
**问题**: 冷却检查在 @Transactional 内，但只需要读操作
**修复**: 冷却检查移到事务外

### M4. CompetitionService.updateSingleRanking() N+1 查询

**文件**: `CompetitionService.java`
**问题**: 每用户每题目一条 SQL，O(users × problems)
**修复**: 改为批量查询

---

## 优化 (O)

### O1. PointService.getPointsFromUser() 冗余查询

**文件**: `PointService.java`
**问题**: 传入 User 对象但总是查 DB
**修复**: 直接用 JdbcTemplate 查，不依赖 User 对象

### O2. 前端竞赛创建缺少必填校验

**文件**: `frontend/index.html`
**问题**: 创建竞赛时未校验时间字段
**修复**: 添加前端校验

---

*下面开始修复所有问题。*
