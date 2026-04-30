# TurboWarp 集成指南

本指南详细说明了 Scratch 社区平台中 TurboWarp 编辑器的集成方案、架构设计和部署步骤。

## 集成方案概述

Scratch 社区平台采用本地 wrapper iframe 方案集成 TurboWarp 编辑器。该方案通过在本地托管 wrapper HTML 页面，解决了跨域通信问题，同时保持了与 TurboWarp 官方编辑器的功能一致性。

### 核心设计思路

1. **本地 wrapper 架构**：使用本地 `/turbowarp/` 目录下的 HTML 页面作为 wrapper，避免直接跨域访问。
2. **iframe 嵌套转发**：wrapper 页面通过 iframe 加载 TurboWarp 官方 CDN 内容，利用同源策略实现安全的 postMessage 通信。
3. **消息桥接机制**：通过 `ScratchBridge` 模块封装 postMessage 通信协议，提供简洁的 API 接口。
4. **配置管理**：统一的 TurboWarp 配置管理模块，支持开发和生产环境切换。

### 方案优势

- **安全性高**：通过 sandbox 属性限制 iframe 权限，防止恶意代码执行。
- **性能优良**：本地静态资源可利用 CDN 加速和浏览器缓存。
- **维护简单**：更新 TurboWarp 只需重新构建，无需修改业务代码。
- **开发友好**：支持开发环境使用远程 TurboWarp，生产环境使用本地版本。

## 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                           用户浏览器                                  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                       Vue 3 前端应用                            │  │
│  │  ┌─────────────────┐    ┌─────────────────┐    ┌───────────┐  │  │
│  │  │ ScratchEditor   │    │ ScratchPreview  │    │  API 层   │  │  │
│  │  │     View       │    │    组件         │    │  (Axios)  │  │  │
│  │  └────────┬────────┘    └────────┬────────┘    └─────┬─────┘  │  │
│  │           │                        │                   │        │  │
│  │           └────────────┬───────────┘                   │        │  │
│  │                        ▼                                │        │  │
│  │            ┌──────────────────────┐                     │        │  │
│  │            │   ScratchBridge      │                     │        │  │
│  │            │   (postMessage 桥接)  │                     │        │  │
│  │            └──────────┬───────────┘                     │        │  │
│  └───────────────────────┼──────────────────────────────────┘        │
│                          ▼                                            │
│  ┌───────────────────────────────────────────────────────────────┐   │
│  │                    iframe 容器                                │   │
│  │  ┌─────────────────┐              ┌─────────────────────────┐ │   │
│  │  │ editor.html     │              │     player.html         │ │   │
│  │  │ (编辑器包装器)   │              │    (播放器包装器)        │ │   │
│  │  └────────┬────────┘              └──────────┬────────────┘ │   │
│  │           │                                      │             │   │
│  │           ▼                                      ▼             │   │
│  │  ┌───────────────────────────────────────────────────────────┐ │   │
│  │  │              TurboWarp CDN (turbowarp.org)                │ │   │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐    │ │   │
│  │  │  │  编辑器界面  │  │  播放器界面  │  │   VM 运行时     │    │ │   │
│  │  │  └─────────────┘  └─────────────┘  └─────────────────┘    │ │   │
│  │  └───────────────────────────────────────────────────────────┘ │   │
│  └───────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Express.js 后端                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  项目管理API  │  │  文件上传API  │  │  文件下载API  │              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘              │
│         │                 │                 │                      │
│         └─────────────────┴─────────────────┘                      │
│                           ▼                                        │
│                  ┌──────────────────┐                              │
│                  │    内存存储        │                              │
│                  │  (In-Memory DB)   │                              │
│                  └──────────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 通信流程

#### 编辑器模式通信流程

```
用户操作 → ScratchEditorView → ScratchBridge → iframe postMessage
                                                          │
                                                          ▼
                                                   editor.html (wrapper)
                                                          │
                                                          ▼
                                                   iframe → TurboWarp CDN
```

#### 项目加载流程

