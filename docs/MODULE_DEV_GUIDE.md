# 🧩 模块开发指南

> 版本：v2.0 | 日期：2026-04-25
> 每个模块从 0 到 1 的标准开发流程
> v1.1: 二次审计修复 — Entity 改用 @Getter/@Setter、VO 不继承、补全 Service 实现、JSON 库标注

---

## 一、模块开发清单

每个模块开发前，按此清单逐项完成。以 editor 模块为示例。

### Step 1：确认数据表 ✅

确认 init.sql 中对应的表已存在，字段满足业务需求。

```sql
-- editor 模块对应表：project
-- 确认字段：id, user_id, title, description, cover_url, sb3_url, status,
--           block_count, complexity_score, like_count, comment_count,
--           view_count, parse_result, tags, created_at, updated_at, deleted
```

**如果需要新增表或字段**：先修改 `docker/init.sql`，再开发代码。

### Step 2：创建 Entity

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/entity/Project.java

@Getter                                                // ← 不用 @Data，见 CODING_STANDARDS 1.3
@Setter
@TableName(value = "project", autoResultMap = true)    // ← autoResultMap 必须！否则 JSON 字段查询返回 null
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String description;
    private String coverUrl;
    private String sb3Url;
    private String status;          // draft / published

    private Integer blockCount;
    private Double complexityScore;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String parseResult;     // JSON 字符串

    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
```

**检查项**：
- [ ] 字段名与数据库列名对应（驼峰 ↔ 下划线）
- [ ] `@TableId` 标注主键
- [ ] `@TableLogic` 标注逻辑删除字段
- [ ] JSON 字段用 `@TableField(typeHandler = JacksonTypeHandler.class)`
- [ ] `@TableName(autoResultMap = true)` — **有 JSON 字段时必须加**
- [ ] 用 `@Getter/@Setter`，不用 `@Data`
- [ ] 时间字段用 `LocalDateTime`

### Step 3：创建 Mapper

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/mapper/ProjectMapper.java

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    // 简单 CRUD 继承 BaseMapper 即可
    // 复杂查询在这里定义方法，对应 XML 写 SQL
}
```

**如果需要自定义 SQL**：
```java
// Mapper 接口
List<ProjectVO> selectHotProjects(@Param("days") int days, @Param("limit") int limit);

// resources/mapper/editor/ProjectMapper.xml
<select id="selectHotProjects" resultType="...">
    SELECT * FROM project
    WHERE status = 'published' AND deleted = 0
      AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
    ORDER BY like_count + view_count DESC
    LIMIT #{limit}
</select>
```

### Step 4：创建 DTO

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/dto/CreateProjectDTO.java

@Data                                                    // DTO 可以用 @Data
public class CreateProjectDTO {

    @NotBlank(message = "项目标题不能为空")
    @Size(max = 200, message = "标题最多 200 字")
    private String title;

    @Size(max = 5000, message = "描述最多 5000 字")
    private String description;

    private String coverUrl;
    private String tags;
}
```

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/dto/UpdateProjectDTO.java

@Data
public class UpdateProjectDTO {

    @Size(max = 200, message = "标题最多 200 字")
    private String title;           // 可选更新

    @Size(max = 5000)
    private String description;     // 可选更新

    private String coverUrl;
    private String tags;
}
```

**DTO 规则**：
- [ ] 只包含前端需要传入的字段
- [ ] 用 `@NotBlank` / `@Size` / `@Min` / `@Max` 等做参数校验
- [ ] 更新 DTO 的字段全部用包装类型（`Integer` 而非 `int`），支持部分更新
- [ ] 不包含 `id`、`userId`、`createdAt` 等系统字段

### Step 5：创建 VO

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/vo/ProjectVO.java

@Data                                                    // VO 可以用 @Data
public class ProjectVO {
    private Long id;
    private Long userId;
    private String authorName;      // 关联查询
    private String authorAvatar;    // 关联查询
    private String title;
    private String description;
    private String coverUrl;
    private String status;
    private Integer blockCount;
    private Double complexityScore;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private String tags;
    private LocalDateTime createdAt;
    // ❌ 不返回：sb3Url, parseResult, deleted, updatedAt
}
```

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/vo/ProjectDetailVO.java

// ⚠️ 不继承 ProjectVO！Lombok @Data 在继承场景有 equals/hashCode 对称性问题
@Data
public class ProjectDetailVO {
    // 复制 ProjectVO 的全部字段
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String description;
    private String coverUrl;
    private String status;
    private Integer blockCount;
    private Double complexityScore;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private String tags;
    private LocalDateTime createdAt;

    // 详情页额外字段
    private String sb3Url;          // 下载链接
    private String parseResult;     // 解析结果 JSON
    private Boolean isLiked;        // 当前用户是否已点赞
}
```

**VO 规则**：
- [ ] 脱敏：不返回 `password`、`deleted` 等敏感字段
- [ ] 列表 VO 精简，详情 VO 完整
- [ ] 关联数据（如作者名称）在 VO 中平铺，不在前端再查
- [ ] **详情 VO 不继承列表 VO**（避免 Lombok equals/hashCode 对称性问题）

