# Scratch 集成方案深度分析

> 分析时间: 2026-04-24
> 对比项目: open-scratch/teaching-open vs scratch-community-platform

---

## 一、teaching-open 的 Scratch 集成方式

### 1.1 架构概览

```
┌─────────────────────────────────────────────────┐
│  Vue 前端 (Ant Design Vue)                       │
│  ┌─────────────────────────────────────────────┐ │
│  │  /scratch3/index.html  ← 独立 HTML 页面     │ │
│  │  ┌─────────────────────────────────────┐    │ │
│  │  │  lib.min.js (13MB)                  │    │ │
│  │  │  = scratch-vm + scratch-gui +       │    │ │
│  │  │    scratch-render + scratch-audio   │    │ │
│  │  │    + scratch-storage + scratch-parser│   │ │
│  │  │  预编译打包，非源码 fork              │    │ │
│  │  └─────────────────────────────────────┘    │ │
│  │  scratch.js (1.45MB) ← 自定义封装层         │ │
│  │  window.scratchConfig ← 配置驱动            │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  后端 (Spring Boot / JeecgBoot)                  │
│  ├── WebSocket: /websocket/scratch/cloudData     │
│  ├── REST: /api/teaching/scratch/backpack        │
│  └── 文件存储: 七牛云 (qiniu)                    │
└─────────────────────────────────────────────────┘
```

### 1.2 核心集成方式：预编译分发包

**不是 fork scratch-gui 源码**，而是使用预编译的 Scratch 3.0 分发包：

| 文件 | 大小 | 作用 |
|------|------|------|
| `lib.min.js` | 13MB | Scratch 全量库（VM + GUI + Render + Audio） |
| `scratch.js` | 1.45MB | 自定义封装层（UI 定制、通信接口） |
| `chunks/gui.js` | ~2MB | Scratch GUI 组件 |
| `chunks/player.js` | ~500KB | 播放器模式 |
| `index.html` | 14KB | 编辑器页面 |
| `player.html` | 2.6KB | 播放器页面 |
| `scratch-mobile.html` | 8.4KB | 移动端适配 |

### 1.3 配置驱动集成（window.scratchConfig）

```javascript
window.scratchConfig = {
  session: {
    token: userToken,        // 用户认证
    username: userInfo.realname,
  },
  backpack: {
    enable: true,
    api: "/api/teaching/scratch/backpack",  // 背包 API
  },
  cloudData: {
    enable: true,
    id: "create",            // 云变量 ID
    api: "/api/websocket/scratch/cloudData" // WebSocket
  },
  projectInfo: {
    projectName: "",
    authorUsername: userInfo.username,
    authorAvatar: './static/avatar.png',
  },
  logo: {
    show: true,
    url: window.getLogo(),
    handleClickLogo: () => { window.open("/") }
  },
  stageArea: {
    fullscreenButton: { show: true },
    startButton: { show: true },
    stopButton: { show: true }
  },
  menuBar: { /* 菜单栏样式定制 */ },
  handleVmInitialized: (vm) => { window.vm = vm },
  handleDefaultProjectLoaded: () => { /* 项目加载完成 */ }
}
```

### 1.4 宿主通信机制

| 通信方式 | 方向 | 用途 |
|---------|------|------|
| `window.scratch.loadProject(url, cb)` | 宿主 → Scratch | 加载项目 |
| `window.scratch.setCloudId(id)` | 宿主 → Scratch | 设置云变量 ID |
| `window.scratch.setFullScreen(bool)` | 宿主 → Scratch | 全屏控制 |
| `CustomEvent('scratchFullScreen')` | Scratch → 宿主 | 全屏事件通知 |
| `CustomEvent('scratchUnFullScreen')` | Scratch → 宿主 | 退出全屏事件 |
| URL 参数 (`workId`, `workFile`, `scene`) | 宿主 → Scratch | 初始配置 |
| `handleVmInitialized(vm)` | Scratch → 宿主 | VM 初始化完成回调 |
| `handleBeforeStart/Stop()` | 宿主拦截 | 播放/停止前钩子 |

### 1.5 项目加载流程

```
1. 用户点击"编辑" → 打开 /scratch3/index.html?workId=xxx&scene=create
2. index.html 读取 URL 参数，获取 userToken、workId 等
3. 设置 window.scratchConfig（包含用户信息、API 地址等）
4. 加载 lib.min.js（13MB Scratch 全量库）
5. scratch.js 根据 scratchConfig 初始化编辑器
6. handleDefaultProjectLoaded 回调触发
7. 调用 window.scratch.loadProject(workFileUrl) 加载 SB3 文件
8. VM 反序列化项目，渲染到舞台
```

### 1.6 后端集成

**WebSocket 云变量**：
```java
@ServerEndpoint("/websocket/scratch/cloudData")
public class ScratchWebSocket {
    // 管理 Scratch 云变量的实时同步
    // 使用 Redis 存储云变量值
    // 支持多用户同时编辑同一项目的云变量
}
```

**背包 API**：
```java
// /api/teaching/scratch/backpack
// 存储用户的积木、角色、造型等背包数据
// 支持跨项目复用
```

**文件存储**：
- 使用七牛云（Qiniu）存储 SB3 文件和封面图
- 上传流程：前端直传七牛 → 回调后端记录文件信息

---

## 二、我们项目的 Scratch 集成方式

### 2.1 当前方案：TurboWarp iframe 嵌入

