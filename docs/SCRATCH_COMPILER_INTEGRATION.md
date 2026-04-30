# Scratch 编译器集成方案

> 更新时间: 2026-04-30
> 状态: ✅ 已实现

---

## 一、架构变更

### 原方案：TurboWarp iframe 嵌入

```
┌─────────────────────────────────────────┐
│  Vue 3 前端                              │
│  ┌───────────────────────────────────┐  │
│  │  <iframe src="turbowarp.org">     │  │
│  │  外部托管，无法定制 UI             │  │
│  │  依赖外部服务可用性               │  │
│  │  CORS 限制，通信受限              │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### 新方案：本地 Scratch 编译器

```
┌─────────────────────────────────────────────────────┐
│  Vue 3 前端                                          │
│  ┌─────────────────────────────────────────────────┐ │
│  │  <iframe src="/scratch-editor/scratch-editor.html"> │
│  │  本地部署的 scratch-gui 编译器                    │ │
│  │  完全自主可控，可定制 UI                          │ │
│  │  同域通信，无 CORS 限制                           │ │
│  │  支持深度集成（云变量/背包）                      │ │
│  └─────────────────────────────────────────────────┘ │
│                                                       │
│  /scratch-editor/                                     │
│  ├── scratch-editor.html   # 自定义编辑器页面         │
│  ├── scratch-player.html   # 自定义播放器页面         │
│  ├── gui.js               # Scratch GUI 核心 (23MB)  │
│  ├── player.js            # Scratch 播放器 (23MB)    │
│  ├── static/              # Scratch 静态资源          │
│  │   ├── assets/          # 教程图片等                │
│  │   ├── blocks-media/    # 积木图标                  │
│  │   └── extensions/      # 扩展资源                  │
│  └── chunks/              # 代码分割块                │
└─────────────────────────────────────────────────────┘
```

---

## 二、构建流程

### 2.1 构建脚本

```bash
# 手动构建
bash scripts/build-scratch-editor.sh

# CI 自动构建（在 frontend 构建步骤中自动执行）
```

### 2.2 构建流程

1. 克隆 [scratchfoundation/scratch-gui](https://github.com/scratchfoundation/scratch-gui)
2. 安装依赖（`npm install --ignore-scripts`）
3. 生成 stub 文件（microbit hex）
4. 使用 webpack 构建（`NODE_OPTIONS="--openssl-legacy-provider"`）
5. 复制构建产物到 `frontend-vue/public/scratch-editor/`
6. 复制自定义模板页面（`scripts/scratch-editor-templates/`）

### 2.3 自定义模板

| 文件 | 用途 | 特点 |
|------|------|------|
| `scratch-editor.html` | 完整编辑器 | 积木区 + 舞台 + 角色区 |
| `scratch-player.html` | 精简播放器 | 仅舞台 + 控制按钮 |

### 2.4 通信协议

通过 `window.postMessage` 实现宿主与 Scratch 编辑器的双向通信：

**宿主 → Scratch：**
| 消息类型 | 参数 | 说明 |
|---------|------|------|
| `load-project` | `{ url }` | 加载 sb3 项目 |
| `exportProject` | - | 导出当前项目为 sb3 |
| `enter-editor` | - | 切换到编辑模式 |
| `enter-player` | - | 切换到播放模式 |
| `green-flag` | - | 运行项目 |
| `stop-all` | - | 停止项目 |
| `set-project-name` | `{ name }` | 设置项目名称 |
| `set-fullscreen` | `{ fullscreen }` | 全屏控制 |

**Scratch → 宿主：**
| 消息类型 | 数据 | 说明 |
|---------|------|------|
| `editor-ready` | `{}` | 编辑器就绪 |
| `player-ready` | `{}` | 播放器就绪 |
| `vm-initialized` | `{}` | VM 初始化完成 |
| `project-loaded` | `{ url }` | 项目加载完成 |
| `project-changed` | `{}` | 项目内容变更 |
| `project-save` | `{ data }` | 导出的 sb3 数据 (base64) |
| `project-start` | `{}` | 项目开始运行 |
| `project-stop` | `{}` | 项目停止 |

---

## 三、与 TurboWarp 方案的对比

| 维度 | TurboWarp iframe | 本地 Scratch 编译器 |
|------|-----------------|-------------------|
| **UI 定制** | ❌ 不可定制 | ✅ 完全可控 |
| **通信机制** | ⚠️ 有限的 postMessage | ✅ 完整的 postMessage + window.scratch API |
| **云变量** | ❌ 不支持 | ✅ 可扩展 |
| **背包** | ❌ 不支持 | ✅ 可扩展 |
| **依赖外部服务** | ❌ TurboWarp 宕机则不可用 | ✅ 完全自主 |
| **CORS 处理** | ⚠️ 需要 Nginx 代理 | ✅ 同域无需 CORS |
| **文件大小** | ✅ 0（外部加载） | ⚠️ ~70MB（本地部署） |
| **维护成本** | ✅ 自动更新 | ⚠️ 需要跟进 Scratch 版本 |
| **功能完整度** | ✅ TurboWarp 扩展增强 | ✅ 原版 Scratch 3.0 |
| **移动端适配** | ⚠️ 基本响应式 | ✅ 可专门适配 |

---

## 四、文件结构

```
scratch-community-platform/
├── scripts/
│   ├── build-scratch-editor.sh          # 构建脚本
│   └── scratch-editor-templates/        # 自定义模板（git tracked）
│       ├── scratch-editor.html          # 编辑器页面
│       └── scratch-player.html          # 播放器页面
├── frontend-vue/
│   └── public/
│       └── scratch-editor/              # 构建产物（git ignored）
│           ├── scratch-editor.html      # ← 从模板复制
│           ├── scratch-player.html      # ← 从模板复制
│           ├── gui.js                   # Scratch GUI 核心
│           ├── player.js               # Scratch 播放器
│           └── static/                 # 静态资源
└── .scratch-gui/                        # scratch-gui 源码（git ignored）
```

---

## 五、后续扩展计划

### 5.1 短期（v4.0）
- [ ] UI 定制：自定义 logo、菜单栏颜色
- [ ] 积木缩放配置
- [ ] 隐藏特定积木分类

### 5.2 中期（v4.1）
- [ ] WebSocket 云变量支持
- [ ] 背包系统集成
- [ ] 编辑器内嵌作业要求面板

### 5.3 长期（v4.2）
- [ ] 移动端专门适配（scratch-mobile.html）
- [ ] 自定义素材库
- [ ] AI 辅助编程集成