### Step 6：创建 Service

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/service/ProjectService.java

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final SB3Parser sb3Parser;            // 来自 scratch-sb3 模块（fastjson2）
    private final FileUploadUtils fileUploadUtils;

    /**
     * 创建项目
     */
    @Transactional
    public ProjectVO create(Long userId, CreateProjectDTO dto) {
        Project project = new Project();
        BeanUtils.copyProperties(dto, project);
        project.setUserId(userId);
        project.setStatus("draft");
        project.setLikeCount(0);
        project.setCommentCount(0);
        project.setViewCount(0);
        projectMapper.insert(project);
        return toVO(project);
    }

    /**
     * 我的项目列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<ProjectVO> myProjects(Long userId, Page<Project> page) {
        Page<Project> result = projectMapper.selectPage(page,
            new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(Project::getDeleted, 0)
                .orderByDesc(Project::getCreatedAt)
        );
        return toVOPage(result);
    }

    /**
     * 项目详情
     */
    @Transactional(readOnly = true)
    public ProjectDetailVO getDetail(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);
        ProjectDetailVO vo = new ProjectDetailVO();
        BeanUtils.copyProperties(project, vo);
        // TODO: 查询作者信息、是否点赞
        return vo;
    }

    /**
     * 更新项目信息
     */
    @Transactional
    public void update(Long userId, Long projectId, UpdateProjectDTO dto) {
        Project project = getAndCheckOwner(userId, projectId);
        if (dto.getTitle() != null) project.setTitle(dto.getTitle());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) project.setCoverUrl(dto.getCoverUrl());
        if (dto.getTags() != null) project.setTags(dto.getTags());
        projectMapper.updateById(project);
    }

    /**
     * 删除项目（软删除）
     */
    @Transactional
    public void delete(Long userId, Long projectId) {
        Project project = getAndCheckOwner(userId, projectId);
        projectMapper.deleteById(projectId);
    }

    /**
     * 上传 sb3 文件 + 自动解析
     */
    @Transactional
    public void uploadSb3(Long userId, Long projectId, MultipartFile file) {
        // 1. 校验项目归属
        Project project = getAndCheckOwner(userId, projectId);

        // 2. 上传到 MinIO
        String key = fileUploadUtils.upload(file, "sb3");
        String url = fileUploadUtils.getUrl("sb3", key);

        // 3. 解析 sb3（注意：scratch-sb3 用 fastjson2，不要用 Jackson 的 JSON 类）
        SB3ParseResult parseResult = sb3Parser.parse(file.getBytes());

        // 4. 更新项目
        project.setSb3Url(url);
        project.setBlockCount(parseResult.getBlockCount());
        project.setComplexityScore(parseResult.getComplexityScore());
        // fastjson2 序列化
        project.setParseResult(com.alibaba.fastjson2.JSON.toJSONString(parseResult));
        projectMapper.updateById(project);
    }

    /**
     * 获取 sb3 下载链接
     */
    @Transactional(readOnly = true)
    public String getSb3Url(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);
        if (project.getSb3Url() == null) {
            throw new BizException(20003, "项目未上传 sb3 文件");
        }
        return project.getSb3Url();
    }

    /**
     * 发布项目
     */
    @Transactional
    public void publish(Long userId, Long projectId) {
        Project project = getAndCheckOwner(userId, projectId);
        if (project.getSb3Url() == null) {
            throw new BizException(20003, "请先上传 sb3 文件再发布");
        }
        project.setStatus("published");
        projectMapper.updateById(project);
    }

    // ==================== 私有方法 ====================

    private Project getAndCheck(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(20001, "项目不存在");
        }
        return project;
    }

    private Project getAndCheckOwner(Long userId, Long projectId) {
        Project project = getAndCheck(projectId);
        if (!project.getUserId().equals(userId)) {
            throw new BizException(20002, "无权操作此项目");
        }
        return project;
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(project, vo);
        return vo;
    }

    private Page<ProjectVO> toVOPage(Page<Project> page) {
        Page<ProjectVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }
}
```

**Service 规则**：
- [ ] 构造器注入（`@RequiredArgsConstructor`），不用 `@Autowired`
- [ ] 写操作加 `@Transactional`
- [ ] 只读操作加 `@Transactional(readOnly = true)`
- [ ] 权限校验在 Service 内完成（不依赖 Controller）
- [ ] 对外抛 `BizException`，不抛 RuntimeException
- [ ] JSON 序列化使用模块内已有的库（scratch-sb3 用 fastjson2）

### Step 7：创建 Controller

```java
// scratch-editor/src/main/java/com/scratch/community/module/editor/controller/ProjectController.java

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/project")
    public R<ProjectVO> create(@Valid @RequestBody CreateProjectDTO dto) {
        return R.ok(projectService.create(LoginUser.getUserId(), dto));
    }

    @GetMapping("/project")
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(projectService.myProjects(LoginUser.getUserId(), new Page<>(page, size)));
    }

    @GetMapping("/project/{id}")
    public R<ProjectDetailVO> detail(@PathVariable Long id) {
        return R.ok(projectService.getDetail(LoginUser.getUserId(), id));
    }

    @PutMapping("/project/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody UpdateProjectDTO dto) {
        projectService.update(LoginUser.getUserId(), id, dto);
        return R.ok();
    }

    @DeleteMapping("/project/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(LoginUser.getUserId(), id);
        return R.ok();
    }

    @PostMapping("/project/{id}/sb3")
    public R<Void> uploadSb3(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file) {
        // 文件校验在 Controller 层完成
        if (file.isEmpty()) {
            throw new com.scratch.community.common.exception.BizException(9998, "文件不能为空");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new com.scratch.community.common.exception.BizException(9998, "文件大小超过 50MB 限制");
        }
        projectService.uploadSb3(LoginUser.getUserId(), id, file);
        return R.ok();
    }

    @GetMapping("/project/{id}/sb3")
    public R<String> downloadSb3(@PathVariable Long id) {
        return R.ok(projectService.getSb3Url(LoginUser.getUserId(), id));
    }

    @PostMapping("/project/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        projectService.publish(LoginUser.getUserId(), id);
        return R.ok();
    }
}
```

**Controller 规则**：
- [ ] 方法名对应 HTTP 语义（create/list/detail/update/delete）
- [ ] 路径遵循 RESTful 规范
- [ ] 用 `@Valid` 触发 DTO 校验
- [ ] 文件上传在 Controller 层校验大小/类型
- [ ] 只调用 Service，不写业务逻辑
- [ ] 统一返回 `R<T>`

### Step 8：联调验证

```bash
# 1. 启动基础设施
cd docker && docker-compose up -d mysql redis minio

