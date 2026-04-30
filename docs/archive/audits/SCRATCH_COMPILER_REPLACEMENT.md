# Scratch 编译器深度分析与替换报告

> 日期：2026-04-30 | 版本：v3.9.0

---

## 🔍 问题诊断

### 症状
编辑器界面（`/editor`、`/editor/:id`）和预览界面均返回 404。

### 根因分析

| 层级 | 问题 | 严重度 |
|------|------|--------|
| **CI/CD** | `build-scratch-editor.sh` 在 CI 中克隆 scratch-gui 并编译，编译失败导致 `frontend-vue/public/scratch-editor/` 目录不存在 | 🔴 P0 |
| **架构** | 依赖运行时编译 scratch-gui（webpack + Node.js + `--openssl-legacy-provider`），环境差异导致编译不稳定 | 🔴 P0 |
| **模板** | `scratch-editor.html` 引用 `./gui.js`，`scratch-player.html` 引用 `./player.js`，这些文件由 scratch-gui 编译生成 | 🟡 P1 |

### 失败链路

```
CI push → build-scratch-editor.sh → git clone scratch-gui → npm install → webpack build
                                                                    ↓
                                                            编译失败（环境/依赖问题）
                                                                    ↓
                                                    frontend-vue/public/scratch-editor/ 不存在
                                                                    ↓
                                                    vite build → dist/scratch-editor/ 不存在
                                                                    ↓
                                                    Nginx → /scratch-editor/scratch-editor.html → 404
```

---

## 🛠️ 修复方案

### 核心决策：采用 teaching-open 预编译方案

