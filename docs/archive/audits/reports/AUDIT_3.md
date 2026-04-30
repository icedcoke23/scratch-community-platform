# 🔍 Sprint 3 联调审查报告

> 审查时间：2026-04-23 17:40
> 审查范围：scratch-sb3 模块 (11 文件) + scratch-editor 模块 (9 文件)
> 审查方式：API 静态分析 + 依赖链追踪 + 代码交叉比对

---

## 审查结果：1 严重（已修复）+ 0 中等

### S1. SB3Parser 未注册为 Spring Bean — 启动即失败 ❌→✅ 已修复
- **位置**: `ProjectService.java` 构造函数注入
- **问题**: `SB3Parser` 是纯 Java 类（无 @Component），但 `ProjectService` 通过 `@RequiredArgsConstructor` 构造器注入它，Spring 容器找不到 Bean 定义
- **修复**: 新增 `EditorConfig.java`，用 `@Bean` 注册 `SB3Parser`
- **状态**: ✅ 已推送到 GitHub

---

## 依赖链验证 ✅

```
scratch-sb3 (纯 Java, fastjson2 + lombok)
    ↑ 依赖
scratch-editor (scratch-common + scratch-sb3)
    ↑ 组件扫描
scratch-app (scanBasePackages = "com.scratch.community")
```

| 检查项 | 结果 |
|--------|------|
| scratch-sb3 无 Spring 依赖 | ✅ |
| scratch-editor pom.xml 依赖 scratch-sb3 | ✅ |
| scratch-editor pom.xml 依赖 scratch-common | ✅ |
| EditorConfig @Bean 注册 SB3Parser | ✅ 已修复 |

## Mapper 扫描验证 ✅

| 检查项 | 结果 |
|--------|------|
| `@MapperScan("com.scratch.community.module.*.mapper")` | ✅ 匹配 `module.editor.mapper.ProjectMapper` |
| `scanBasePackages = "com.scratch.community"` | ✅ 匹配 `module.editor.config.EditorConfig` |

## ErrorCode 常量验证 ✅

| 代码中使用的 ErrorCode | 是否存在 |
|----------------------|---------|
| `ErrorCode.PROJECT_NOT_FOUND` (20001) | ✅ |
| `ErrorCode.PROJECT_NO_PERMISSION` (20002) | ✅ |
| 直接用 `20003` (SB3_FORMAT_ERROR) | ✅ |
| 直接用 `9998` (PARAM_ERROR) | ✅ |

## API 接口链路验证 ✅

| API | Controller → Service → Mapper/Parser |
|-----|--------------------------------------|
| POST /api/project | create() → projectMapper.insert() ✅ |
| GET /api/project | myProjects() → projectMapper.selectPage() ✅ |
| GET /api/project/{id} | getDetail() → projectMapper.selectById() ✅ |
| PUT /api/project/{id} | update() → getAndCheckOwner() → projectMapper.updateById() ✅ |
| DELETE /api/project/{id} | delete() → getAndCheckOwner() → projectMapper.deleteById() ✅ |
| POST /api/project/{id}/sb3 | uploadSb3() → fileUploadUtils.upload() + sb3Parser.parse() ✅ |
| GET /api/project/{id}/sb3 | getSb3Url() → projectMapper.selectById() ✅ |
| POST /api/project/{id}/publish | publish() → getAndCheckOwner() → projectMapper.updateById() ✅ |

## 数据流验证 ✅

| 流程 | 验证 |
|------|------|
| CreateProjectDTO → Project 字段映射 | title/description/coverUrl/tags 全部匹配 ✅ |
| Project → ProjectVO BeanUtils.copyProperties | 字段名一致 ✅ |
| Project → ProjectDetailVO BeanUtils.copyProperties | 字段名一致 + 额外字段 sb3Url/parseResult/isLiked ✅ |
| Entity @TableField(fill=INSERT) → MybatisMetaObjectHandler | createdAt 自动填充 ✅ |
| Entity @TableField(fill=INSERT_UPDATE) → MybatisMetaObjectHandler | updatedAt 自动填充 ✅ |
| Entity @TableLogic → MyBatis-Plus | deleted 逻辑删除 ✅ |
| Entity @TableName(autoResultMap=true) + JacksonTypeHandler | JSON 字段 parseResult 正确映射 ✅ |

## 分页处理一致性 ✅

| 模块 | 处理方式 |
|------|---------|
| user 模块 | `R.ok(userService.searchUsers(...))` 直接包装 `Page<UserVO>` |
| editor 模块 | `R.ok(projectService.myProjects(...))` 直接包装 `Page<ProjectVO>` |
| **一致性** | ✅ 相同模式 |

## 未联调项（需要本地环境）

| 项目 | 原因 | 优先级 |
|------|------|--------|
| `mvn compile` 编译验证 | 无 Java/Maven 环境 | P0 — 首次本地部署时验证 |
| `docker-compose up` 启动 | 无 Docker 环境 | P0 |
| `/api/health` 健康检查 | 依赖上两项 | P0 |
| 注册 → 登录 → 拿 Token | 依赖上两项 | P0 |
| 项目 CRUD 全链路 | 依赖上两项 | P0 |
| sb3 上传 + 解析 | 依赖上两项 + 需要测试 sb3 文件 | P0 |

---

## 结论

代码层面审查通过。唯一的阻断问题（SB3Parser Bean 注册）已修复。剩余联调项需要本地 Java/Maven/Docker 环境。
