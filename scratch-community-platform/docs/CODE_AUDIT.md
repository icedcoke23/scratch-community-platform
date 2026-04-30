# Scratch Community Platform 代码审计报告

**审计日期**: 2026-04-30
**审计范围**: 后端 API、前端 Vue 组件、TurboWarp 集成
**审计版本**: v1.0.0

---

## 一、执行摘要

本次审计对 scratch-community-platform 项目进行了全面的二次代码审计，重点关注后端 API 安全性、前端代码质量、TurboWarp 集成以及整体架构设计。审计共发现 **18 个问题**，其中高严重程度问题 **5 个**，中严重程度问题 **8 个**，低严重程度问题 **5 个**。

### 代码质量评分

| 评估维度 | 评分 (满分 10) | 说明 |
|---------|--------------|------|
| TypeScript 类型安全 | 6.5 | 基础类型定义完善，但存在 `any` 类型滥用 |
| 错误处理 | 7.0 | 前后端错误处理机制较完善，但部分场景遗漏 |
| 安全性 | 5.0 | **严重问题**：CORS、postMessage 存在安全隐患 |
| 代码可维护性 | 7.5 | 组件拆分合理，代码结构清晰 |
| 性能考虑 | 6.5 | 存在同步阻塞操作，缺少缓存策略 |
| API 设计 | 7.0 | RESTful 设计较规范，但存在路由重复 |
| Vue 3 最佳实践 | 8.0 | Composition API 使用规范，TypeScript 集成良好 |

**综合评分**: 6.8 / 10

---

## 二、高严重程度问题 (5 个)

### 问题 1: CORS 配置过于宽松 [高]

**问题描述**:
CORS 中间件允许所有来源访问，设置了 `Access-Control-Allow-Credentials: true`，但允许来源为 `*`，这在现代浏览器中会导致凭证无法生效，且安全性不足。

**文件位置**:
- [requestLogger.ts#L45](file:///workspace/scratch-community-platform/backend/src/middleware/requestLogger.ts#L45)
- [app.ts#L12](file:///workspace/scratch-community-platform/backend/src/app.ts#L12)

```typescript
// requestLogger.ts:L45
res.header('Access-Control-Allow-Origin', process.env.CORS_ORIGIN || '*');

// app.ts:L12
app.use(cors());
```

**严重程度**: 高

**修复建议**:
```typescript
// requestLogger.ts
const allowedOrigins = process.env.CORS_ORIGINS?.split(',') || ['http://localhost:3000'];
res.header('Access-Control-Allow-Origin', allowedOrigins.includes(origin) ? origin : '');

// app.ts
app.use(cors({
  origin: process.env.CORS_ORIGINS?.split(',') || ['http://localhost:3000'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

**预期效果**:
- 防止未经授权的跨域请求
- 明确允许的域名列表，减少攻击面

---

### 问题 2: postMessage 通信缺少来源验证 [高]

**问题描述**:
所有 TurboWarp wrapper 页面在转发 postMessage 时使用 `postMessage(data, '*')`，任何网站都可以接收或伪造消息，存在 XSS 和消息注入风险。

**文件位置**:
- [editor.html#L186](file:///workspace/scratch-community-platform/frontend-vue/public/turbowarp/editor.html#L186)
- [player.html#L191](file:///workspace/scratch-community-platform/frontend-vue/public/turbowarp/player.html#L191)
- [scratchBridge.ts#L238](file:///workspace/scratch-community-platform/frontend-vue/src/utils/scratchBridge.ts#L238)

```typescript
// scratchBridge.ts:L238
this.iframe.contentWindow.postMessage(message, '*')
```

**严重程度**: 高

**修复建议**:
```typescript
// 定义可信来源常量
const TURBOWARP_ORIGIN = 'https://turbowarp.org';

// 发送消息时指定具体来源
this.iframe.contentWindow.postMessage(message, TURBOWARP_ORIGIN);

// 接收消息时验证来源
if (event.origin !== TURBOWARP_ORIGIN) {
  console.warn('[ScratchBridge] Invalid origin:', event.origin);
  return;
}
```

```html
// editor.html
window.parent.postMessage({ type: 'iframe-ready', mode: 'editor' }, 'https://yourdomain.com');
```

**预期效果**:
- 防止恶意网站伪造或窃取 postMessage 消息
- 确保只有来自 TurboWarp 的消息被处理

---

### 问题 3: 数据存储使用内存 Map [高]

**问题描述**:
`ProjectService` 使用 JavaScript Map 作为数据存储，服务重启后所有数据将丢失。这不适合生产环境。

**文件位置**:
- [projectService.ts#L21](file:///workspace/scratch-community-platform/backend/src/services/projectService.ts#L21)

```typescript
private projects: Map<string, Project> = new Map();
```

**严重程度**: 高 (生产环境)

**修复建议**:
```typescript
// 方案 1: 引入数据库 (推荐)
import { PrismaClient } from '@prisma/client';

class ProjectService {
  private prisma = new PrismaClient();

  async createProject(dto: CreateProjectDTO): Promise<Project> {
    return this.prisma.project.create({
      data: {
        title: dto.title,
        description: dto.description,
        tags: dto.tags,
        status: 'draft'
      }
    });
  }
}

// 方案 2: 文件持久化 (简单场景)
import fs from 'fs/promises';

class FileProjectService {
  private filePath = path.join(__dirname, '../../data/projects.json');

  async save(): Promise<void> {
    await fs.writeFile(this.filePath, JSON.stringify([...this.projects.entries()]));
  }
}
```

**预期效果**:
- 数据持久化保存
- 支持服务重启后数据恢复

---

### 问题 4: 缺少请求速率限制 [高]

**问题描述**:
后端 API 没有实现速率限制 (rate limiting)，可能导致恶意用户发起 DDoS 攻击或暴力破解。

**文件位置**: 整体后端应用

**严重程度**: 高

**修复建议**:
```typescript
// 安装: npm install express-rate-limit
import rateLimit from 'express-rate-limit';

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 分钟窗口
  max: 100, // 每次窗口最多 100 个请求
  message: { code: 429, msg: '请求过于频繁，请稍后再试', timestamp: Date.now() }
});

const uploadLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 分钟窗口
  max: 5, // 每次窗口最多 5 次上传
  message: { code: 429, msg: '上传过于频繁', timestamp: Date.now() }
});

// 应用中间件
app.use('/api', apiLimiter);
app.use('/api/v1/project/:id/sb3/upload', uploadLimiter);
```

**预期效果**:
- 防止滥用和暴力攻击
- 提升服务稳定性

---

### 问题 5: 文件上传同步阻塞写入 [高]

**问题描述**:
`projectController.ts` 中使用 `fs.writeFileSync` 同步写入文件，会阻塞事件循环，影响并发处理能力。

**文件位置**:
- [projectController.ts#L149](file:///workspace/scratch-community-platform/backend/src/controllers/projectController.ts#L149)
- [projectController.ts#L207](file:///workspace/scratch-community-platform/backend/src/controllers/projectController.ts#L207)

```typescript
// 同步写入 - 阻塞操作
fs.writeFileSync(destPath, req.file.buffer);
fs.writeFileSync(sb3Path, sb3Data);
```

**严重程度**: 高

**修复建议**:
```typescript
// 使用异步写入
import fs from 'fs/promises';

export const handleUploadSb3 = async (req: Request, res: Response) => {
  try {
    // ... 验证逻辑

    await fs.writeFile(destPath, req.file.buffer);

    // 或者使用流式写入 (大文件推荐)
    const writeStream = fs.createWriteStream(destPath);
    writeStream.write(req.file.buffer);
    await new Promise<void>((resolve, reject) => {
      writeStream.on('finish', resolve);
      writeStream.on('error', reject);
      writeStream.end();
    });

    res.json(successResponse({ sb3Path, filename: req.file.originalname, size: req.file.size }));
  } catch (error) {
    // 错误处理
  }
};
```

**预期效果**:
- 非阻塞操作，提升并发性能
- 大文件上传不会冻结服务器

---

## 三、中严重程度问题 (8 个)

### 问题 6: 路由定义重复 [中]

**问题描述**:
存在两套项目 API 路由定义，一套在 `app.ts` 中，另一套在 `projectRoutes.ts` 中，可能导致路由冲突和维护困难。

**文件位置**:
- [app.ts#L47-72](file:///workspace/scratch-community-platform/backend/src/app.ts#L47-72)
- [projectRoutes.ts#L1-33](file:///workspace/scratch-community-platform/backend/src/routes/projectRoutes.ts#L1-33)

```typescript
// app.ts 中定义的路由
app.post('/api/projects', ...)
app.get('/api/projects', ...)
app.get('/api/projects/:id', ...)