# 2. 启动后端
cd backend && mvn spring-boot:run -pl scratch-app

# 3. 验证
# 注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","nickname":"测试用户"}'

# 登录（拿 Token）
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 创建项目（带 Token）
curl -X POST http://localhost:8080/api/project \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"title":"我的第一个项目","description":"Hello Scratch"}'

# 上传 sb3
curl -X POST http://localhost:8080/api/project/1/sb3 \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@test.sb3"

# 查看详情
curl http://localhost:8080/api/project/1 \
  -H "Authorization: Bearer <TOKEN>"

# 发布
curl -X POST http://localhost:8080/api/project/1/publish \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 二、模块依赖关系

```
scratch-common-core     ← 所有公共模块的基础（R<T>, ErrorCode, 事件, 工具, 配置）
    ↑
scratch-common-redis    ← 依赖 core（分布式锁/限流/缓存/调度锁）
    ↑
scratch-common-security ← 依赖 redis（JWT/认证/角色/幂等性）
    ↑
scratch-common-audit    ← 依赖 core（敏感词过滤）

scratch-sb3             ← 依赖 core（Jackson）
scratch-judge-core      ← 独立
    ↑
scratch-user            ← 依赖 security
scratch-editor          ← 依赖 security + sb3
scratch-social          ← 依赖 security + audit
scratch-judge           ← 依赖 security + judge-core
scratch-classroom       ← 依赖 security
scratch-system          ← 依赖 security + audit
    ↑
scratch-app             ← 聚合所有模块
```

**依赖规则**：
- ❌ 禁止反向依赖（editor 不能依赖 user）
- ❌ 禁止循环依赖
- ✅ 模块间通过 userId/judgeId 等 ID 关联，不直接依赖其他业务模块
- ✅ 共享库（sb3, judge-core）不依赖任何业务模块
- ✅ scratch-app 可依赖所有模块（它是聚合层）

---

## 三、ErrorCode 分配表

| 模块 | 范围 | 已使用 |
|------|------|--------|
| 通用 | 0-9999 | 0, 9996-9999 |
| user | 10000-19999 | 10001-10007 |
| editor | 20000-29999 | 20001-20004 |
| social | 30000-39999 | 30001-30004 |
| judge | 40000-49999 | 40001-40004 |
| classroom | 50000-59999 | 50001-50003 |
| system | 60000-69999 | 60001-60003 |

**新增 ErrorCode**：在对应模块范围内顺序添加，不跳跃。

---

## 四、JSON 库使用说明

| 模块 | JSON 库 | import |
|------|---------|--------|
| 全部模块 | Jackson（Spring 内置） | `com.fasterxml.jackson.databind.ObjectMapper` |

> ✅ 全项目统一使用 Jackson，无 fastjson2 依赖。

---

## 五、Sprint 开发节奏

```
Day 1     确认数据表 + 创建 Entity/Mapper/DTO/VO
Day 2-3   开发 Service 核心逻辑
Day 4     创建 Controller + 单测
Day 5     联调验证 + Bug 修复
Day 6     代码审计 + 文档更新
Day 7     缓冲 / 技术债 / 下一 Sprint 规划
```

---

*每个新模块开发前，先读一遍本指南。按照 Step 1-8 顺序推进，不要跳步。*
