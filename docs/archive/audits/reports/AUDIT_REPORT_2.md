# 🔍 开发文档二次审计报告

> 审计时间：2026-04-23 17:25
> 审计范围：DEV_PLAN.md + CODING_STANDARDS.md + MODULE_DEV_GUIDE.md + QA_CHECKLIST.md
> 审计依据：与实际代码库交叉比对（backend/*.java, pom.xml, init.sql）

---

## 严重问题 (5 个)

### S1. MODULE_DEV_GUIDE Entity 示例使用 @Data，与 CODING_STANDARDS 自相矛盾
- **位置**: `MODULE_DEV_GUIDE.md` Step 2
- **问题**: Entity 示例标注 `@Data`，但 CODING_STANDARDS 1.3 节明确说 "Entity 不做业务逻辑"，QA_CHECKLIST 说 "Entity 避免 @Data 用 @Getter/@Setter"。实际代码中 LoginUser 已修复为 @Getter/@Setter 并注释说明原因。
- **影响**: 开发者照搬示例会违反规范，审计时反复被标记

### S2. FileUploadUtils 无文件大小校验，安全规范形同虚设
- **位置**: `CODING_STANDARDS.md` 6.1 节 + 实际代码 `FileUploadUtils.java`
- **问题**: 规范写 "文件上传校验类型 + 大小限制（50MB）"，但实际 FileUploadUtils.upload() 没有任何大小/类型校验。init.sql 配置 `upload.max_size=52428800`（50MB）也未在代码中使用。
- **影响**: 恶意用户可上传超大文件耗尽磁盘/内存

### S3. MODULE_DEV_GUIDE 示例代码中多处方法在 Service 中不存在
- **位置**: `MODULE_DEV_GUIDE.md` Step 7 ProjectController
- **问题**: Controller 引用了 `myProjects()`、`getDetail()`、`update()`、`delete()`、`publish()`、`getSb3Url()` 共 6 个方法，但 Step 6 的 ProjectService 示例中只实现了 `create()` 和 `uploadSb3()`。
- **影响**: 开发者照搬后编译失败

### S4. JSON 库混用：scratch-sb3 用 fastjson2，示例代码用 JSON.toJSONString
- **位置**: `MODULE_DEV_GUIDE.md` Step 6 + 实际代码 `scratch-sb3/pom.xml`
- **问题**: scratch-sb3 模块依赖 fastjson2，但 scratch-common 用 Jackson。示例代码中 `JSON.toJSONString(parseResult)` 没标注 import 路径，开发者会混淆用哪个库。
- **影响**: 编译错误或运行时 JSON 序列化行为不一致

### S5. ProjectDetailVO 继承 ProjectVO 时 @Data 的 Lombok 陷阱
- **位置**: `MODULE_DEV_GUIDE.md` Step 5
- **问题**: `ProjectDetailVO extends ProjectVO` 且两者都标 `@Data`，Lombok 生成的 equals/hashCode 会因父类/子类字段不一致导致对称性违反（`a.equals(b) != b.equals(a)`）。
- **影响**: 用 VO 做集合操作（Set/Map）时出现诡异 Bug

---

## 中等问题 (8 个)

### M1. DEV_PLAN sb3-parser 解压描述与接口签名矛盾
- **位置**: `DEV_PLAN.md` 3.1.1 验收标准
- **问题**: 写 "能解压 sb3 为临时目录"，但 SB3Parser 接口签名是 `parse(byte[] sb3Bytes)`，应该是内存解压而非写磁盘

### M2. DEV_PLAN Sprint 4/5 缺少预估工时
- **位置**: `DEV_PLAN.md` 第四、五节
- **问题**: Sprint 3 每个任务有预估工时（总计 ~27h），Sprint 4/5 只列优先级无工时，无法做 Sprint 规划

### M3. CODING_STANDARDS @Data 问题描述不精确
- **位置**: `CODING_STANDARDS.md` 1.2 命名规范 + QA_CHECKLIST
- **问题**: 写 "Entity 避免 @Data 的 ThreadLocal 问题"，实际 ThreadLocal 是 static 字段，Lombok @Data 不会为 static 字段生成 equals/hashCode。真正问题是 @Data 的 toString() 会暴露所有字段值（包括密码等），且 equals/hashCode 在继承场景有对称性问题。

### M4. CODING_STANDARDS 安全规范缺少文件校验实现指导
- **位置**: `CODING_STANDARDS.md` 6.1 节
- **问题**: 只写 "校验文件类型 + 大小限制"，没有给出实现方式（应该在 Controller 层用 MultipartFile.getSize() 校验，还是在 Service 层，还是用全局 Filter）

### M5. DEV_PLAN 跨 Sprint 持续任务与 Sprint 3 计划冲突
- **位置**: `DEV_PLAN.md` 第六节 vs 第三节
- **问题**: 说 "每 Sprint 预留 20% 时间清理技术债"，但 Sprint 3 的 7 天计划全部排满开发任务，没有留缓冲

### M6. MODULE_DEV_GUIDE 模块依赖图与实际 pom.xml 不完全一致
- **位置**: `MODULE_DEV_GUIDE.md` 第二节
- **问题**: 依赖图写 `scratch-classroom ← 依赖 user, judge`，但 classroom 的 pom.xml 实际只依赖 common。模块间通过 userId/judgeId 关联，不直接依赖。

### M7. CODING_STANDARDS 分页排序参数写入规范但代码未实现
- **位置**: `CODING_STANDARDS.md` 4.2 节
- **问题**: 定义了 `sort` 和 `order` 参数，但当前所有 Controller 都没有实现排序参数。规范应标注 "规划中" 或 "推荐"，避免开发者以为是现有标准。

### M8. QA_CHECKLIST 缺少 JSON 字段处理检查
- **位置**: `QA_CHECKLIST.md` 第四节
- **问题**: Entity 中 JSON 字段需要 `@TableField(typeHandler = JacksonTypeHandler.class)` + `@TableName(autoResultMap = true)`，但检查清单没有这一项。遗漏容易导致 MyBatis-Plus 查询 JSON 字段返回 null。

---

## 优化建议 (3 个)

### O1. 建议在 MODULE_DEV_GUIDE 中补充 "完整 Service 实现"
当前 Step 6 只展示了 create 和 uploadSb3 两个方法，建议补充 list/detail/update/delete/publish 的完整实现，形成可复制的模板。

### O2. 建议 DEV_PLAN 增加 Sprint 燃尽图/进度追踪格式
当前 DoD 是 checklist 形式，建议增加简单的进度追踪表，方便看板式管理。

### O3. 建议 CODING_STANDARDS 补充 MinIO 文件校验的标准实现
给出一个标准的文件校验工具方法，包含类型白名单 + 大小限制 + 文件头魔数校验。

---

## 审计结论

| 级别 | 数量 | 状态 |
|------|------|------|
| 严重 | 5 | 全部修复 ↓ |
| 中等 | 8 | 全部修复 ↓ |
| 优化 | 3 | 已采纳 |