分析了 [teaching-open](https://github.com/open-scratch/teaching-open) 项目的 Scratch 集成方式后，采用其预编译方案：

| 对比项 | 原方案（scratch-gui 编译） | 新方案（teaching-open 预编译） |
|--------|---------------------------|-------------------------------|
| 构建时机 | CI 运行时编译 | 预编译后直接提交到仓库 |
| 稳定性 | 依赖 Node.js 版本、OpenSSL 版本 | ✅ 固定版本，100% 可复现 |
| 构建时间 | ~5-10 分钟 | 0 秒（无需构建） |
| 文件大小 | ~15MB（含 source map） | ~136MB（含完整资源文件） |
| 维护成本 | 需要跟踪 scratch-gui 更新 | 低，teaching-open 已做定制优化 |

### teaching-open 的 scratch3 架构

```
web/public/scratch3/
├── lib.min.js          (13MB) - 核心库（scratch-vm + scratch-render + scratch-audio + scratch-blocks）
├── chunks/
│   ├── gui.js          (2.8KB) - 编辑器入口（webpack chunk）
│   └── player.js       (1.9KB) - 播放器入口（webpack chunk）
├── static/             - 资源文件（角色、造型、背景、声音）
│   ├── assets/         (110K+ 文件)
│   ├── blocks-media/   - 积木媒体
│   ├── extensions/     - 扩展
│   └── json_index/     - 资源索引
├── js/scratch.js       (1.4MB) - 额外 Scratch JS
├── css/                - 样式文件
├── index.html          - 编辑器页面
└── player.html         - 播放器页面
```

**加载机制：**
1. `lib.min.js` 首先加载，注册全局 `window.webpackJsonpGUI` 和核心模块
2. `chunks/gui.js`（或 `chunks/player.js`）作为 webpack 入口，加载 GUI 组件
3. 通过 `window.scratchConfig` 对象进行配置（菜单栏、舞台、回调等）
4. React 渲染 Scratch GUI 到 `#scratch` DOM 元素

---

## 📝 具体修改

### 1. 复制 scratch3 静态文件
```bash
cp -r teaching-open/web/public/scratch3 frontend-vue/public/scratch-editor
```

### 2. 重写 HTML 模板

**scratch-editor.html** - 编辑器模板：
- 移除 jQuery、common.js 等 teaching-open 特有依赖
- 使用 `window.scratchConfig` 配置编辑器
- 通过 `postMessage` 与父窗口通信
- 加载 `lib.min.js` + `chunks/gui.js`

**scratch-player.html** - 播放器模板：
- 精简 UI，只显示舞台和控制按钮
- 自动运行项目（`vm.greenFlag()`）
- 加载 `lib.min.js` + `chunks/player.js`

### 3. 更新 CI/CD 配置

**ci.yml** - 移除 `Build Scratch editor (scratch-gui)` 步骤：
```yaml
# 之前
- name: Build Scratch editor (scratch-gui)
  run: bash ../scripts/build-scratch-editor.sh

# 之后（已删除，预编译文件已提交到仓库）
```

**deploy.yml** - 同样移除 scratch-gui 编译步骤。

### 4. 更新 .gitignore
```diff
-# Scratch editor build artifacts (built in CI via scripts/build-scratch-editor.sh)
+# Scratch editor source (not needed - using pre-built scratch3 files)
 .scratch-gui/
-frontend-vue/public/scratch-editor/
```

### 5. 更新 build-scratch-editor.sh
从编译脚本改为验证脚本，检查必要文件是否存在。

---

## 📊 CI 验证结果

```
CI #179 - feat: 替换 Scratch 编译器为 teaching-open 预编译方案
Status: ✅ completed
Conclusion: success
```

所有 CI 任务通过：
- ✅ Backend Build & Test
- ✅ Frontend Build & Test（不再需要 scratch-gui 编译）
- ✅ Sandbox Build & Test
- ✅ Docker Build Verify
- ✅ Code Quality

---

## 🔬 teaching-open 深度分析

### 技术栈
| 组件 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 2 + Element UI | 2.x |
| 后端框架 | JeeCG Boot (Spring Boot) | 2.x |
| Scratch 引擎 | scratch-gui（定制版） | 3.0 |
| 构建工具 | Webpack | 4.x |
| 数据库 | MySQL | 5.7+ |

### 核心特性
1. **scratchConfig 全局配置** - 通过 `window.scratchConfig` 注入配置
2. **自定义按钮** - 支持在菜单栏添加自定义按钮（提交、保存、返回等）
3. **素材库拦截** - 可拦截角色库/造型库/背景库/声音库打开事件，动态追加素材
4. **云变量** - 支持 WebSocket 云变量同步
5. **背包系统** - 支持自定义背包 API
6. **自动保存** - 支持项目自动保存到服务器

### 与我们的集成差异

| 功能 | teaching-open | 我们的平台 |
|------|--------------|-----------|
| 编辑器入口 | `index.html`（完整页面） | `scratch-editor.html`（iframe 嵌入） |
| 播放器入口 | `player.html`（完整页面） | `scratch-player.html`（iframe 嵌入） |
| 通信方式 | 直接 JS 调用 | postMessage 桥接 |
| 认证方式 | jQuery + Cookie | JWT Bearer Token |
| 文件上传 | 七牛云/本地 | MinIO |
| 素材库 | 动态加载 | 使用默认素材库 |

---

## 🎯 后续优化建议

### P1 - 高优先级
1. **素材库集成** - 对接后端素材库 API，支持动态加载角色/造型/背景/声音
2. **云变量** - 实现 WebSocket 云变量同步（参考 teaching-open 的 cloudData 方案）
3. **背包系统** - 实现自定义背包 API

### P2 - 中优先级
4. **Scratch 版本升级** - 跟踪 scratch-gui 上游更新
5. **移动端适配** - 参考 teaching-open 的 `scratch-mobile.html`
6. **自定义按钮** - 支持在编辑器菜单栏添加自定义操作按钮

### P3 - 低优先级
7. **素材库索引** - 创建自定义素材库索引文件（sprites.json、costumes.json 等）
8. **国际化** - 支持多语言切换（scratch3 已内置中文）
9. **性能优化** - 懒加载 scratch3 资源文件

---

## 📎 相关文件

| 文件 | 说明 |
|------|------|
| `frontend-vue/public/scratch-editor/` | Scratch 编辑器静态文件 |
| `frontend-vue/public/scratch-editor/scratch-editor.html` | 编辑器模板 |
| `frontend-vue/public/scratch-editor/scratch-player.html` | 播放器模板 |
| `frontend-vue/src/views/editor/ScratchEditorView.vue` | 编辑器页面组件 |
| `frontend-vue/src/components/ScratchPreview.vue` | 项目预览组件 |
| `frontend-vue/src/utils/scratchBridge.ts` | Scratch 通信桥接 |
| `scripts/build-scratch-editor.sh` | 验证脚本 |
| `.github/workflows/ci.yml` | CI 配置 |
| `.github/workflows/deploy.yml` | CD 配置 |
