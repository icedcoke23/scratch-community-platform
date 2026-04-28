# 🔍 AUDIT_16 — CompetitionService N+1 查询优化审计

> 审计时间：2026-04-24 00:30
> 审计范围：CompetitionService.java 排名计算逻辑
> 审计类型：专项审计（性能优化）

---

## 一、问题回顾

**原问题 (AUDIT_14 M4)**：`updateSingleRanking()` 在循环中逐用户逐题目查询 submission 表，导致 O(U × P) 的 N+1 查询。

**原始查询次数**（50 人 × 5 题）：352 次

---

## 二、优化方案

采用**双策略**组合：

| 场景 | 策略 | 方法 | 查询次数 |
|------|------|------|---------|
| 实时提交 | 增量更新 | `submitAnswer()` → `recalculateUserRanking()` + `reorderRankings()` | P + 5 = **10** |
| 全量重算 | 批量查询 | `updateRankings()` → 单次查询 + 内存计算 | 2U + 4 = **104** |

### 核心改动

1. **`submitAnswer()`**：不再调用 `updateRankings()`，改为：
   - `recalculateUserRanking()` — 只查当前用户的 P 道题提交
   - `reorderRankings()` — 全局排序（只排顺序，不重算分数）

2. **`updateRankings()`**：替换为批量查询版：
   - 1 次查询获取所有用户所有题目的提交记录
   - 内存中按 userId → problemId 分组
   - `calculateUserRanking()` 纯内存计算

3. **新增 `RankingCalcResult`**：内部数据类，消除重复计算代码

4. **新增工具方法**：`placeholders()`、`concatParams()` 支持 IN 查询

---

## 三、代码审查

### 3.1 正确性验证

| 检查项 | 结果 |
|--------|------|
| 批量查询 SQL 参数化（防注入） | ✅ 使用 `?` 占位符 + JdbcTemplate 参数绑定 |
| IN 查询空列表保护 | ✅ registrations 为空时提前 return |
| 分数计算逻辑与原始一致 | ✅ 同样的 totalScore/solvedCount/penalty 算法 |
| 排序规则与原始一致 | ✅ totalScore DESC, penalty ASC |
| 排名记录 upsert 逻辑不变 | ✅ 存在则更新，不存在则插入 |
| 增量更新正确性 | ✅ 只重算当前用户，全局排序保证排名正确 |
| 未使用的 import 清理 | ✅ 移除了 Submission entity import |

### 3.2 边界条件

| 场景 | 处理 |
|------|------|
| 竞赛无参赛者 | `registrations.isEmpty()` 提前返回 ✅ |
| 用户无某题提交 | `getOrDefault(pid, emptyList())` 返回空列表 ✅ |
| problemScores 长度不足 | 使用 `i < problemScores.size()` 判断，默认 100 ✅ |
| 批量查询无结果 | `userProblemSubmissions` 为空 map，计算出 0 分 ✅ |

### 3.3 安全性

| 检查项 | 结果 |
|--------|------|
| SQL 注入防护 | ✅ 全部使用 `?` 参数化 |
| 事务一致性 | ✅ 方法级 @Transactional |
| 日志记录 | ✅ submitAnswer 保留 log.info |

---

## 四、性能对比

### 查询次数对比

| 场景 (U=用户数, P=题目数) | 优化前 | submitAnswer 增量 | updateRankings 批量 |
|-------------------------|--------|-------------------|-------------------|
| 小型 (10×3) | 62 | 8 | 24 |
| 中型 (50×5) | **352** | **10** | **104** |
| 大型 (200×10) | **2,402** | **15** | **404** |

### 响应时间估算 (50 人 × 5 题)

| 操作 | 优化前 | 优化后 |
|------|--------|--------|
| submitAnswer | ~350ms | ~15ms |
| 10 人同时提交 | 3,520 查询 | 100 查询 |

---

## 五、审计结论

| 级别 | 数量 | 说明 |
|------|------|------|
| 严重 | 0 | — |
| 中等 | 0 | — |
| 优化 | 0 | — |

**结论**：N+1 查询问题已通过双策略方案彻底解决。代码正确性、边界条件、安全性均已验证。AUDIT_14 M4 关闭。
