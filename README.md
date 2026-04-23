# Scratch Community Platform

面向 K12 课后教学的 Scratch 编程社区平台。

## 架构

```
backend/          Spring Boot 3 多模块 (JDK 17)
├── scratch-app/          启动模块
├── scratch-common/       公共基础 (认证/异常/审核/工具)
├── scratch-user/         用户系统
├── scratch-editor/       创作引擎
├── scratch-social/       社区系统
├── scratch-judge/        判题系统
├── scratch-classroom/    教室管理
├── scratch-system/       系统管理
├── scratch-sb3/          sb3 解析库 (共享库)
└── scratch-judge-core/   判题核心库 (共享库)

sandbox/          Node.js 判题沙箱 (scratch-vm headless)
docker/           Docker Compose + Dockerfile + 初始化 SQL
docs/             开发文档
前期/             前期设计文档
```

## 快速启动

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d mysql redis minio
```

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar scratch-app/target/*.jar
```

或开发模式:

```bash
cd backend
mvn spring-boot:run -pl scratch-app
```

### 3. 启动沙箱

```bash
cd sandbox
npm install
npm start
```

### 4. 验证

```bash
curl http://localhost:8080/api/health
```

## 技术栈

| 层级 | 选型 |
|---|---|
| 后端 | Spring Boot 3.2 + JDK 17 + MyBatis-Plus 3.5 |
| 判题沙箱 | Node.js 18 + scratch-vm |
| 数据库 | MySQL 8.0 + Redis 7 + MinIO |
| 部署 | Docker + Docker Compose |

## 模块

| 模块 | 说明 | 状态 |
|---|---|---|
| user | 注册/登录/关注/班级 | 🚧 开发中 |
| editor | 项目CRUD/sb3保存解析 | 📋 待开发 |
| social | 点赞/评论/排行榜/Feed | 📋 待开发 |
| judge | 题目/判题/提交记录 | 📋 待开发 |
| classroom | 作业布置/提交/批改 | 📋 待开发 |
| system | 审核/通知/配置 | 📋 待开发 |
| sb3-parser | sb3 深度解析库 | 📋 待开发 |
| judge-core | 判题核心库 | 📋 待开发 |
| sandbox | 判题沙箱服务 | ✅ 骨架完成 |

## 文档

- [系统设计草案 v0.3](前期/架构设计/系统设计草案_v0.3.md)
- [模块详细设计 v0.2](前期/架构设计/模块详细设计_v0.2.md)
- [数据库表设计 v0.2](前期/数据库设计/核心表设计_v0.2.md)
- [技术栈选型 v0.4](前期/技术选型/技术栈选型_v0.4.md)
- [功能清单 v0.5](前期/需求文档/功能清单_v0.5.md)
- [开发进度](docs/PROGRESS.md)
