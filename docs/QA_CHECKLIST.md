# ✅ 质量检查清单

> 版本：v2.0 | 日期：2026-04-25
> 每个 Sprint 结束 + 每个 PR 提交前的自检清单
> v1.1: 二次审计修复 — 补充 Lombok/JSON 字段检查、安全检查项

---

## 一、PR 提交前自检

### 代码质量
- [ ] `mvn clean compile` 编译通过，零 Warning
- [ ] 无 `System.out.println` / `System.err.println`
- [ ] 无 `e.printStackTrace()`
- [ ] 无注释掉的代码块（除非有明确 TODO）
- [ ] 无硬编码的魔法数字（用常量替代）
- [ ] 无未使用的 import

### Lombok 使用
- [ ] DTO/VO 使用 `@Data`
- [ ] Entity 使用 `@Getter` `@Setter`（不用 `@Data`，避免 toString 暴露敏感字段）
- [ ] 含 ThreadLocal 的类使用 `@Getter` `@Setter` `@ToString`（参考 LoginUser）
- [ ] 详情 VO 不继承列表 VO（避免 equals/hashCode 对称性问题）

### MyBatis-Plus
- [ ] Entity 有 JSON 字段时：`@TableField(typeHandler = JacksonTypeHandler.class)`
- [ ] Entity 有 JSON 字段时：`@TableName(autoResultMap = true)`
- [ ] 主键标注 `@TableId(type = IdType.AUTO)`
- [ ] 逻辑删除字段标注 `@TableLogic`
- [ ] 时间字段标注 `@TableField(fill = FieldFill.INSERT)` / `INSERT_UPDATE`

### 安全
- [ ] 无硬编码密码/密钥/Token
- [ ] 无拼接 SQL（用参数化查询）
- [ ] 文件上传在 Controller 层校验了类型和大小
- [ ] 用户输入经过敏感词过滤
- [ ] 角色校验用了 `@RequireRole` 注解
- [ ] 软删除，无物理删除业务数据

### 接口
- [ ] 所有接口返回 `R<T>`
- [ ] POST/PUT 接口有 `@Valid` 参数校验
- [ ] 分页参数统一（page/size）
- [ ] 错误码使用 `ErrorCode` 枚举或在分配范围内
- [ ] 无接口返回 `password` 等敏感字段

### 数据库
- [ ] 新表有 `id`, `created_at`, `updated_at` 字段
- [ ] 需要软删除的表有 `deleted` 字段
- [ ] 索引命名规范（uk_/idx_/ft_）
- [ ] 字符集 utf8mb4
- [ ] init.sql 与 Entity 字段一致

---

## 二、Sprint 结束审计

### 功能完整性
- [ ] Sprint 目标中的所有 P0 任务已完成
- [ ] 所有 API 已联调通过
- [ ] 核心逻辑有单元测试
- [ ] 异常场景有处理（空值、越权、重复操作）

### 架构合规
- [ ] 新增代码遵循包结构规范
- [ ] 模块间无循环依赖
- [ ] 无跨模块直接调用 Mapper
- [ ] ErrorCode 在分配范围内
- [ ] JSON 库使用与所在模块一致（sb3 用 fastjson2，其他用 Jackson）

### 文档同步
- [ ] PROGRESS.md 已勾选完成项
- [ ] TODO.md 已更新下一 Sprint 任务
- [ ] README.md Roadmap 状态已更新
- [ ] 新增/修改的接口有文档说明

### 基础设施
- [ ] `docker-compose up` 正常启动
- [ ] init.sql 与 Entity 字段一致
- [ ] 无遗漏的数据库表/字段变更

---

## 三、代码审计模板

每个 Sprint 结束时，按以下模板输出审计报告，追加到 `docs/AUDIT_REPORT.md`。

```markdown
## Sprint N 审计报告

> 审计时间：YYYY-MM-DD
> 审计范围：xxx 模块 + xxx 模块 (N 个文件)

### 严重问题 (X 个)

#### S1. [问题标题]
- **位置**: `文件路径`
- **问题**: [描述]
- **修复**: [方案]

### 中等问题 (X 个)

#### M1. [问题标题]
- **位置**: `文件路径`
- **问题**: [描述]
- **修复**: [方案]

### 优化建议 (X 个)

#### O1. [建议标题]
- **描述**: [建议内容]

### 审计结论
- 严重问题：X 个（已修复 / 待修复）
- 中等问题：X 个（已修复 / 待修复）
- 优化建议：X 个
```

---

## 四、Sprint 完成标准（Definition of Done）

一个 Sprint 只有满足以下 **全部条件** 才算完成：

| # | 条件 | 验证方式 |
|---|------|---------|
| 1 | 所有 P0 任务代码完成 | PROGRESS.md 全部 [x] |
| 2 | 编译通过 | `mvn clean compile` 零 Warning |
| 3 | 打包成功 | `mvn clean package -DskipTests` |
| 4 | 核心接口联调通过 | curl/Postman 验证 |
| 5 | 单测通过 | `mvn test` |
| 6 | 代码审计完成 | 审计报告已追加 |
| 7 | 文档同步 | PROGRESS/TODO/README 已更新 |
| 8 | 无严重问题遗留 | 审计报告中 S 级全部修复 |

---

## 五、常见问题检查

### 编译失败
```bash
# 检查依赖
mvn dependency:tree | grep "Could not find"

# 检查 Lombok
# 确保 IDE 安装了 Lombok 插件
# 确保 pom.xml 有 lombok 依赖 + annotationProcessorPaths
```

### 接口 401
```bash
# 检查 Token 是否过期
# 检查 AuthInterceptor 是否拦截了该路径
# 检查 application.yml 中的白名单路径
```

### 数据库字段不匹配
```bash
# 对比 init.sql 和 Entity 类
# 检查驼峰/下划线转换
# 检查 @TableField 注解
```

### JSON 字段查询返回 null
```bash
# 检查 @TableName(autoResultMap = true) 是否加了
# 检查 @TableField(typeHandler = JacksonTypeHandler.class) 是否加了
# 检查是否在 Mapper XML 中手动指定了 typeHandler
```

---

*质量是习惯，不是检查。每次写完代码先过一遍这个清单，再提交。*