```
┌─────────────────────────────────────────────────┐
│  Vue 3 前端 (Element Plus)                       │
│  ┌─────────────────────────────────────────────┐ │
│  │  ScratchEditorView.vue                      │ │
│  │  ┌─────────────────────────────────────┐    │ │
│  │  │  <iframe src="turbowarp.org/editor">│    │ │
│  │  │  外部托管，无法定制 UI               │    │ │
│  │  │  postMessage 通信（有限）            │    │ │
│  │  └─────────────────────────────────────┘    │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  后端 (Spring Boot 3.2)                          │
│  ├── REST: /api/project/* (项目 CRUD)            │
│  ├── MinIO: SB3 文件存储                         │
│  └── 无 WebSocket、无云变量                      │
└─────────────────────────────────────────────────┘
```

### 2.2 对比分析

| 维度 | teaching-open | 我们的项目 | 差距 |
|------|--------------|-----------|------|
| **集成方式** | 预编译分发包（本地部署） | TurboWarp iframe（外部托管） | 🔴 核心差距 |
| **UI 定制** | 完全可控（logo/菜单/按钮/颜色） | 不可定制（TurboWarp 固定 UI） | 🔴 |
| **通信机制** | `window.scratch.*` API + 回调 + CustomEvent | `postMessage`（有限） | 🔴 |
| **云变量** | WebSocket + Redis 实时同步 | 无 | 🔴 |
| **背包** | 后端 API + 数据库 | 无 | 🟡 |
| **项目加载** | 直接 URL 加载 + 回调 | 需要手动处理 | 🟡 |
| **移动端** | 专门的 `scratch-mobile.html` | 响应式但无专门适配 | 🟡 |
| **文件大小** | 13MB（本地加载，快） | 0（外部加载，依赖网络） | 🟢 我们更小 |
| **维护成本** | 高（需要跟进 Scratch 版本） | 低（TurboWarp 自动更新） | 🟢 我们更低 |
| **部署复杂度** | 静态文件部署 | 无需部署 | 🟢 我们更简单 |

---

## 三、深度分析与建议

### 3.1 当前方案的优势

1. **零维护成本** — TurboWarp 自动更新，不需要跟进 Scratch 版本
2. **零部署开销** — 不需要部署 13MB 的静态文件
3. **快速上线** — iframe 嵌入几分钟搞定
4. **功能完整** — TurboWarp 比原版 Scratch 功能更强（性能优化、扩展支持）

### 3.2 当前方案的劣势

1. **无法定制 UI** — 不能修改 logo、菜单、按钮、颜色主题
2. **通信受限** — postMessage 只能做基本的模式切换和导出
3. **无云变量** — 不支持 Scratch 云变量（多人实时协作的基础）
4. **无背包** — 不能跨项目复用积木和角色
5. **依赖外部服务** — TurboWarp 宕机则编辑器不可用
6. **无法深度集成** — 不能在 Scratch 编辑器内显示作业要求、评分等

### 3.3 推荐方案：渐进式升级

```
阶段 1（当前）：TurboWarp iframe 嵌入
  ↓ 快速上线，验证产品需求
  
阶段 2（短期）：自建 Scratch 分发包
  ↓ 使用 scratch-vm + scratch-gui 预编译部署
  ↓ 实现 window.scratchConfig 配置驱动
  ↓ 实现基本 UI 定制（logo/颜色/菜单）
  
阶段 3（中期）：深度集成
  ↓ WebSocket 云变量
  ↓ 背包系统
  ↓ 编辑器内嵌作业要求/评分
  ↓ 移动端专门适配
```

### 3.4 阶段 2 实现方案

**技术选型**：
```bash
# 方案 A：使用 TurboWarp 源码构建（推荐）
git clone https://github.com/TurboWarp/scratch-gui.git
cd scratch-gui
npm install
# 修改 src/ 下的组件定制 UI
npm run build
# 部署 build/ 到 web/public/scratch3/

# 方案 B：使用原版 scratch-gui 构建
git clone https://github.com/scratchfoundation/scratch-gui.git
cd scratch-gui
npm install
npm run build
```

**目录结构**：
```
frontend-vue/public/scratch3/
├── index.html          # 编辑器页面
├── player.html         # 播放器页面
├── static/             # Scratch 静态资源
│   ├── js/
│   ├── css/
│   └── media/
└── lib.min.js          # 构建后的全量库
```

**Vue 集成**：
```vue
<template>
  <div class="scratch-editor">
    <iframe
      :src="editorUrl"
      @load="onEditorLoad"
      ref="editorFrame"
    />
  </div>
</template>

<script setup>
// 通过 URL 参数传递配置
const editorUrl = computed(() => {
  const params = new URLSearchParams({
    workId: props.projectId,
    scene: props.mode,        // 'create' | 'play'
    token: userStore.token,
  })
  return `/scratch3/index.html?${params}`
})

// 监听 Scratch 事件
window.addEventListener('message', (e) => {
  if (e.data.type === 'scratchFullScreen') { /* ... */ }
  if (e.data.type === 'projectSaved') { /* ... */ }
})
</script>
```

---

## 四、结论

| 方案 | 适用阶段 | 推荐度 |
|------|---------|--------|
| TurboWarp iframe | MVP / 快速验证 | ⭐⭐⭐⭐ |
| 预编译分发包 | 产品化 / 教学平台 | ⭐⭐⭐⭐⭐ |
| fork scratch-gui 源码 | 深度定制 / 商业产品 | ⭐⭐⭐ |
| 完全自研编辑器 | 不推荐 | ⭐ |

**当前建议**：保持 TurboWarp iframe 方案作为快速迭代的主力，同时启动阶段 2 的预编译分发包方案，用于需要深度集成的场景（如作业编辑、竞赛答题）。
