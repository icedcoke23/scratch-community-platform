# TurboWarp 自托管集成指南

## 概述

本指南说明如何在 scratch-community-platform 项目中自托管 TurboWarp 编辑器。

## 架构说明

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3)                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ScratchEditorView.vue  ← postMessage →          │   │
│  │ ScratchPreview.vue       ScratchBridge.ts         │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              本地 TurboWarp (iframe)                  │
│  /turbowarp/editor-iframe.html                        │
│  /turbowarp/player-iframe.html                        │
│  /turbowarp/static/...                                │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              后端 (Spring Boot)                        │
│  /api/v1/project/{id}/sb3/download (CORS 头)           │
└─────────────────────────────────────────────────────────┘
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `scripts/build-turbowarp.sh` | TurboWarp 构建脚本 |
| `nginx.conf.turbowarp` | 完整的 Nginx 配置 |
| `frontend-vue/src/utils/scratchBridge.ts` | postMessage 通信协议 |
| `frontend-vue/src/utils/turbowarpConfig.ts` | TurboWarp 配置管理 |
| `frontend-vue/src/views/editor/ScratchEditorView.vue` | 编辑器页面 |
| `frontend-vue/src/components/ScratchPreview.vue` | 预览组件 |

## 部署步骤

### 步骤 1: 构建 TurboWarp

```bash
# 进入项目目录
cd scratch-community-platform

# 运行构建脚本
chmod +x scripts/build-turbowarp.sh
./scripts/build-turbowarp.sh
```

这将：
1. 克隆 TurboWarp scratch-gui 仓库
2. 安装依赖
3. 构建生产版本
4. 复制产物到 `frontend-vue/public/turbowarp/`
5. 生成自定义的 `editor-iframe.html` 和 `player-iframe.html`

### 步骤 2: 配置 Nginx

将 `nginx.conf.turbowarp` 的内容合并到现有的 `nginx.conf`：

```bash
# 备份现有配置
cp nginx.conf nginx.conf.backup

# 合并配置（手动或使用工具）
# 确保以下关键配置存在：
# - /turbowarp/ location 指向 /var/www/turbowarp/
# - /api/ 代理到后端
# - CORS 头配置
```

或者直接使用提供的完整配置：

```bash
# 替换 nginx.conf
cp nginx.conf.turbowarp nginx.conf

# 重载 Nginx
nginx -t && nginx -s reload
```

### 步骤 3: 创建 TurboWarp 目录

```bash
# 创建目录
sudo mkdir -p /var/www/turbowarp

# 复制构建产物
sudo cp -r frontend-vue/public/turbowarp/* /var/www/turbowarp/

# 设置权限
sudo chown -R www-data:www-data /var/www/turbowarp
```

### 步骤 4: 验证部署

```bash
# 检查文件是否存在
ls -la /var/www/turbowarp/

# 检查 Nginx 配置
nginx -t

# 测试访问
curl -I http://localhost/turbowarp/
```

## postMessage 通信协议

### 宿主 → TurboWarp

| 消息类型 | 参数 | 说明 |
|---------|------|------|
| `export-project` | - | 请求导出项目 |
| `load-project` | `{ url: string }` | 加载项目 |
| `enter-editor` | - | 切换到编辑模式 |
| `enter-player` | - | 切换到播放模式 |
| `set-fullscreen` | `{ fullscreen: boolean }` | 设置全屏 |

### TurboWarp → 宿主

| 消息类型 | 参数 | 说明 |
|---------|------|------|
| `vm-initialized` | `{ vmId: string }` | VM 初始化完成 |
| `project-changed` | - | 项目已变更 |
| `project-save` | `{ data: string }` | 项目保存完成 (base64) |
| `fullscreen` | `{ fullscreen: boolean }` | 全屏状态变化 |
| `error` | `{ error: string }` | 错误信息 |
| `editor-mode` | - | 进入编辑模式 |
| `player-mode` | - | 进入播放模式 |

## 开发模式

在开发模式下，前端会使用远程 TurboWarp (`https://turbowarp.org`)：

```typescript
// turbowarpConfig.ts
if (isProduction) {
  return {
    mode: 'local',
    editorUrl: '/turbowarp/editor-iframe.html',
    playerUrl: '/turbowarp/player-iframe.html'
  }
} else {
  return {
    mode: 'remote',
    editorUrl: 'https://turbowarp.org/editor',
    playerUrl: 'https://turbowarp.org/'
  }
}
```

## 生产构建

```bash
cd frontend-vue
npm run build

# 复制 Turbowarp 到构建目录
cp -r public/turbowarp dist/

# 部署
sudo cp -r dist/* /var/www/html/
```

## 常见问题

### 1. CORS 错误

如果遇到 CORS 错误，检查：

```bash
# 检查响应头
curl -I http://localhost/api/v1/project/1/sb3/download

# 应该包含：
# Access-Control-Allow-Origin: *
```

### 2. VM 未初始化

确保 postMessage 消息类型正确：

```javascript
// 正确
window.parent.postMessage({ type: 'vm-initialized', vmId: 'vm-1' }, '*')

// 错误 - 缺少 type
window.parent.postMessage({ vmId: 'vm-1' }, '*')
```

### 3. iframe 无法加载

检查 sandbox 属性：

```html
<iframe
  sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
  ...
/>
```

### 4. 项目加载失败

确保项目 URL 正确且支持 CORS：

```bash
# 测试直接下载
curl -o test.sb3 http://localhost/api/v1/project/1/sb3/download

# 检查文件类型
file test.sb3
# 应该是: test.sb3: HTML document 或 Zip archive
```

## 自定义扩展

如果需要加载自定义扩展，可以在构建脚本中添加：

```javascript
// 在 editor-iframe.html 中
const EXTENSION_URL = 'http://localhost:8000/my-extension.js';
vm.extensionManager.loadExtensionURL(EXTENSION_URL);
```

## 性能优化

1. **启用 Gzip 压缩** - Nginx 配置已包含
2. **缓存静态资源** - JS/CSS 文件缓存 30 天
3. **使用 CDN** - 考虑将 Turbowarp 部署到 CDN

## 更新 TurboWarp

```bash
# 删除旧版本
rm -rf scripts/turbowarp

# 重新构建
./scripts/build-turbowarp.sh

# 更新部署
sudo cp -r frontend-vue/public/turbowarp/* /var/www/turbowarp/
```
