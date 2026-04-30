# 📐 编码规范

> 版本：v2.0 | 日期：2026-04-25
> 适用范围：backend (Java) + sandbox (Node.js)
> v1.1: 二次审计修复 — 修正 @Data 描述、补充文件校验实现、标注未实现规范

---

## 一、Java 后端规范

### 1.1 包结构

```
com.scratch.community
├── common/                          # 公共基础（不依赖任何 module）
│   ├── config/                      # 全局配置
│   ├── exception/                   # 异常定义 + 全局处理
│   ├── result/                      # R<T> + ErrorCode + PageResult
│   ├── auth/                        # JWT + 拦截器 + 注解
│   ├── util/                        # 工具类
│   └── audit/                       # 内容审核
│
└── module/
    └── {module-name}/               # 按模块划分
        ├── controller/              # Controller 层（只做参数接收 + 调用 Service）
        ├── service/                 # Service 层（业务逻辑）
        ├── entity/                  # MyBatis-Plus 实体（对应数据库表）
        ├── mapper/                  # Mapper 接口
        ├── dto/                     # 入参 DTO（接收前端数据）
        └── vo/                      # 出参 VO（返回前端数据）
```

### 1.2 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `UserService`, `ProjectController` |
| 方法名 | lowerCamelCase | `getUserInfo()`, `createProject()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_UPLOAD_SIZE`, `DEFAULT_PAGE_SIZE` |
| 变量 | lowerCamelCase | `userId`, `projectList` |
| 数据库表 | snake_case | `user_follow`, `project_comment` |
| 数据库字段 | snake_case | `created_at`, `user_id` |
| Entity 类名 | 与表名对应，UpperCamelCase | 表 `project` → `Project` |
| DTO 类名 | 动作 + DTO | `RegisterDTO`, `CreateProjectDTO` |
| VO 类名 | 名词 + VO | `UserVO`, `ProjectDetailVO` |
| Controller | 名词 + Controller | `UserController`, `ProjectController` |
| Service | 名词 + Service | `UserService`, `ProjectService` |
| Mapper | 名词 + Mapper | `UserMapper`, `ProjectMapper` |

### 1.3 Lombok 使用规范

**三种场景，三种用法：**

| 场景 | 推荐注解 | 原因 |
|------|---------|------|
| DTO / VO | `@Data` | 纯数据载体，需要 getter/setter/toString/equals |
| Entity | `@Getter` `@Setter` | 避免 @Data 的 toString() 暴露所有字段值（含密码等敏感字段），避免 equals/hashCode 在继承场景的对称性问题 |
| LoginUser 等含 ThreadLocal 的类 | `@Getter` `@Setter` `@ToString` | @Data 的 toString() 会调用 static ThreadLocal.get()，可能导致意外行为 |

**为什么 Entity 不用 @Data：**
1. `@Data` 的 `toString()` 会输出所有字段值，Entity 可能包含 password 等敏感字段
2. `@Data` 的 `equals/hashCode` 包含所有字段，在继承体系中违反对称性（父类.equals(子类) ≠ 子类.equals(父类)）
3. Entity 通常不需要自定义 equals/hashCode（用 ID 比较即可）

### 1.4 分层职责（严格遵守）

```
Controller  →  只做：参数校验、调用 Service、包装 R<T> 返回
               不做：业务逻辑、数据库操作、异常处理（交给 GlobalExceptionHandler）

Service     →  只做：业务逻辑、事务管理、调用 Mapper
               不做：HTTP 相关操作、直接操作 HttpServletRequest

Mapper      →  只做：数据库 CRUD（继承 BaseMapper）
               复杂查询：写在 XML 或用 LambdaQueryWrapper

Entity      →  只做：数据库字段映射
               不做：业务逻辑、包含关联对象

DTO         →  只做：接收前端输入 + Validation 注解
               不包含：数据库 ID、创建时间等系统字段

VO          →  只做：返回前端的数据结构
               不包含：password、deleted 等敏感/内部字段
```

### 1.5 统一返回体

所有 Controller 接口必须返回 `R<T>`：

```java
// 成功
return R.ok();                        // 无数据
return R.ok(userVO);                  // 单个对象
return R.ok(pageResult);              // 分页数据

// 失败 — 使用 ErrorCode 枚举
throw new BizException(ErrorCode.USER_NOT_FOUND);
throw new BizException(ErrorCode.PARAM_ERROR);

// 失败 — 自定义错误码
throw new BizException(9998, "不能关注自己");
```

**禁止**：直接返回 `ResponseEntity`、`Map`、裸字符串。

### 1.6 异常处理

```java
// 业务异常 — 用 BizException
throw new BizException(ErrorCode.PROJECT_NOT_FOUND);

// 参数校验 — 用 Jakarta Validation（@Valid）
public R<Void> update(@Valid @RequestBody UpdateProjectDTO dto) { ... }

// 未知异常 — 全局 GlobalExceptionHandler 兜底，不要在 Controller catch
```

### 1.7 MyBatis-Plus 使用规范

