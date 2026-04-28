# 贡献指南

感谢你对 Scratch Community Platform 的关注！以下是参与贡献的流程和规范。

## 开发流程

### 1. Fork & Clone

```bash
git clone https://github.com/YOUR_USERNAME/scratch-community-platform.git
cd scratch-community-platform
git remote add upstream https://github.com/icedcoke23/scratch-community-platform.git
```

### 2. 创建分支

```bash
git checkout -b feat/your-feature
# 或
git checkout -b fix/your-bugfix
```

分支命名规范：
- `feat/xxx` — 新功能
- `fix/xxx` — Bug 修复
- `docs/xxx` — 文档变更
- `refactor/xxx` — 重构
- `test/xxx` — 测试

### 3. 开发

请遵循 [编码规范](docs/CODING_STANDARDS.md)：

- **Java**：包结构、命名规范、Lombok 用法、分层职责
- **Node.js**：const 优先、async/await 错误处理、winston 日志
- **数据库**：表设计规范、索引命名、字符集
- **API**：RESTful 风格、统一返回体 `R<T>`

### 4. 自查

提交前对照 [质量检查清单](docs/QA_CHECKLIST.md) 自查：

```bash
# 编译检查
cd backend && mvn clean compile

# 运行测试
mvn test

# 打包验证
mvn package -DskipTests
```

### 5. 提交

Commit Message 格式：

```
<type>(<scope>): <subject>

# 示例
feat(editor): 添加项目 Remix 接口
fix(sandbox): 修复 scratch-vm 超时未清理问题
docs: 更新 PROGRESS.md Sprint 14 进度
```

### 6. Pull Request

- 标题遵循 commit message 格式
- 描述包含：做了什么、为什么做、怎么测的
- 关联 Issue（如有）

## 开发环境

### 前置条件

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+

### 启动

```bash
# 1. 基础设施
cd docker && docker-compose up -d mysql redis minio

# 2. 后端
cd backend && mvn spring-boot:run -pl scratch-app

# 3. 沙箱
cd sandbox && npm install && npm start

# 4. 验证
curl http://localhost:8080/api/health
```

## 代码规范要点

| 规则 | 说明 |
|------|------|
| Entity | 用 `@Getter/@Setter`，不用 `@Data` |
| DTO/VO | 用 `@Data`，详情 VO 不继承列表 VO |
| Controller | 只做参数校验 + 调用 Service + 返回 `R<T>` |
| Service | 构造器注入（`@RequiredArgsConstructor`），写操作加 `@Transactional` |
| ErrorCode | 按模块分段：通用 0-9999、user 10000-19999、editor 20000-29999... |
| JSON 库 | scratch-sb3 用 fastjson2，其他模块用 Jackson |
| Git | 遵循 commit message 格式，PR 前自审 |

## 报告 Bug

通过 [GitHub Issues](https://github.com/icedcoke23/scratch-community-platform/issues) 提交，包含：

- 复现步骤
- 期望行为 vs 实际行为
- 环境信息（JDK 版本、OS 等）
- 相关日志

## 提出功能建议

通过 Issue 提交，标记为 `enhancement`，描述：

- 使用场景
- 预期行为
- 实现思路（可选）

## 文档

项目文档位于 `docs/` 目录，开发前请阅读：

- [编码规范](docs/CODING_STANDARDS.md)
- [模块开发指南](docs/MODULE_DEV_GUIDE.md)
- [踩坑记录](docs/PITFALLS.md)