```
1. 用户访问 /editor/:projectId
2. ScratchEditorView 调用 buildEditorUrl() 构建编辑器 URL
3. URL 参数包含 project_url，指向 /api/v1/project/:id/sb3/download
4. iframe 加载 editor.html，通过 postMessage 通知父页面准备就绪
5. ScratchBridge 接收 iframe-ready 事件，触发 onReady 回调
6. 编辑器通过 project_url 参数加载项目数据
7. VM 初始化完成，发送 vm-initialized 消息
8. 父页面接收消息，更新 UI 状态
```

#### 项目保存流程

```
1. 用户点击保存按钮
2. ScratchEditorView 调用 ScratchBridge.saveProject()
3. ScratchBridge 发送 tw-save-project 消息到 iframe
4. TurboWarp VM 导出项目数据
5. 通过 postMessage 发送 save-result 消息
6. ScratchEditorView 接收结果，调用 API 保存到后端
```

## 文件说明

### 目录结构

```
scratch-community-platform/
├── frontend-vue/
│   ├── public/
│   │   └── turbowarp/           # TurboWarp 静态资源目录
│   │       ├── editor.html      # 编辑器 wrapper 页面
│   │       ├── player.html      # 播放器 wrapper 页面
│   │       ├── index.html       # 入口页面
│   │       └── static/          # TurboWarp 编译后的静态文件
│   │           ├── js/
│   │           ├── css/
│   │           └── assets/
│   ├── src/
│   │   ├── components/
│   │   │   └── ScratchPreview.vue    # 项目预览组件
│   │   ├── views/
│   │   │   └── editor/
│   │   │       └── ScratchEditorView.vue   # 编辑器页面
│   │   └── utils/
│   │       ├── scratchBridge.ts      # postMessage 通信桥接
│   │       └── turbowarpConfig.ts     # TurboWarp 配置管理
├── scripts/
│   └── build-turbowarp.sh      # TurboWarp 构建脚本
└── nginx.conf                  # Nginx 配置文件
```

### 核心文件详解

#### ScratchBridge 模块

`scratchBridge.ts` 是整个通信协议的核心实现，提供了以下功能：

| 功能 | 说明 |
|------|------|
| 消息监听 | 自动处理 iframe 发送的各种 postMessage 事件 |
| 消息发送 | 提供简洁的方法发送控制命令到 TurboWarp |
| 状态管理 | 跟踪编辑器模式和播放状态 |
| 回调机制 | 支持灵活的事件回调配置 |
| 生命周期 | 提供 start、stop、destroy 方法管理桥接生命周期 |

#### TurboWarp 配置模块

`turbowarpConfig.ts` 负责管理 TurboWarp 的 URL 配置：

| 配置项 | 说明 |
|--------|------|
| mode | 运行环境模式：local（生产）或 remote（开发） |
| editorUrl | 编辑器 wrapper 页面的 URL |
| playerUrl | 播放器 wrapper 页面的 URL |
| baseUrl | TurboWarp 官方 CDN 的基础 URL |

#### Wrapper 页面

##### editor.html

编辑器 wrapper 页面，主要职责包括：

- 通过 iframe 加载 TurboWarp 编辑器
- 处理 URL 参数中的 project_url
- 转发 postMessage 消息
- 监听 TurboWarp 的 ready 事件并通知父页面

##### player.html

播放器 wrapper 页面，主要职责包括：

- 通过 iframe 加载 TurboWarp 播放器
- 支持项目自动播放
- 处理全屏切换
- 转发播放控制消息

## 部署步骤

### 环境准备

在开始部署之前，请确保系统满足以下要求：

| 要求 | 说明 |
|------|------|
| Node.js | 版本 >= 18.0.0 |
| npm | 版本 >= 9.0.0 |
| Git | 用于克隆和更新代码 |
| 系统内存 | 建议 >= 2GB（构建 TurboWarp 时需要） |
| 磁盘空间 | 建议 >= 5GB 可用空间 |

### 步骤一：构建 TurboWarp

TurboWarp 需要从源码构建，这是集成过程中最关键的一步：

```bash
# 进入项目根目录
cd scratch-community-platform

# 赋予构建脚本执行权限
chmod +x scripts/build-turbowarp.sh

# 执行构建脚本
./scripts/build-turbowarp.sh
```

构建脚本会自动执行以下操作：