```java
// ✅ 简单查询用 LambdaQueryWrapper
userMapper.selectOne(
    new LambdaQueryWrapper<User>()
        .eq(User::getUsername, username)
        .eq(User::getStatus, 1)
);

// ✅ 分页查询用 Page 对象
Page<User> page = new Page<>(pageNum, pageSize);
userMapper.selectPage(page, wrapper);

// ✅ JSON 字段必须标注 typeHandler + autoResultMap
@Data
@TableName(value = "project", autoResultMap = true)  // ← 必须！
public class Project {
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String parseResult;     // JSON 字段
}

// ✅ 复杂 JOIN 查询写在 XML 里（resources/mapper/*.xml）
// ❌ 禁止在 Service 里拼接 SQL 字符串
// ❌ 禁止使用 Wrapper 的 apply() 拼接原始 SQL（防注入）
```

### 1.8 事务管理

```java
// 只在 Service 层使用 @Transactional
@Transactional
public void follow(Long followerId, Long followingId) {
    // 写操作
}

// 只读事务加 readOnly = true
@Transactional(readOnly = true)
public Page<UserVO> searchUsers(String keyword, Page<User> page) {
    // 只读查询
}

// ❌ 禁止在 Controller 加 @Transactional
// ❌ 禁止吞掉异常后不回滚（catch + 不 throw = 事务不回滚）
```

---

## 二、Node.js 沙箱规范

### 2.1 文件结构

```
sandbox/
├── src/
│   ├── server.js            # Express 服务入口
│   └── judge-worker.js      # 判题核心逻辑
├── package.json
└── Dockerfile
```

### 2.2 编码规范

```javascript
// ✅ 使用 const，避免 var
const VirtualMachine = require('scratch-vm');

// ✅ 错误处理 — async/await + try/catch
async function executeTask(task) {
  try {
    const result = await worker.judge(task);
    task.status = 'done';
  } catch (err) {
    task.status = 'error';
    logger.error(`任务失败: ${task.id}`, err);
  }
}

// ✅ 日志用 winston，禁止 console.log
logger.info(`任务创建: ${taskId}`);
logger.error(`判题失败`, err);

// ❌ 禁止使用 eval()、new Function()、动态 require
// ❌ 禁止在判题逻辑里访问文件系统（下载除外）
```

---

## 三、数据库规范

### 3.1 表设计

```sql
-- ✅ 每张表必须有以下字段
`id`          BIGINT NOT NULL AUTO_INCREMENT  -- 主键
`created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
`updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
`deleted`     TINYINT NOT NULL DEFAULT 0      -- 逻辑删除（需要的表）

-- ✅ 索引命名
-- uk_  唯一索引：uk_username, uk_follow
-- idx_ 普通索引：idx_user, idx_status
-- ft_  全文索引：ft_search
```

### 3.2 字段规范

| 规则 | 说明 |
|------|------|
| 主键 | 统一用 `BIGINT AUTO_INCREMENT` |
| 外键 | 不建物理外键，应用层保证一致性 |
| 枚举 | 用 `VARCHAR` + 应用层常量，不用 MySQL ENUM |
| JSON | 可用 `JSON` 类型字段，Entity 中用 `@TableField(typeHandler = JacksonTypeHandler.class)` + `@TableName(autoResultMap = true)` |
| 时间 | 统一用 `DATETIME`，不用 `TIMESTAMP`（2038 问题） |
| 字符集 | 统一 `utf8mb4` + `utf8mb4_unicode_ci` |
| 软删除 | `deleted` 字段，查询时 WHERE deleted = 0 |

---

## 四、API 设计规范

### 4.1 URL 规范

```
# ✅ RESTful 风格
POST   /api/project              创建
GET    /api/project              列表（分页）
GET    /api/project/{id}         详情
PUT    /api/project/{id}         更新
DELETE /api/project/{id}         删除（软删除）

# ✅ 子资源
POST   /api/project/{id}/sb3     上传 sb3
GET    /api/project/{id}/sb3     下载 sb3
POST   /api/project/{id}/publish 发布

# ✅ 搜索
GET    /api/user/search?q=keyword&page=1&size=20

# ❌ 避免
GET    /api/getUser              ← 不是 RESTful
POST   /api/user/delete          ← 应该用 DELETE
GET    /api/project/list/all     ← 冗余路径
```

### 4.2 请求规范

```
# Content-Type
POST/PUT 请求体：application/json
文件上传：multipart/form-data

# 分页参数（统一）
page    — 页码，从 1 开始，默认 1
size    — 每页条数，默认 20，最大 100

# 排序参数（规划中，当前未实现）
sort    — 排序字段，如 "created_at"
order   — asc / desc，默认 desc
```

> ⚠️ **注意**：`sort`/`order` 排序参数为规划中的标准，当前版本尚未在 Controller 中实现。新增接口时推荐预留这两个参数。

### 4.3 响应规范