// projectRoutes.ts 中定义的路由
router.post('/', ...)  // /api/v1/project
router.get('/', ...)
router.get('/:id', ...)  // /api/v1/project/:id
```

**严重程度**: 中

**修复建议**:
移除 `app.ts` 中的重复路由定义，统一使用 `projectRoutes.ts`:
```typescript
// app.ts 只保留
app.use('/api/v1/project', projectRoutes);
```

**预期效果**:
- 路由定义统一，减少维护成本
- 避免路由冲突

---

### 问题 7: ScratchBridge.destroy() 方法不完整 [中]

**问题描述**:
`destroy()` 方法没有移除已注册的事件监听器，可能导致内存泄漏和回调在组件卸载后仍被触发。

**文件位置**:
- [scratchBridge.ts#L273-277](file:///workspace/scratch-community-platform/frontend-vue/src/utils/scratchBridge.ts#L273-277)

```typescript
destroy() {
  this.stop()  // 只移除监听器，但没有清理回调
  this.iframe = null
  this.callbacks = {}
}
```

**严重程度**: 中

**修复建议**:
```typescript
destroy() {
  // 停止监听
  this.stop()

  // 清理 iframe 引用
  this.iframe = null

  // 重置状态
  this.ready = false
  this.state = 'stopped'
  this.mode = 'editor'

  // 清理回调 - 使用 Object.keys 确保可枚举属性被清理
  this.callbacks = {
    onReady: undefined,
    onError: undefined,
    onProjectLoaded: undefined,
    onProjectChanged: undefined,
    onPlaybackStateChange: undefined,
    onGreenFlag: undefined,
    onStopAll: undefined,
    onSaveResult: undefined,
    onStateResult: undefined,
    onMessage: undefined
  }

  // 清理 messageHandler 引用
  this.messageHandler = null
}
```

**预期效果**:
- 防止内存泄漏
- 确保组件卸载后不再触发回调

---

### 问题 8: 搜索防抖定时器未正确清理 [中]

**问题描述**:
`ProjectListView.vue` 中搜索防抖使用的 `searchTimer` 没有在组件卸载时清理。

**文件位置**:
- [ProjectListView.vue#L171](file:///workspace/scratch-community-platform/frontend-vue/src/views/ProjectListView.vue#L171)
- [ProjectListView.vue#L202-209](file:///workspace/scratch-community-platform/frontend-vue/src/views/ProjectListView.vue#L202-209)

```typescript
let searchTimer: number | null = null

const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    currentPage.value = 1
    fetchProjects()
  }, 300)
}
// 缺少 onBeforeUnmount 清理
```

**严重程度**: 中

**修复建议**:
```typescript
import { ref, onMounted, onBeforeUnmount } from 'vue'

// ... 其他代码

