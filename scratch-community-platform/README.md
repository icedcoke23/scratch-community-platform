# Scratch 社区平台

基于 TurboWarp 的自托管 Scratch 在线编程平台，支持项目创建、编辑、预览和社区分享功能。

## 📖 项目介绍

Scratch 社区平台是一个现代化的在线编程教育平台，提供了完整的 Scratch 项目管理和创作环境。该平台集成了 TurboWarp 编辑器，支持用户创建、编辑、运行和分享 Scratch 项目。

### 核心特性

- 🎨 **在线编辑器**：集成 TurboWarp，提供流畅的代码块编辑体验
- ▶️ **实时预览**：支持项目即时运行和全屏播放
- 📁 **项目管理**：创建、编辑、发布和分享 Scratch 项目
- 🔒 **安全沙箱**：基于 iframe 沙箱机制，确保平台安全
- 📱 **响应式设计**：适配多种设备，随时随地创作

## 🛠 技术栈

### 前端技术

- **框架**：Vue 3 (Composition API)
- **构建工具**：Vite 5
- **路由**：Vue Router 4
- **状态管理**：Pinia
- **UI 组件**：Element Plus
- **HTTP 客户端**：Axios
- **编程语言**：TypeScript 5

### 后端技术

- **框架**：Express.js
- **编程语言**：TypeScript
- **文件上传**：Multer
- **跨域支持**：Cors
- **运行时**：Node.js

### 部署技术

- **Web 服务器**：Nginx
- **容器化**：Docker (可选)
- **进程管理**：PM2 (可选)

## 🚀 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0 或 yarn >= 1.22.0
- Git
- (可选) Docker 和 Docker Compose

### 克隆项目

```bash
git clone https://github.com/your-repo/scratch-community-platform.git
cd scratch-community-platform
```

### 安装依赖

#### 后端依赖

```bash
cd backend
npm install
```

#### 前端依赖

```bash
cd frontend-vue
npm install
```

### 构建 TurboWarp

TurboWarp 是平台的核心编辑器组件，需要单独构建：

```bash
cd ..
chmod +x scripts/build-turbowarp.sh
./scripts/build-turbowarp.sh
```

> 💡 构建脚本会自动下载 TurboWarp 源码并构建到 `frontend-vue/public/turbowarp/` 目录。

### 启动开发服务器

#### 启动后端服务

```bash
cd backend
npm run dev
```

后端服务会在 `http://localhost:8080` 启动，健康检查端点：`http://localhost:8080/health`

#### 启动前端服务

```bash
cd frontend-vue
npm run dev
```

前端服务会在 `http://localhost:5173` 启动（Vite 默认端口）。

### 访问平台

打开浏览器访问 `http://localhost:5173`，即可使用平台功能。

## 📁 项目结构

```
scratch-community-platform/
├── backend/                    # 后端服务
│   ├── src/
│   │   ├── controllers/       # 控制器层
│   │   ├── middleware/       # 中间件
│   │   ├── routes/           # 路由定义
│   │   ├── services/         # 业务逻辑层
│   │   ├── types/            # TypeScript 类型定义
│   │   ├── utils/            # 工具函数
│   │   ├── app.ts            # 应用主文件
│   │   └── index.ts          # 服务入口
│   ├── package.json
│   └── tsconfig.json
├── frontend-vue/              # 前端应用
│   ├── src/
│   │   ├── api/              # API 调用层
│   │   ├── components/       # 公共组件
│   │   ├── composables/      # 组合式函数
│   │   ├── router/           # 路由配置
│   │   ├── stores/           # 状态管理
│   │   ├── utils/            # 工具函数
│   │   ├── views/            # 页面组件
│   │   ├── App.vue           # 根组件
│   │   └── main.ts           # 应用入口
│   ├── public/
│   │   └── turbowarp/        # TurboWarp 静态资源
│   ├── package.json
│   └── vite.config.ts
├── scripts/                   # 构建脚本
│   └── build-turbowarp.sh    # TurboWarp 构建脚本
├── docs/                      # 项目文档
│   ├── ARCHITECTURE.md       # 架构设计文档
│   ├── DEPLOYMENT.md         # 部署指南
│   ├── DEV_REPORT.md         # 开发报告
│   ├── TESTING.md            # 测试文档
│   └── TURBOWARP_INTEGRATION.md  # TurboWarp 集成文档
├── nginx.conf                # Nginx 配置文件
├── package.json              # 根目录 package.json
└── README.md                 # 项目说明文档
```

## 🏗 前端架构

### 目录说明

