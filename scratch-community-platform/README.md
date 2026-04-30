# TurboWarp 自托管集成说明

## 项目介绍

这是一个完整的 TurboWarp 自托管集成方案，支持 Scratch 编辑器和预览功能。

## 项目架构

### 核心设计思路：

1. **本地集成方案 - 使用本地的 `/turbowarp/` 目录下的 wrapper HTML 页面
2. **iframe 转发** - 这些 wrapper 页面实际从 TurboWarp CDN 中加载编辑器
3. **通信协议** - 通过 postMessage 机制
4. **跨域问题** - 通过 wrapper 页面是同源策略绕过

### 目录结构：

```
scratch-community-platform/
├── docs/
│   └── TURBOWARP_INTEGRATION.md
├── frontend-vue/
│   ├── public/
│   │   └── turbowarp/
│   │       ├── editor.html
│   │       ├── player.html
│   │       └── index.html
│   ├── src/
│   │   ├── components/
│   │   │   └── ScratchPreview.vue
│   │   ├── views/
│   │   │   └── editor/
│   │   │       └── ScratchEditorView.vue
│   │   ├── utils/
│   │   │   ├── scratchBridge.ts
│   │   │   └── turbowarpConfig.ts
│   │   ├── App.vue
│   │   ├── main.ts
│   │   └── router/
│   │       └── index.ts
│   ├── package.json
│   └── vite.config.ts
├── nginx.conf
├── scripts/
│   └── build-turbowarp.sh
└── README.md
```

## 部署指南

### 快速开始

#### 1. 构建 TurboWarp
```bash
cd scratch-community-platform/
chmod +x scripts/build-turbowarp.sh
./scripts/build-turbowarp.sh
```

#### 2. 安装前端依赖
```bash
cd frontend-vue/
npm install
```

#### 3. 启动开发服务器
```bash
npm run dev
```

### 生产环境

## 功能特性

1. **编辑器** - Scratch 编辑器集成
2. **预览器** - TurboWarp 播放器
3. **保存/
4. **全屏支持**
5. **沙箱安全

## 集成方案说明

### 本地 wrapper HTML 架构

1. **editor.html** - 编辑器 wrapper
2. **player.html** - 播放器 wrapper
3. **iframe** - TurboWarp 官方 CDN

### 通信协议

```
宿主页面 → (parent page → wrapper → TurboWarp
```

## 后端 API 端点

| 端点 | 用途
| ---- | ----
| `/api/v1/project/:id | 获取项目信息
| `/api/v1/project` | 创建项目
| `/api/v1/project/:id` | 更新项目
| `/api/v1/project/:id/sb3` | 上传/下载 SB3 文件

## 注意事项

### CORS 配置

在 nginx.conf 中必须配置正确的 CORS 头

### 安全

TurboWarp 官方 CDN

## 许可证

MIT