onBeforeUnmount(() => {
  // 清理搜索定时器
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})
```

**预期效果**:
- 防止组件卸载后定时器仍被执行
- 避免潜在的内存泄漏

---

### 问题 9: ID 生成不安全 [中]

**问题描述**:
使用 `Math.random()` 生成项目 ID，碰撞概率较高，且可预测。不适合生产环境。

**文件位置**:
- [projectService.ts#L23-25](file:///workspace/scratch-community-platform/backend/src/services/projectService.ts#L23-25)

```typescript
private generateId(): string {
  return `project_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}
```

**严重程度**: 中

**修复建议**:
```typescript
// 方案 1: 使用 UUID
import { v4 as uuidv4 } from 'uuid';

private generateId(): string {
  return uuidv4();
}

// 方案 2: 使用 nanoid (更短的 ID)
import { nanoid } from 'nanoid';

private generateId(): string {
  return nanoid();
}
```

**预期效果**:
- 唯一性保证
- 不可预测性

---

### 问题 10: 缺少请求超时和取消机制 [中]

**问题描述**:
前端 API 请求设置了 30 秒超时，但没有提供请求取消机制 (AbortController)。

**文件位置**:
- [index.ts#L5-8](file:///workspace/scratch-community-platform/frontend-vue/src/api/index.ts#L5-8)

**严重程度**: 中

**修复建议**:
```typescript
// 添加请求拦截器支持取消
request.interceptors.request.use((config) => {
  // 生成请求 ID
  config.headers['X-Request-Id'] = crypto.randomUUID();
  return config;
});

// 在需要取消的地方使用
const controller = new AbortController();

async function fetchWithCancel() {
  try {
    const response = await request.get('/api/v1/project', {
      signal: controller.signal
    });
    return response;
  } catch (error) {
    if (axios.isCancel(error)) {
      console.log('请求已取消');
    }
    throw error;
  }
}

// 取消请求
controller.abort();
```

**预期效果**:
- 组件卸载时取消进行中的请求
- 提升用户体验

---

### 问题 11: iframe 加载超时处理不完善 [中]

**问题描述**:
TurboWarp wrapper 页面中的 loadTimeout 在某些错误情况下没有被正确清理。

**文件位置**:
- [editor.html#L253-272](file:///workspace/scratch-community-platform/frontend-vue/public/turbowarp/editor.html#L253-272)

```typescript
function setupIframeLoadHandler() {
  let loadTimeout;

  iframe.addEventListener('load', function() {
    clearTimeout(loadTimeout);
    // ...
  });

  iframe.addEventListener('error', function() {
    clearTimeout(loadTimeout);
    // ...
  });

  // 如果 initialize() 中提前 return，timeout 不会被清理
}
```

**严重程度**: 中

**修复建议**:
```javascript
let loadTimeout = null;

function clearLoadTimeout() {
  if (loadTimeout) {
    clearTimeout(loadTimeout);
    loadTimeout = null;
  }
}

function setupIframeLoadHandler() {
  iframe.addEventListener('load', function() {
    clearLoadTimeout();
    hideLoading();
  });

  iframe.addEventListener('error', function() {
    clearLoadTimeout();
    showError('加载失败', '无法加载 Scratch 编辑器');
  });

  loadTimeout = setTimeout(function() {
    showError('加载超时', '编辑器加载时间过长');
  }, 30000);
}

// 在 initialize 中使用
function initialize() {
  const validatedUrl = validateProjectUrl(projectUrl);
  if (!validatedUrl) {
    clearLoadTimeout(); // 清理超时
    return;
  }
  // ...
}
```

**预期效果**:
- 防止超时回调在错误处理后仍被执行
- 更可靠的资源清理

---

### 问题 12: 错误响应状态码不一致 [中]

**问题描述**:
控制器中部分地方使用自定义 `errorResponse` 返回状态码，部分地方直接使用 `res.status()`，导致响应格式不统一。

**文件位置**:
- [projectController.ts](file:///workspace/scratch-community-platform/backend/src/controllers/projectController.ts)

```typescript
// 不一致的用法
res.status(400).json(errorResponse('Title is required'));  // 状态码 400 重复

res.status(500).json(errorResponse('Failed to create project'));  // 内部错误用 500
```

**严重程度**: 中

**修复建议**:
统一使用 `utils/response.ts` 中的辅助函数:
```typescript
import { sendError } from '../utils/response';

// 替换所有控制器中的错误响应
sendError(res, 'Title is required', 1001, 400);  // code, 自定义码, http状态码
```

**预期效果**:
- 响应格式统一
- 便于前端统一处理

---

### 问题 13: 缺少输入验证在部分端点 [中]

**问题描述**:
虽然有 `projectValidation` 中间件定义，但在路由中没有实际应用。某些端点缺少输入验证。

**文件位置**:
- [projectRoutes.ts](file:///workspace/scratch-community-platform/backend/src/routes/projectRoutes.ts)

```typescript
// 路由定义中缺少验证中间件
router.post('/', handleCreateProject);  // 没有应用 projectValidation.create
router.put('/:id', handleUpdateProject);  // 没有应用 projectValidation.update
```

**严重程度**: 中

**修复建议**:
```typescript
import { projectValidation } from '../middleware/validation';

router.post('/', projectValidation.create, handleCreateProject);
router.get('/:id', projectValidation.getById, handleGetProject);
router.put('/:id', projectValidation.update, handleUpdateProject);
```

**预期效果**:
- 防止无效数据进入业务逻辑
- 统一验证规则

---

## 四、低严重程度问题 (5 个)

### 问题 14: API 响应类型使用 `any` [低]

**问题描述**:
部分 API 方法的响应类型定义为 `any`，丢失了 TypeScript 类型安全。

**文件位置**:
- [index.ts#L82-96](file:///workspace/scratch-community-platform/frontend-vue/src/api/index.ts#L82-96)

```typescript
update(id: number, data: Partial<ProjectDetail>): Promise<ApiResponse<any>> {
  return request.put(`/v1/project/${id}`, data)
}
```

**严重程度**: 低

**修复建议**:
```typescript
interface UpdateProjectResponse {
  id: number
  title: string
  updatedAt: string
}

update(id: number, data: Partial<ProjectDetail>): Promise<ApiResponse<UpdateProjectResponse>> {
  return request.put(`/v1/project/${id}`, data)
}
```

---

### 问题 15: 硬编码的超时时间 [低]

**问题描述**:
多处硬编码超时时间，不便于统一管理和调整。

**文件位置**:
- [app.ts#L13-14](file:///workspace/scratch-community-platform/backend/src/app.ts#L13-14)
- [index.ts#L7](file:///workspace/scratch-community-platform/frontend-vue/src/api/index.ts#L7)

```typescript
// 后端
express.json({ limit: '10mb' })
express.urlencoded({ extended: true, limit: '10mb' })

// 前端
timeout: 30000
```

**修复建议**:
使用配置文件或环境变量:
```typescript
const MAX_BODY_SIZE = process.env.MAX_BODY_SIZE || '10mb';
const REQUEST_TIMEOUT = parseInt(process.env.REQUEST_TIMEOUT || '30000');

app.use(express.json({ limit: MAX_BODY_SIZE }));

const request = axios.create({
  timeout: REQUEST_TIMEOUT
});
```

---

### 问题 16: 缺少日志结构化输出 [低]

**问题描述**:
使用 `console.log/error/warn` 输出日志，生产环境中难以解析和搜索。

**文件位置**: 全局

```typescript
console.error('Create project error:', error);
console.log(logMessage);
```

**修复建议**:
```typescript
import winston from 'winston';

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.json()
  ),
  transports: [
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.simple()
      )
    })
  ]
});

// 替换 console
logger.error('Create project error', { error, context: 'ProjectController' });
```

---

### 问题 17: 组件重复渲染风险 [低]

**问题描述**:
`ScratchPreview.vue` 中某些 watch 可能导致不必要的重复渲染。

**文件位置**:
- [ScratchPreview.vue#L462-473](file:///workspace/scratch-community-platform/frontend-vue/src/components/ScratchPreview.vue#L462-473)

```typescript
watch(() => props.projectId, (newId) => {
  if (newId && props.autoLoad) {
    handleLoadPreview()
  } else if (!newId) {
    loaded.value = false
    // 销毁 bridge
  }
})
```

**修复建议**:
添加 `watchEffect` 替代或使用 shallowRef:
```typescript
watch(() => props.projectId, (newId, oldId) => {
  if (newId === oldId) return  // 避免相同值触发
  // ...
})
```

---

### 问题 18: 缺少 API 版本管理策略 [低]

**问题描述**:
API 路由使用 `/v1` 版本，但缺少版本废弃和迁移策略文档。

**文件位置**:
- [projectRoutes.ts](file:///workspace/scratch-community-platform/backend/src/routes/projectRoutes.ts)

**修复建议**:
创建 `docs/API_VERSIONING.md` 文档:
```markdown
# API 版本管理策略

## 当前版本
- v1 (稳定)

## 版本迁移规则
1. 旧版本至少维护 6 个月
2. 废弃前 3 个月发布弃用警告
3. 重大变更只在新版本中引入
```

---

## 五、最佳实践建议

### 1. TypeScript 最佳实践

```typescript
// 避免使用 any，使用 unknown 代替
function parseResponse(data: unknown): ApiResponse<unknown> {
  if (typeof data !== 'object' || data === null) {
    throw new Error('Invalid response format');
  }
  return data as ApiResponse<unknown>;
}

// 使用 const 断言固定类型
const MESSAGES = {
  READY: 'tw-editor-ready',
  ERROR: 'tw-editor-error'
} as const;

type MessageType = typeof MESSAGES[keyof typeof MESSAGES];
```

### 2. 错误处理最佳实践

```typescript
// 统一错误处理
class ErrorHandler {
  static handleControllerError(error: Error, res: Response, context: string) {
    logger.error(`${context} error`, { error: error.message, stack: error.stack });

    if (error instanceof AppError) {
      return sendError(res, error.message, error.code, error.statusCode);
    }

    return sendError(res, 'Internal server error', 500, 500);
  }
}

// 在控制器中使用
export const handleCreateProject = async (req: Request, res: Response) => {
  try {
    // 业务逻辑
  } catch (error) {
    ErrorHandler.handleControllerError(error, res, 'handleCreateProject');
  }
};
```

### 3. 前端状态管理最佳实践

```typescript
// 使用 Pinia 进行状态管理
import { defineStore } from 'pinia';

export const useProjectStore = defineStore('project', {
  state: () => ({
    currentProject: null as ProjectDetail | null,
    loading: false,
    error: null as string | null
  }),

  actions: {
    async loadProject(id: number) {
      this.loading = true;
      this.error = null;
      try {
        const response = await projectApi.getDetail(id);
        if (response.code === 0) {
          this.currentProject = response.data;
        } else {
          this.error = response.msg;
        }
      } catch (e) {
        this.error = 'Failed to load project';
      } finally {
        this.loading = false;
      }
    }
  }
});
```

---

## 六、风险评估

### 安全风险

| 风险项 | 风险等级 | 缓解措施 |
|-------|---------|---------|
| CORS 过度宽松 | 高 | 限制允许的域名列表 |
| postMessage XSS | 高 | 验证消息来源 |
| 缺少速率限制 | 高 | 实施 rate limiting |
| 输入验证不足 | 中 | 应用验证中间件 |
| 敏感信息日志 | 低 | 使用结构化日志 |

### 性能风险

| 风险项 | 风险等级 | 缓解措施 |
|-------|---------|---------|
| 同步文件写入 | 高 | 改为异步/流式写入 |
| 内存数据存储 | 高 | 引入数据库 |
| 无缓存策略 | 中 | 添加 Redis 缓存 |
| 缺少分页限制 | 中 | 强制分页参数 |

### 可靠性风险

| 风险项 | 风险等级 | 缓解措施 |
|-------|---------|---------|
| 服务重启数据丢失 | 高 | 数据持久化 |
| 外部依赖无降级 | 中 | 添加降级方案 |
| 缺少健康检查详情 | 低 | 完善健康检查端点 |

---

## 七、总结与建议优先级

### 立即修复 (1 周内)

1. CORS 配置限制 - 问题 1
2. postMessage 来源验证 - 问题 2
3. 添加速率限制 - 问题 4
4. 文件上传异步处理 - 问题 5

### 短期修复 (1 个月内)

5. 引入数据库存储 - 问题 3
6. 修复路由重复定义 - 问题 6
7. 完善 ScratchBridge.destroy() - 问题 7
8. 添加输入验证中间件 - 问题 13

### 中期优化 (2-3 个月)

9. 搜索防抖清理 - 问题 8
10. ID 生成器升级 - 问题 9
11. 请求取消机制 - 问题 10
12. iframe 超时处理 - 问题 11

### 长期改进

13. 日志系统结构化
14. 缓存策略实施
15. API 版本管理策略

---

## 附录

### A. 审计文件清单

**后端文件**:
- `backend/src/app.ts` - Express 应用配置
- `backend/src/index.ts` - 入口文件
- `backend/src/controllers/projectController.ts` - 项目控制器
- `backend/src/services/projectService.ts` - 项目服务
- `backend/src/routes/projectRoutes.ts` - 项目路由
- `backend/src/middleware/*.ts` - 中间件
- `backend/src/types/*.ts` - 类型定义

**前端文件**:
- `frontend-vue/src/views/editor/ScratchEditorView.vue` - 编辑器页面
- `frontend-vue/src/components/ScratchPreview.vue` - 预览组件
- `frontend-vue/src/views/ProjectListView.vue` - 项目列表
- `frontend-vue/src/api/index.ts` - API 调用
- `frontend-vue/src/utils/scratchBridge.ts` - 通信桥接
- `frontend-vue/src/utils/turbowarpConfig.ts` - TurboWarp 配置

**TurboWarp 集成**:
- `frontend-vue/public/turbowarp/editor.html` - 编辑器 wrapper
- `frontend-vue/public/turbowarp/player.html` - 播放器 wrapper

### B. 评分标准说明

评分采用 10 分制:
- 9-10: 优秀，符合行业最佳实践
- 7-8: 良好，有少量改进空间
- 5-6: 中等，存在明显问题需要修复
- 3-4: 较差，需要大规模重构
- 1-2: 很差，存在严重问题

---

**审计完成时间**: 2026-04-30
**审计人员**: Code Audit Team
**版本**: 1.0.0