- **api/**：封装 API 调用，统一管理接口请求
- **components/**：可复用的 Vue 组件
- **composables/**：Vue 3 组合式函数
- **router/**：路由配置和导航守卫
- **stores/**：Pinia 状态管理 stores
- **utils/**：工具函数和辅助模块
- **views/**：页面级组件，对应路由

### 主要页面路由

| 路径 | 组件 | 说明 |
|------|------|------|
| `/` | HomeView | 首页，展示平台介绍 |
| `/preview-demo` | PreviewDemoView | 预览功能演示 |
| `/projects` | ProjectListView | 项目列表页面 |
| `/editor/:id?` | ScratchEditorView | Scratch 编辑器页面 |

### 核心组件

- **ScratchPreview.vue**：Scratch 项目预览组件，支持全屏播放
- **ScratchEditorView.vue**：Scratch 编辑器页面，整合 TurboWarp
- **ScratchBridge.ts**：postMessage 通信桥接模块
- **turbowarpConfig.ts**：TurboWarp 配置管理模块

## 🔌 后端 API

### 基础信息

- **基础 URL**：`/api/v1`
- **认证方式**：Bearer Token (可选)
- **响应格式**：

```json
{
  "code": 0,
  "data": {},
  "msg": "success",
  "timestamp": 1234567890
}
```

### API 端点

#### 健康检查

```
GET /health
```

响应示例：

```json
{
  "code": 0,
  "data": { "status": "ok" },
  "msg": "success",
  "timestamp": 1710000000000
}
```

#### 项目管理

##### 获取项目列表

```
GET /api/v1/project
```

查询参数：

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| page | number | 页码 | 1 |
| pageSize | number | 每页数量 | 10 |
| status | string | 状态过滤 | - |
| search | string | 搜索关键词 | - |
| sortBy | string | 排序字段 | createdAt |
| sortOrder | string | 排序方向 | desc |

##### 获取项目详情

```
GET /api/v1/project/:id
```

##### 创建项目

```
POST /api/v1/project
```

请求体：

```json
{
  "title": "我的项目",
  "description": "项目描述",
  "tags": "游戏,动画"
}
```

##### 更新项目

```
PUT /api/v1/project/:id
```

##### 发布项目

```
POST /api/v1/project/:id/publish
```

##### 删除项目

```
DELETE /api/v1/project/:id
```

##### 上传 SB3 文件

```
POST /api/v1/project/:id/sb3/upload
Content-Type: multipart/form-data

file: <SB3 文件>
```

##### 下载 SB3 文件

```
GET /api/v1/project/:id/sb3/download
```

返回 SB3 格式的项目文件。

## 🖥 部署说明

### 开发环境部署

1. 安装依赖（见快速开始）
2. 构建 TurboWarp
3. 启动后端和前端服务
4. 配置 Nginx 反向代理（可选）

### 生产环境部署

详见 [部署文档](docs/DEPLOYMENT.md)，包括：

- 环境要求
- 手动部署步骤
- Docker 部署
- Nginx 配置
- HTTPS 配置
- 性能优化
- 监控和日志

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /var/www/scratch-platform/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # TurboWarp 静态资源
    location /turbowarp/ {
        alias /var/www/scratch-platform/turbowarp/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🔧 开发指南

### 代码规范

- 使用 TypeScript 进行开发
- 遵循 ESLint 配置的代码规范
- 使用 Prettier 格式化代码

### 常用命令

#### 后端命令

```bash
cd backend

# 开发模式（热重载）
npm run dev

# 生产构建
npm run build

# 启动生产服务
npm start

# 代码检查
npm run lint
```

#### 前端命令

```bash
cd frontend-vue

# 开发模式
npm run dev

# 类型检查
npm run build

# 代码格式化
npm run format

# 代码检查和修复
npm run lint
```

### 添加新功能

1. 创建分支：`git checkout -b feature/your-feature`
2. 在对应的层（api/components/services）添加代码
3. 编写测试（如果需要）
4. 提交代码：`git commit -m 'feat: 添加新功能'`
5. 推送到远程：`git push origin feature/your-feature`
6. 创建 Pull Request

### TurboWarp 自定义

如果需要自定义 TurboWarp 功能，编辑 `scripts/build-turbowarp.sh` 脚本中的构建配置。详细说明请参考 [TurboWarp 集成文档](docs/TURBOWARP_INTEGRATION.md)。

## 📚 相关文档

- [架构设计](docs/ARCHITECTURE.md) - 系统架构详解
- [部署指南](docs/DEPLOYMENT.md) - 详细部署说明
- [TurboWarp 集成](docs/TURBOWARP_INTEGRATION.md) - TurboWarp 集成方案
- [开发报告](docs/DEV_REPORT.md) - 开发历程记录
- [测试文档](docs/TESTING.md) - 测试指南

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: 添加某个特性'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 开发规范

- 代码风格统一
- 添加必要的注释
- 更新相关文档
- 确保通过所有测试

## 📄 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [TurboWarp](https://turbowarp.org/) - 快速的 Scratch 修改版
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Express.js](https://expressjs.com/) - 灵活的 Node.js Web 框架
- [Element Plus](https://element-plus.org/) - Vue 3 UI 组件库