1. 检查并克隆 TurboWarp scratch-gui 仓库
2. 安装 Node.js 依赖
3. 执行生产环境构建
4. 复制构建产物到 `frontend-vue/public/turbowarp/` 目录
5. 生成自定义的 editor.html 和 player.html 文件

> 注意：首次构建可能需要较长时间（取决于网络速度和系统性能），请耐心等待。

### 步骤二：安装项目依赖

#### 后端依赖安装

```bash
cd backend
npm install
```

后端依赖包括：

- express：Web 应用框架
- cors：跨域资源共享
- multer：文件上传处理
- TypeScript 相关类型定义

#### 前端依赖安装

```bash
cd frontend-vue
npm install
```

前端依赖包括：

- vue：核心框架
- vue-router：路由管理
- pinia：状态管理
- element-plus：UI 组件库
- axios：HTTP 客户端

### 步骤三：配置环境变量（可选）

如果需要自定义配置，可以创建环境变量文件：

#### 后端环境变量

```bash
# backend/.env
PORT=8080
NODE_ENV=production
CORS_ORIGIN=https://your-domain.com
```

#### 前端环境变量

```bash
# frontend-vue/.env.production
VITE_API_BASE_URL=https://your-domain.com/api
VITE_APP_TITLE=Scratch 社区平台
```

### 步骤四：启动服务

#### 开发环境启动

分别启动后端和前端服务：

```bash
# 终端一：启动后端服务
cd backend
npm run dev

# 终端二：启动前端服务
cd frontend-vue
npm run dev
```

#### 生产环境部署

```bash
# 构建后端
cd backend
npm run build

# 构建前端
cd frontend-vue
npm run build

# 复制 TurboWarp 到构建目录
cp -r public/turbowarp dist/
```

### 步骤五：配置 Nginx

