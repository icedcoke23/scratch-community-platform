# 开发进度

## Phase 1: MVP 核心闭环

### Sprint 1: 脚手架 + common 基础层
- [x] Maven 多模块项目结构
- [x] common 层: R<T> 统一返回体 + ErrorCode
- [x] common 层: GlobalExceptionHandler 全局异常处理
- [x] common 层: JwtUtils + AuthInterceptor + LoginUser
- [x] common 层: RequireRole 角色注解
- [x] common 层: WebMvcConfig (CORS + 拦截器)
- [x] common 层: MybatisPlusConfig 分页插件
- [x] common 层: MinioConfig + FileUploadUtils
- [x] common 层: RedisUtils
- [x] common 层: SensitiveWordFilter 敏感词过滤
- [x] app 模块: ScratchCommunityApplication 启动类
- [x] app 模块: HealthController 健康检查
- [x] app 模块: application.yml 配置
- [x] Docker Compose: MySQL + Redis + MinIO
- [x] Docker: init.sql 建表脚本 (18 张表)
- [x] Docker: Dockerfile.backend

### Sprint 2: user 模块 (用户系统)
- [x] Entity: User, UserFollow, ClassRoom, ClassStudent
- [x] Mapper: UserMapper, UserFollowMapper, ClassMapper, ClassStudentMapper
- [x] DTO: RegisterDTO, LoginDTO, UpdateUserDTO, ChangePasswordDTO, CreateClassDTO
- [x] VO: UserVO, LoginVO, ClassVO
- [x] Service: UserService (注册/登录/信息/关注/搜索)
- [x] Service: ClassService (创建/加入/成员管理)
- [x] Controller: UserController (全部 API)
- [x] 补全: searchUsers 返回 UserVO 分页结果
- [ ] TODO: 粉丝列表/关注列表查询补全 (需要自定义 SQL JOIN)
- [ ] TODO: ClassService.joinClass 补全 class_student 插入
- [ ] TODO: ClassService.getMyClasses 补全
- [ ] TODO: 测试用例

### 代码审计 (2026-04-23)
- [x] S1: application.yml 日志改为环境变量控制 + dev/prod profile
- [x] S2: init.sql 管理员密码 BCrypt 哈希替换为真实值
- [x] S3: AuthInterceptor 区分 401/403 HTTP 状态码
- [x] S4: scratch-user 移除不必要的 scratch-sb3 依赖
- [x] S5: Docker MySQL 移除无效环境变量
- [x] S6: SensitiveWordFilter 线程安全修复 (ConcurrentHashMap.newKeySet)
- [x] M2: docker-compose 移除废弃 version 字段
- [x] M3: hutool-all 瘦身为 hutool-core
- [x] M4: JwtUtils 移除未使用 import
- [x] M7: LoginUser 改用 @Getter/@Setter 避免 ThreadLocal 问题
- [x] M8: sandbox 变量类型判断修复
- [x] M9: searchUsers 补全返回值转换
- [x] 补充: MybatisMetaObjectHandler 自动填充处理器
- [x] 补充: ClassStudent entity + mapper
- [x] 补充: social/system 模块移除不必要的 sb3 依赖

### Sprint 3: editor 模块 + sb3-parser 库
- [ ] sb3-parser: SB3Parser 入口
- [ ] sb3-parser: SB3Unzipper 解压
- [ ] sb3-parser: ProjectJsonParser 解析
- [ ] sb3-parser: BlockChainBuilder 程序链
- [ ] sb3-parser: SpriteExtractor 角色提取
- [ ] sb3-parser: ComplexityCalculator 复杂度评分
- [ ] editor: Entity/Mapper/DTO/VO
- [ ] editor: ProjectService (CRUD + 保存解析)
- [ ] editor: ProjectController

### Sprint 4: judge 模块 + judge-core 库 + sandbox
- [ ] sandbox: scratch-vm headless 判题逻辑完善
- [ ] judge-core: JudgeCore + ScratchAlgoStrategy + ChoiceStrategy
- [ ] judge: Entity/Mapper/DTO/VO
- [ ] judge: ProblemService (题目 CRUD)
- [ ] judge: JudgeService (提交/异步判题/结果)

### Sprint 5: social 模块
- [ ] social: Entity/Mapper
- [ ] social: SocialService (点赞/评论)
- [ ] social: FeedService (最新+热门)
- [ ] social: RankService (Redis Sorted Set)

### Sprint 6: classroom 模块
- [ ] classroom: Entity/Mapper
- [ ] classroom: HomeworkService (布置/提交/批改)

### Sprint 7: system 模块
- [ ] system: NotifyService (通知 CRUD)
- [ ] system: AuditService (敏感词过滤)
- [ ] system: ConfigService (系统配置)
- [ ] system: 管理员接口

## 关键决策

| 日期 | 决策 |
|---|---|
| 2026-04-23 | 从 12 模块精简到 6 模块 + 2 共享库 |
| 2026-04-23 | MVP 不做: 云变量/积分/AI判题/课程/学情统计 |
| 2026-04-23 | 判题沙箱: Node.js + scratch-vm headless |
| 2026-04-23 | 存储: MySQL + Redis + MinIO (不做 MongoDB/ES) |