```json
// ✅ 成功
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}

// ✅ 分页
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [ ... ],
    "total": 100,
    "size": 20,
    "current": 1
  }
}

// ✅ 失败
{
  "code": 10001,
  "msg": "用户名已存在",
  "data": null
}
```

### 4.4 HTTP 状态码

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未登录 / Token 无效 |
| 403 | 无权限（角色不够） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 五、Git 提交规范

### 5.1 Commit Message 格式

```
<type>(<scope>): <subject>

# type 取值
feat      — 新功能
fix       — 修复 Bug
docs      — 文档变更
style     — 代码格式（不影响逻辑）
refactor  — 重构（不是新功能也不是修 Bug）
test      — 添加/修改测试
chore     — 构建工具、依赖变更
audit     — 代码审计修复

# scope 取值
对应模块名：user, editor, social, judge, classroom, system, sb3, sandbox, common, docker, docs

# 示例
feat(editor): 添加项目 CRUD 接口
fix(sandbox): 修复 scratch-vm 超时未清理问题
docs: 更新 PROGRESS.md Sprint 3 进度
audit(common): 修复 SensitiveWordFilter 线程安全问题
refactor(user): getFollowers 改用自定义 SQL JOIN 查询
```

### 5.2 分支策略

```
main          — 稳定版本，只接受 merge
develop       — 开发主线，Sprint 工作分支
feat/xxx      — 功能分支，从 develop 拉出
fix/xxx       — 修复分支
docs/xxx      — 文档分支
```

**流程**：
```
develop → feat/sprint3-editor → 开发 → 自测 → merge 回 develop
develop → main（每个 Sprint 结束，打 tag）
```

### 5.3 PR 规范

- 标题遵循 commit message 格式
- 描述包含：做了什么、为什么做、怎么测的
- 关联 Issue（如有）
- 自查清单：
  - [ ] 编译通过
  - [ ] 核心逻辑有单测
  - [ ] 无硬编码密码/密钥
  - [ ] 无 System.out.println / console.log
  - [ ] DTO/VO 字段脱敏

---

## 六、安全规范

### 6.1 必须遵守

| 规则 | 说明 |
|------|------|
| 密码 | 必须 BCrypt 加密存储，禁止明文 |
| Token | JWT 有效期 7 天，使用 HS256 签名 |
| SQL | 禁止拼接 SQL，用参数化查询 |
| 文件上传 | 校验文件类型 + 大小限制（50MB） |
| CORS | 只允许配置的域名 |
| 敏感词 | 用户输入必须过 SensitiveWordFilter |
| 角色校验 | 用 `@RequireRole` 注解，不在 Service 里 if 判断 |

### 6.2 文件上传校验标准实现

```java
// 在 Controller 层校验（进入 Service 之前拦截）
@PostMapping("/project/{id}/sb3")
public R<Void> uploadSb3(@PathVariable Long id,
                          @RequestParam("file") MultipartFile file) {
    // 1. 校验非空
    if (file.isEmpty()) {
        throw new BizException(ErrorCode.PARAM_ERROR);
    }
    // 2. 校验大小（50MB）
    if (file.getSize() > 50 * 1024 * 1024) {
        throw new BizException(ErrorCode.PARAM_ERROR, "文件大小超过 50MB 限制");
    }
    // 3. 校验类型（sb3 本质是 ZIP）
    String contentType = file.getContentType();
    if (!"application/zip".equals(contentType)
        && !"application/octet-stream".equals(contentType)) {
        throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 .sb3 文件");
    }
    projectService.uploadSb3(LoginUser.getUserId(), id, file);
    return R.ok();
}
```

> 文件校验在 **Controller 层** 完成，Service 层只处理业务逻辑。

### 6.3 sandbox 安全

| 规则 | 说明 |
|------|------|
| 超时 | 每次判题必须设置 timeLimit（默认 30s） |
| 网络 | sandbox 不应访问外部网络（除下载 sb3） |
| 文件系统 | 判题过程禁止读写本地文件（临时下载除外） |
| 进程隔离 | 生产环境用 Docker 容器隔离每次判题 |

---

## 七、文档规范

### 7.1 代码注释

```java
/**
 * 用户服务
 * 处理注册、登录、关注等用户相关业务
 */
@Service
public class UserService {

    /**
     * 用户注册
     * @param dto 注册信息（用户名、密码、昵称）
     * @return 用户 VO（脱敏）
     * @throws BizException 用户名已存在时抛出 USER_EXISTS
     */
    public UserVO register(RegisterDTO dto) { ... }
}
```

### 7.2 文档更新

| 文档 | 更新时机 |
|------|---------|
| PROGRESS.md | 每完成一个任务项，勾选 [x] |
| TODO.md | 每 Sprint 开始/结束时刷新 |
| README.md | 每 Sprint 结束更新 Roadmap 状态 |
| AUDIT_REPORT.md | 每 Sprint 结束做代码审计 |

---

*本规范是项目的基础约束。任何代码提交都必须符合以上标准。发现不合理的规范可以提出修改，但修改前必须遵守当前版本。*