将以下配置添加到 Nginx 配置文件（通常位于 `/etc/nginx/sites-available/default` 或 `/etc/nginx/nginx.conf`）：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 根目录和首页
    root /var/www/scratch-platform/dist;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # TurboWarp 静态资源
    location /turbowarp/ {
        alias /var/www/scratch-platform/turbowarp/;
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # 启用 gzip 压缩
        gzip on;
        gzip_types text/css application/javascript;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # CORS 头（如果后端未配置）
        add_header Access-Control-Allow-Origin *;
        add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
        add_header Access-Control-Allow-Headers "Content-Type, Authorization";
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

配置完成后，验证并重载 Nginx：

```bash
# 验证配置语法
nginx -t

# 重载配置
nginx -s reload
```

### 步骤六：验证部署

部署完成后，通过以下方式验证：

```bash
# 检查 TurboWarp 文件是否存在
ls -la /var/www/scratch-platform/turbowarp/

# 检查 Nginx 配置
nginx -t

# 测试前端访问
curl -I http://localhost/

# 测试 API 访问
curl -I http://localhost/health

# 测试 TurboWarp 资源
curl -I http://localhost/turbowarp/editor.html
```

## postMessage 通信协议

### 协议概述

postMessage 通信协议是前端与 TurboWarp iframe 之间的桥梁，支持双向消息传递。本协议采用消息类型驱动的设计，每条消息都包含 `type` 字段用于标识消息类型。

### 消息格式

所有消息都遵循统一的数据结构：

```typescript
interface BaseMessage {
  type: string;           // 消息类型，必需字段
  [key: string]: any;    // 其他可选字段
}
```

### 宿主 → TurboWarp 消息

这些消息由父页面（Vue 应用）发送到 TurboWarp iframe：

| 消息类型 | 参数 | 说明 | 使用场景 |
|----------|------|------|----------|
| play | - | 开始播放 | 用户点击绿旗按钮 |
| stop | - | 停止播放 | 用户点击停止按钮 |
| pause | - | 暂停播放 | 用户点击暂停按钮 |
| reload | - | 重新加载项目 | 刷新项目内容 |
| tw-save-project | - | 请求保存项目 | 用户点击保存按钮 |
| tw-load-project | { projectUrl: string } | 加载指定项目 | 切换项目 |
| tw-get-state | - | 获取当前状态 | 查询 VM 状态 |
| set-fullscreen | { fullscreen: boolean } | 设置全屏模式 | 切换全屏显示 |
| set-controls | { controls: object } | 设置控制选项 | 修改控制面板配置 |

### TurboWarp → 宿主消息

这些消息由 TurboWarp iframe 发送到父页面：

| 消息类型 | 参数 | 说明 | 使用场景 |
|----------|------|------|----------|
| iframe-ready | { mode: 'editor' | 'player', state?: PlaybackState } | iframe 准备就绪 | 初始化完成 |
| error | { error: string } | 错误信息 | 发生错误时 |
| project-loaded | { projectId?: number | string, projectUrl?: string } | 项目加载完成 | 项目成功加载 |
| project-changed | { projectId?: number | string, hasUnsavedChanges?: boolean } | 项目已变更 | 代码块被修改 |
| playback-state | { state: 'stopped' | 'running' | 'paused' } | 播放状态变化 | 播放控制改变 |
| green-flag | - | 绿旗被点击 | 开始执行脚本 |
| stop-all | - | 全部停止 | 用户点击停止 |
| save-result | { success: boolean, error?: string, projectId?: number | string } | 保存结果 | 保存操作完成 |
| state-result | { state: unknown } | 状态查询结果 | VM 状态查询响应 |
| editor-mode | - | 进入编辑模式 | 切换到编辑器 |
| player-mode | - | 进入播放模式 | 切换到播放器 |

### ScratchBridge 使用示例

```typescript
import { createScratchBridge } from '@/utils/scratchBridge';

// 创建桥接实例
const bridge = createScratchBridge({
  iframe: editorIframe,  // iframe 元素引用
  callbacks: {
    // 初始化完成回调
    onReady: (mode, state) => {
      console.log(`编辑器已就绪，当前模式：${mode}`);
      updateUI(mode, state);
    },
    
    // 错误处理回调
    onError: (error) => {
      ElMessage.error(`编辑器错误：${error}`);
    },
    
    // 项目加载完成回调
    onProjectLoaded: (projectId, projectUrl) => {
      console.log(`项目已加载：${projectId}`);
    },
    
    // 项目变更回调
    onProjectChanged: (projectId, hasUnsavedChanges) => {
      if (hasUnsavedChanges) {
        showUnsavedIndicator();
      }
    },
    
    // 播放状态变化回调
    onPlaybackStateChange: (state) => {
      updatePlayButton(state);
    },
    
    // 绿旗点击回调
    onGreenFlag: () => {
      console.log('开始执行脚本');
    },
    
    // 保存结果回调
    onSaveResult: (success, error, projectId) => {
      if (success) {
        ElMessage.success('项目保存成功');
      } else {
        ElMessage.error(`保存失败：${error}`);
      }
    }
  },
  debug: true  // 开启调试日志
});

// 启动消息监听
bridge.start();

// 发送播放命令
function handlePlay() {
  bridge.play();
}

// 发送停止命令
function handleStop() {
  bridge.stopPlayback();
}

// 保存项目
function handleSave() {
  bridge.saveProject();
}

// 加载新项目
function handleLoadProject(projectUrl) {
  bridge.loadProject(projectUrl);
}

// 组件销毁时清理
onUnmounted(() => {
  bridge.destroy();
});
```

## 常见问题

### 跨域问题

#### 问题描述

浏览器控制台显示跨域错误（CORS error），无法正常加载项目或通信。

#### 解决方案

1. 检查后端 CORS 配置

确保后端正确配置了 CORS 头：

```typescript
// backend/src/app.ts
import cors from 'cors';

app.use(cors({
  origin: '*',  // 生产环境应限制为具体域名
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

2. 检查响应头

```bash
curl -I http://localhost/api/v1/project/1/sb3/download
```

确保响应包含 `Access-Control-Allow-Origin: *`

3. 检查 iframe sandbox 属性

```html
<iframe
  :src="editorUrl"
  sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
  allow="autoplay"
></iframe>
```

### VM 未初始化

#### 问题描述

编辑器显示正常，但无法执行播放操作，提示 VM 未初始化。

#### 原因分析

TurboWarp VM 初始化是异步过程，需要等待 `vm-initialized` 消息后再进行操作。

#### 解决方案

确保正确监听并处理 `iframe-ready` 消息：

```typescript
const bridge = createScratchBridge({
  iframe: editorIframe,
  callbacks: {
    onReady: (mode, state) => {
      // VM 已准备就绪，可以安全地发送命令
      console.log('编辑器已就绪，可以开始操作');
    }
  }
});

// 等待就绪后再执行操作
bridge.start();
```

### 项目加载失败

#### 问题描述

项目无法加载或加载后显示空白。

#### 排查步骤

1. 检查项目 URL 是否正确

```typescript
const projectUrl = buildEditorUrl('/api/v1/project/1/sb3/download');
console.log('Project URL:', projectUrl);
```

2. 验证 SB3 文件可访问

```bash
curl -o test.sb3 http://localhost/api/v1/project/1/sb3/download
file test.sb3  # 应该显示为 Zip archive
```

3. 检查浏览器控制台错误信息

### iframe 无法加载

#### 问题描述

编辑器 iframe 无法正常显示或加载。

#### 解决方案

1. 检查 iframe 元素引用

```typescript
const iframe = document.querySelector('iframe#iframe-editor') as HTMLIFrameElement;
if (!iframe) {
  console.error('找不到 iframe 元素');
}
```

2. 确保 iframe 有正确的 src 属性

```html
<iframe
  ref="editorIframe"
  :src="editorUrl"
  sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
  class="w-full h-full"
></iframe>
```

3. 检查浏览器开发者工具

打开 Network 面板，查看 iframe 资源的加载状态和错误信息。

## 故障排除

### 诊断工具

#### 启用调试日志

```typescript
const bridge = createScratchBridge({
  iframe: editorIframe,
  debug: true  // 开启后会在控制台输出所有消息
});
```

#### 网络请求检查

使用浏览器开发者工具检查以下请求：

| 请求类型 | 检查项 |
|----------|--------|
| editor.html | HTTP 状态码、响应内容 |
| TurboWarp CDN | 是否成功加载 JS/CSS 资源 |
| /api/v1/project/* | SB3 文件下载是否正常 |

### 性能问题

#### 加载缓慢

1. 启用 gzip 压缩

确保 Nginx 配置中启用了 gzip：

```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
gzip_min_length 1000;
```

2. 配置缓存策略

```nginx
location /turbowarp/ {
    expires 30d;
    add_header Cache-Control "public, immutable";
}
```

3. 使用 CDN 加速

考虑将 TurboWarp 静态资源部署到 CDN：

```typescript
// frontend-vue/src/utils/turbowarpConfig.ts
return {
  mode: 'remote',
  editorUrl: 'https://cdn.your-domain.com/editor.html',
  playerUrl: 'https://cdn.your-domain.com/player.html'
};
```

### 内存泄漏

如果长时间使用后出现内存增长，检查以下几点：

1. 确保组件销毁时调用 `bridge.destroy()`
2. 避免在回调中创建闭包导致的循环引用
3. 定期检查浏览器内存使用情况

## 进阶配置

### 自定义 TurboWarp 扩展

如需加载自定义 Scratch 扩展：

```javascript
// 在 editor.html 中添加
const EXTENSION_URL = 'https://your-domain.com/extensions/custom-extension.js';
vm.extensionManager.loadExtensionURL(EXTENSION_URL);
```

### 深色主题

通过 URL 参数启用深色主题：

```typescript
import { buildEditorUrl } from '@/utils/turbowarpConfig';

const darkEditorUrl = buildEditorUrl(projectUrl, {
  theme: 'dark'
});
```

### 性能优化参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| compiler | 启用 JavaScript 编译器 | true |
| fps | 运行帧率 | 60 |
| autoplay | 播放器自动播放 | true |

## 更新 TurboWarp

当需要更新到新版本时：

```bash
# 备份当前版本
cp -r frontend-vue/public/turbowarp frontend-vue/public/turbowarp.bak

# 删除旧版本构建文件
rm -rf frontend-vue/public/turbowarp
rm -rf scripts/turbowarp

# 重新构建
./scripts/build-turbowarp.sh

# 验证新版本
ls -la frontend-vue/public/turbowarp/
```

## 相关资源

- [TurboWarp 官方文档](https://docs.turbowarp.org/)
- [TurboWarp GitHub 仓库](https://github.com/TurboWarp/scratch-gui)
- [postMessage API 文档](https://developer.mozilla.org/zh-CN/docs/Web/API/Window/postMessage)
