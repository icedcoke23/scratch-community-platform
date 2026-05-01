<template>
  <div class="scratch-editor-page">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          {{ t('common.back') }}
        </el-button>
        <el-divider direction="vertical" />
        <el-input
          v-model="projectTitle"
          class="title-input"
          :placeholder="t('editor.title')"
          :maxlength="50"
          @blur="saveTitle"
        />
        <el-tag v-if="isDirty" type="warning" size="small">{{ t('editor.unsaved') }}</el-tag>
        <el-tag v-else type="success" size="small">{{ t('editor.saved') }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button @click="toggleMode" :type="isEditor ? 'primary' : 'default'">
            <el-icon><Edit /></el-icon>
            {{ isEditor ? t('editor.editMode') : t('editor.previewMode') }}
          </el-button>
          <el-button @click="fullscreen" text>
            <el-icon><FullScreen /></el-icon>
          </el-button>
        </el-button-group>
        <el-button type="primary" @click="saveProject" :loading="saving">
          <el-icon><Check /></el-icon>
          {{ t('editor.save') }}
        </el-button>
        <el-button @click="publishProject" :loading="publishing" v-if="project?.status === 'draft'">
          {{ t('editor.publish') }}
        </el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleSb3Import"
          accept=".sb3"
        >
          <el-button text>
            <el-icon><Upload /></el-icon>
            {{ t('editor.import') }}
          </el-button>
        </el-upload>
        <el-dropdown @command="handleExport">
          <el-button text>
            {{ t('editor.export') }}
            <el-icon><Download /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="sb3">{{ t('editor.exportSb3') }}</el-dropdown-item>
              <el-dropdown-item command="image">{{ t('editor.exportImage') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Scratch 编辑器主体 -->
    <div class="editor-container" ref="editorContainer">
      <!-- 加载状态 -->
      <div v-if="loading" class="editor-loading">
        <div class="loading-content">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p class="loading-text">{{ loadingMessage }}</p>
          <p class="loading-hint">{{ t('editor.loadingHint') }}</p>
          <div class="loading-progress">
            <el-progress :percentage="loadProgress" :stroke-width="4" :show-text="false" />
          </div>
          <div v-if="showRetryButton" class="loading-actions">
            <el-button @click="retryLoad" type="primary" size="small">
              重试加载
            </el-button>
            <el-button @click="openBlankEditor" type="default" size="small">
              打开空白编辑器
            </el-button>
          </div>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="loadError" class="editor-error">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <h3 class="error-title">加载失败</h3>
        <p class="error-message">{{ loadError }}</p>
        <div class="error-actions">
          <el-button @click="retryLoad" type="primary" size="small">
            重试
          </el-button>
          <el-button @click="openBlankEditor" type="default" size="small">
            打开空白编辑器
          </el-button>
          <el-button @click="goBack" type="info" size="small">
            返回
          </el-button>
        </div>
      </div>

      <!-- 编辑器 iframe -->
      <iframe
        v-else
        ref="scratchFrame"
        :src="editorUrl"
        class="scratch-iframe"
        :class="{ 'fullscreen': isFullscreen }"
        @load="onIframeLoad"
        @error="onIframeError"
        allow="clipboard-read; clipboard-write"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms"
      />
    </div>

    <!-- 项目信息面板 -->
    <el-drawer v-model="showInfo" :title="t('editor.projectInfo')" size="360px">
      <el-form label-position="top">
        <el-form-item :label="t('editor.title')">
          <el-input v-model="projectTitle" :maxlength="50" />
        </el-form-item>
        <el-form-item :label="t('editor.description')">
          <el-input v-model="projectDescription" type="textarea" :rows="3" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="t('editor.tags')">
          <el-input v-model="projectTags" :placeholder="t('editor.tagsPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="project">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item :label="t('editor.blockCount')">{{ project.blockCount ?? 0 }}</el-descriptions-item>
            <el-descriptions-item :label="t('editor.complexity')">{{ project.complexityScore ?? 0 }}</el-descriptions-item>
            <el-descriptions-item :label="t('common.status')">
              <el-tag :type="project.status === 'published' ? 'success' : 'info'" size="small">
                {{ project.status === 'published' ? t('editor.published') : t('editor.draft') }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('editor.createdAt')">{{ formatDate(project.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProject" :loading="saving" style="width: 100%">
            {{ t('editor.saveChanges') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Edit, FullScreen, Check, Download, Loading, Upload, WarningFilled
} from '@element-plus/icons-vue'
import { projectApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useI18n } from '@/composables'
import { createScratchBridge, type ScratchBridge } from '@/utils/scratchBridge'
import { createLogger } from '@/utils/logger'
import type { ProjectDetail } from '@/types'

const { t } = useI18n()
const logger = createLogger('Editor')

defineOptions({ name: 'ScratchEditorView' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 状态
const loading = ref(true)
const loadError = ref<string | null>(null)
const loadingMessage = ref('正在加载编辑器...')
const loadProgress = ref(0)
const showRetryButton = ref(false)
const saving = ref(false)
const publishing = ref(false)
const isDirty = ref(false)
const isFullscreen = ref(false)
const showInfo = ref(false)
const isEditor = ref(true)
const autoSaving = ref(false)
const editorReady = ref(false)

// 项目数据
const project = ref<ProjectDetail | null>(null)
const projectTitle = ref('')
const projectDescription = ref('')
const projectTags = ref('')

// DOM 引用
const scratchFrame = ref<HTMLIFrameElement>()
const editorContainer = ref<HTMLDivElement>()

// 自动保存定时器
let autoSaveTimer: ReturnType<typeof setInterval> | null = null
const AUTO_SAVE_INTERVAL = 60000

// 超时定时器
let timeoutTimer: ReturnType<typeof setTimeout> | null = null
const TIMEOUT_MS = 20000 // 20秒

// 加载重试计数
let retryCount = 0
const MAX_RETRIES = 3

// ScratchBridge 实例
let scratchBridge: ScratchBridge | null = null

// 是否新建项目
const isNewProject = computed(() => !route.params.id || route.params.id === 'new')

/**
 * Scratch 编辑器 URL
 *
 * 使用本地部署的 Scratch 编辑器（基于 scratch-gui 构建）。
 * 通过 project_url 参数传递 sb3 文件 URL，由本地 Scratch 编辑器加载。
 *
 * 比 TurboWarp iframe 方案的优势：
 * - 完全自主可控，不依赖外部服务
 * - 可定制 UI（logo/菜单/颜色）
 * - 支持深度集成（云变量/背包）
 * - 同域通信，无需 CORS 处理
 */
const editorUrl = ref('about:blank')

/**
 * 加载项目信息 + 构建编辑器 URL
 */
async function loadProject() {
  // 重置状态
  loading.value = true
  loadError.value = null
  loadingMessage.value = '正在加载编辑器...'
  loadProgress.value = 10
  showRetryButton.value = false

  if (isNewProject.value) {
    // 新建项目：直接加载空白 Scratch 编辑器
    loadingMessage.value = '正在初始化编辑器...'
    loadProgress.value = 30
    editorUrl.value = buildLocalEditorUrl('')
    loadProgress.value = 60
    
    // 模拟进度动画
    simulateProgress()
    return
  }

  try {
    loadingMessage.value = '正在加载项目信息...'
    loadProgress.value = 20
    
    const res = await projectApi.getDetail(Number(route.params.id))
    if (res.code === 0 && res.data) {
      project.value = res.data
      projectTitle.value = res.data.title
      projectDescription.value = res.data.description || ''
      projectTags.value = Array.isArray(res.data.tags) 
        ? res.data.tags.join(',') 
        : res.data.tags || ''

      loadingMessage.value = '正在构建编辑器...'
      loadProgress.value = 50

      // 构建编辑器 URL
      await buildEditorUrl()
      
      loadProgress.value = 80
      simulateProgress()
    } else {
      throw new Error(res.msg || '加载项目失败')
    }
  } catch (e) {
    logger.error('加载项目失败:', e)
    const errorMsg = e instanceof Error ? e.message : '加载项目失败'
    loadError.value = errorMsg
    loading.value = false
  }
}

/**
 * 模拟进度动画
 */
function simulateProgress() {
  let currentProgress = loadProgress.value
  
  const progressInterval = setInterval(() => {
    if (currentProgress >= 95) {
      clearInterval(progressInterval)
      return
    }
    currentProgress += 2
    loadProgress.value = Math.min(currentProgress, 95)
  }, 200)

  // 超时检查
  if (timeoutTimer) clearTimeout(timeoutTimer)
  timeoutTimer = setTimeout(() => {
    if (loading.value) {
      logger.warn('Scratch 加载超时（20s）')
      showRetryButton.value = true
      loadingMessage.value = '加载时间过长...'
    }
  }, TIMEOUT_MS)
}

/**
 * 重试加载
 */
async function retryLoad() {
  retryCount++
  if (retryCount >= MAX_RETRIES) {
    ElMessage.warning('重试次数过多，请尝试打开空白编辑器')
    return
  }
  
  logger.log(`重试加载（第 ${retryCount} 次）`)
  loadProject()
}

/**
 * 打开空白编辑器
 */
function openBlankEditor() {
  logger.log('打开空白编辑器')
  loading.value = true
  loadError.value = null
  loadingMessage.value = '正在打开空白编辑器...'
  loadProgress.value = 50
  editorUrl.value = buildLocalEditorUrl('')
  simulateProgress()
}

/**
 * 构建本地 Scratch 编辑器 URL
 *
 * @param sb3Url sb3 文件的下载地址（相对于后端 API）
 * @returns 完整的编辑器 URL
 */
function buildLocalEditorUrl(sb3Url: string): string {
  const base = `${window.location.origin}/scratch-editor/scratch-editor.html`
  const params = new URLSearchParams()

  if (sb3Url) {
    params.set('project_url', sb3Url)
  }

  if (project.value) {
    params.set('project_id', String(project.value.id))
    if (project.value.title) params.set('project_name', project.value.title)
  }

  const query = params.toString()
  return query ? `${base}?${query}` : base
}

/**
 * 构建编辑器 URL
 */
async function buildEditorUrl() {
  if (!project.value) return

  if (project.value.sb3Url) {
    // 有 sb3 文件：构建带 project_url 的编辑器 URL
    // 使用后端下载端点（Nginx 代理已配置 CORS 头，同域无需 CORS）
    const downloadUrl = `${window.location.origin}/api/v1/project/${project.value.id}/sb3/download`
    editorUrl.value = buildLocalEditorUrl(downloadUrl)
    logger.log('编辑器 URL（带项目）:', editorUrl.value)
  } else {
    // 无 sb3 文件：空白编辑器
    editorUrl.value = buildLocalEditorUrl('')
    logger.log('编辑器 URL（空白项目）')
  }
}

// iframe HTML 加载完成（此时 Scratch VM 可能还未就绪）
function onIframeLoad() {
  logger.log('iframe 加载完成')
  loadingMessage.value = '正在初始化编辑器...'
  loadProgress.value = 90
  setupEditorCommunication()
}

// iframe 加载错误
function onIframeError() {
  logger.error('iframe 加载错误')
  loadError.value = '编辑器加载失败，请检查网络连接'
  loading.value = false
}

// 设置与 Scratch 编辑器的通信
function setupEditorCommunication() {
  scratchBridge?.destroy()

  scratchBridge = createScratchBridge({
    iframe: scratchFrame.value || null,
    onReady() {
      editorReady.value = true
      logger.log('Scratch 编辑器就绪（editor-ready 消息已收到）')
      // 编辑器 HTML 已就绪，但项目可能还在加载中
      loadingMessage.value = '正在初始化虚拟机...'
      loadProgress.value = 98
    },
    onVmReady() {
      logger.log('Scratch VM 初始化完成')
    },
    onProjectChanged() {
      isDirty.value = true
    },
    onProjectSave(base64Data) {
      handleSb3AutoSave(base64Data)
    },
    onProjectLoaded() {
      logger.log('Scratch 项目加载完成（project-loaded 消息已收到）')
      loading.value = false
      loadProgress.value = 100
      clearTimeout(timeoutTimer!)
    },
    onError(error) {
      logger.warn('Scratch 错误:', error)
      // 如果是超时错误，也隐藏 loading
      if (error.includes('超时')) {
        loading.value = false
        loadError.value = '编辑器加载超时，请重试'
      }
    }
  })
  scratchBridge.start()
}

/**
 * 自动保存 sb3 到后端
 */
async function handleSb3AutoSave(base64Data: string) {
  if (!project.value || autoSaving.value) return

  try {
    autoSaving.value = true
    const res = await projectApi.autoSaveSb3(project.value.id, base64Data)
    if (res.code === 0) {
      logger.log('sb3 自动保存成功')
    } else {
      logger.warn('sb3 自动保存失败:', res.msg)
    }
  } catch (e) {
    logger.warn('sb3 自动保存异常:', e)
  } finally {
    autoSaving.value = false
  }
}

/**
 * 请求 Scratch 导出当前项目数据
 */
function requestProjectExport() {
  scratchBridge?.exportProject()
}

// 保存项目
async function saveProject() {
  if (!project.value && isNewProject.value) {
    try {
      saving.value = true
      const res = await projectApi.create({
        title: projectTitle.value || t('editor.importedProject'),
        description: projectDescription.value,
        tags: projectTags.value
      })
      if (res.code === 0 && res.data) {
        project.value = res.data as unknown as ProjectDetail
        ElMessage.success(t('editor.projectCreated'))
        // 替换路由，保留编辑器状态
        router.replace(`/editor/${res.data.id}`)
        // 保存成功后，重新构建编辑器 URL（带上项目 ID）
        await buildEditorUrl()
        if (scratchFrame.value) {
          scratchFrame.value.src = editorUrl.value
        }
      }
    } catch (e) {
      ElMessage.error(t('editor.createFailed'))
    } finally {
      saving.value = false
    }
    return
  }

  if (!project.value) return

  try {
    saving.value = true
    requestProjectExport()
    await projectApi.update(project.value.id, {
      title: projectTitle.value,
      description: projectDescription.value,
      tags: projectTags.value
    })
    isDirty.value = false
    ElMessage.success(t('editor.savedMsg'))
  } catch (e) {
    ElMessage.error(t('editor.saveFailed'))
  } finally {
    saving.value = false
  }
}

// 保存标题（失焦时）
function saveTitle() {
  if (project.value && project.value.title !== projectTitle.value) {
    isDirty.value = true
  }
}

// 发布项目
async function publishProject() {
  if (!project.value) return

  try {
    await ElMessageBox.confirm(t('editor.publishConfirm'), t('editor.publishTitle'))
    publishing.value = true
    requestProjectExport()
    await projectApi.publish(project.value.id)
    project.value.status = 'published'
    ElMessage.success(t('editor.publishedMsg'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('editor.publishFailed'))
  } finally {
    publishing.value = false
  }
}

// 切换编辑/预览模式
function toggleMode() {
  isEditor.value = !isEditor.value
  if (isEditor.value) {
    scratchBridge?.enterEditor()
  } else {
    scratchBridge?.enterPlayer()
  }
}

// 全屏
function fullscreen() {
  if (!editorContainer.value) return
  if (!document.fullscreenElement) {
    editorContainer.value.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

// 导出
function handleExport(command: string) {
  if (command === 'sb3') {
    scratchBridge?.exportProject()
    ElMessage.info(t('editor.exportingSb3'))
  } else if (command === 'image') {
    ElMessage.info(t('editor.imageExportWip'))
  }
}

// 导入 SB3 文件
async function handleSb3Import(file: File) {
  try {
    if (isNewProject.value && !project.value) {
      saving.value = true
      const res = await projectApi.create({
        title: projectTitle.value || t('editor.importedProject'),
        description: projectDescription.value,
        tags: projectTags.value
      })
      if (res.code === 0 && res.data) {
        project.value = res.data as unknown as ProjectDetail
        router.replace(`/editor/${res.data.id}`)
      } else {
        ElMessage.error(t('editor.createFailed'))
        return false
      }
      saving.value = false
    }

    if (!project.value) {
      ElMessage.error(t('editor.projectNotReady'))
      return false
    }

    saving.value = true
    const uploadRes = await projectApi.uploadSb3(project.value.id, file)
    if (uploadRes.code === 0) {
      ElMessage.success(t('editor.importSuccess'))
      // 重新构建编辑器 URL 并刷新 iframe
      await buildEditorUrl()
      if (scratchFrame.value) {
        scratchFrame.value.src = editorUrl.value
      }
      isDirty.value = false
    } else {
      ElMessage.error(uploadRes.msg || t('editor.importFailed'))
    }
  } catch (e) {
    ElMessage.error(t('editor.importSb3Failed'))
  } finally {
    saving.value = false
  }
  return false
}

// 返回
function goBack() {
  if (isDirty.value) {
    ElMessageBox.confirm(t('editor.unsavedLeave'), t('confirm.title'), {
      confirmButtonText: t('editor.leave'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    }).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

// 格式化日期
function formatDate(date?: string) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 监听全屏变化
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// 清理
onBeforeUnmount(() => {
  scratchBridge?.destroy()
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.removeEventListener('keydown', handleKeydown)
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
  if (timeoutTimer) {
    clearTimeout(timeoutTimer)
    timeoutTimer = null
  }
})

// 键盘快捷键
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    saveProject()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    if (project.value?.status === 'draft') publishProject()
  }
  if (e.key === 'F11') {
    e.preventDefault()
    fullscreen()
  }
  if (e.key === 'Escape' && isFullscreen.value) {
    fullscreen()
  }
}

onMounted(() => {
  loadProject()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  document.addEventListener('keydown', handleKeydown)

  if (!isNewProject.value) {
    autoSaveTimer = setInterval(() => {
      if (isDirty.value && project.value) {
        requestProjectExport()
      }
    }, AUTO_SAVE_INTERVAL)
  }
})
</script>

<style scoped lang="less">
.scratch-editor-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  z-index: 10;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .title-input {
    width: 300px;

    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }
  }
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #272822;

  &.fullscreen {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 9999;
  }
}

.scratch-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #f0f0f0;
}

.editor-loading,
.editor-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 100;
}

.loading-content {
  text-align: center;
  color: white;
  max-width: 400px;
  padding: 40px;
}

.loading-icon {
  font-size: 64px;
  animation: rotate 1s linear infinite;
  margin-bottom: 20px;
}

.loading-text {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.loading-hint {
  font-size: 14px;
  opacity: 0.8;
  margin: 0 0 24px 0;
}

.loading-progress {
  width: 100%;
  margin-bottom: 20px;
}

.loading-actions,
.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.error-icon {
  font-size: 64px;
  color: #ffd700;
  margin-bottom: 20px;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.error-message {
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 24px 0;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .editor-toolbar {
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 10px;

    .title-input {
      width: 150px;
    }
  }
}
</style>
