<template>
  <div class="scratch-editor-page">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="handleGoBack" text :disabled="navigating">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <el-input
          v-model="projectTitle"
          class="title-input"
          placeholder="输入项目标题..."
          maxlength="50"
          :disabled="saving || publishing"
          @blur="handleTitleBlur"
          @keydown.enter="handleTitleBlur"
          clearable
        />
        <el-tag
          v-if="project"
          :type="project.status === 'published' ? 'success' : 'warning'"
          class="status-tag"
        >
          {{ project.status === 'published' ? '已发布' : '草稿' }}
        </el-tag>
        <el-tag v-else type="info" class="status-tag">未保存</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-tooltip content="全屏模式" placement="bottom">
            <el-button @click="toggleFullscreen" :disabled="loading">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button
          type="primary"
          @click="handleSaveProject"
          :loading="saving"
          :disabled="loading"
        >
          <template #icon>
            <el-icon><Check /></el-icon>
          </template>
          {{ isNewProject && !project ? '创建' : '保存' }}
        </el-button>
        <el-button
          v-if="project?.status === 'draft'"
          type="success"
          @click="handlePublishProject"
          :loading="publishing"
          :disabled="loading"
        >
          <template #icon>
            <el-icon><Promotion /></el-icon>
          </template>
          发布
        </el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleSb3Upload"
          :disabled="loading || uploading"
          accept=".sb3"
        >
          <el-button :loading="uploading" :disabled="loading">
            <template #icon>
              <el-icon><Upload /></el-icon>
            </template>
            导入 SB3
          </el-button>
        </el-upload>
        <el-button
          @click="handleExportSb3"
          :disabled="!project || loading"
          title="导出为 SB3 文件"
        >
          <template #icon>
            <el-icon><Download /></el-icon>
          </template>
          导出
        </el-button>
      </div>
    </div>

    <div class="editor-container" ref="editorContainer">
      <div v-if="loading" class="editor-loading">
        <el-icon class="loading-icon is-loading"><Loading /></el-icon>
        <p class="loading-text">{{ loadingMessage }}</p>
        <p v-if="loadingHint" class="loading-hint">{{ loadingHint }}</p>
      </div>

      <div v-if="error && !loading" class="editor-error">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <p class="error-text">{{ error }}</p>
        <el-button type="primary" @click="handleRetry">
          重试
        </el-button>
        <el-button @click="handleGoBack">
          返回
        </el-button>
      </div>

      <iframe
        v-show="!loading && !error"
        ref="scratchFrame"
        :src="editorUrl"
        class="scratch-iframe"
        :class="{ 'fullscreen': isFullscreen }"
        @load="handleIframeLoad"
        @error="handleIframeError"
        allow="clipboard-read; clipboard-write"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms allow-downloads allow-modals"
      />
    </div>

    <div class="editor-footer">
      <div class="footer-left">
        <span class="shortcut-hint">
          <kbd>Ctrl</kbd> + <kbd>S</kbd> 保存
        </span>
      </div>
      <div class="footer-right">
        <span v-if="lastSaved" class="last-saved">
          最后保存: {{ formatTime(lastSaved) }}
        </span>
        <span class="bridge-status">
          编辑器: {{ bridgeReady ? '就绪' : '连接中...' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  FullScreen,
  Check,
  Promotion,
  Upload,
  Download,
  Loading,
  WarningFilled
} from '@element-plus/icons-vue'
import { projectApi, type ProjectDetail } from '@/api'
import { createScratchBridge, type EditorMode, type PlaybackState } from '@/utils/scratchBridge'
import { buildEditorUrl, getDownloadUrl } from '@/utils/turbowarpConfig'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const uploading = ref(false)
const navigating = ref(false)
const isFullscreen = ref(false)
const bridgeReady = ref(false)

const project = ref<ProjectDetail | null>(null)
const projectTitle = ref('')
const error = ref('')
const lastSaved = ref<Date | null>(null)

const scratchFrame = ref<HTMLIFrameElement | null>(null)
const editorContainer = ref<HTMLDivElement | null>(null)
const bridge = ref<ReturnType<typeof createScratchBridge> | null>(null)

const loadingMessage = ref('正在初始化编辑器...')
const loadingHint = ref('首次加载可能需要几秒钟')

const isNewProject = computed(() => !route.params.id || route.params.id === 'new')

const projectId = computed(() => {
  if (route.params.id && route.params.id !== 'new') {
    return Number(route.params.id)
  }
  return null
})

const editorUrl = computed(() => {
  if (isNewProject.value) {
    return buildEditorUrl()
  }
  if (projectId.value && project.value?.sb3Url) {
    return buildEditorUrl(project.value.sb3Url)
  }
  if (projectId.value) {
    const downloadUrl = getDownloadUrl(projectId.value)
    return buildEditorUrl(downloadUrl)
  }
  return buildEditorUrl()
})

function formatTime(date: Date): string {
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

async function createNewProject(): Promise<number | null> {
  try {
    saving.value = true
    const res = await projectApi.create({
      title: projectTitle.value || '新项目',
      description: ''
    })

    if (res.code === 0 && res.data) {
      const newProject = res.data as unknown as ProjectDetail
      project.value = newProject
      lastSaved.value = new Date()
      ElMessage.success('项目创建成功')
      return newProject.id
    } else {
      ElMessage.error(res.msg || '创建项目失败')
      return null
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '创建项目失败，请检查网络连接')
    return null
  } finally {
    saving.value = false
  }
}

async function loadProject(id: number): Promise<void> {
  try {
    loading.value = true
    error.value = ''
    loadingMessage.value = '正在加载项目...'
    loadingHint.value = ''

    const res = await projectApi.getDetail(id)

    if (res.code === 0 && res.data) {
      project.value = res.data
      projectTitle.value = res.data.title || '未命名项目'
      loadingHint.value = `项目包含 ${res.data.blockCount || 0} 个积木`
    } else {
      error.value = res.msg || '加载项目失败'
      ElMessage.error(error.value)
    }
  } catch (e: any) {
    error.value = e?.message || '加载项目失败，请检查网络连接'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

async function handleSaveProject(): Promise<void> {
  if (!project.value && !isNewProject.value) {
    ElMessage.warning('请先创建或加载项目')
    return
  }

  if (isNewProject.value && !project.value) {
    const newId = await createNewProject()
    if (newId) {
      router.replace(`/editor/${newId}`)
    }
    return
  }

  if (!project.value) return

  try {
    saving.value = true
    const res = await projectApi.update(project.value.id, {
      title: projectTitle.value,
      description: ''
    })

    if (res.code === 0) {
      lastSaved.value = new Date()
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败，请检查网络连接')
  } finally {
    saving.value = false
  }
}

async function handlePublishProject(): Promise<void> {
  if (!project.value) {
    ElMessage.warning('请先保存项目')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要发布项目「${projectTitle.value}」吗？\n发布后项目将对所有用户可见。`,
      '发布确认',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'info',
        confirmButtonClass: 'el-button--success'
      }
    )

    publishing.value = true
    const res = await projectApi.publish(project.value.id)

    if (res.code === 0) {
      project.value.status = 'published'
      lastSaved.value = new Date()
      ElMessage.success('发布成功！您的项目现在对所有人可见')
    } else {
      ElMessage.error(res.msg || '发布失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '发布失败，请检查网络连接')
    }
  } finally {
    publishing.value = false
  }
}

async function handleSb3Upload(file: File): Promise<boolean> {
  if (!file.name.endsWith('.sb3')) {
    ElMessage.warning('请上传 .sb3 格式的文件')
    return false
  }

  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 50MB')
    return false
  }

  if (isNewProject.value && !project.value) {
    const newId = await createNewProject()
    if (!newId) {
      return false
    }
    router.replace(`/editor/${newId}`)
  }

  if (!project.value) {
    ElMessage.error('项目尚未准备好，请稍后重试')
    return false
  }

  try {
    uploading.value = true
    const res = await projectApi.uploadSb3(project.value.id, file)

    if (res.code === 0) {
      ElMessage.success('SB3 文件导入成功，正在刷新编辑器...')
      setTimeout(() => {
        router.go(0)
      }, 1000)
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败，请检查网络连接')
  } finally {
    uploading.value = false
  }

  return false
}

function handleExportSb3(): void {
  if (!project.value) return

  const downloadUrl = `/api/v1/project/${project.value.id}/sb3/download`
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = `${projectTitle.value || 'project'}.sb3`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  ElMessage.success('开始下载 SB3 文件')
}

function handleIframeLoad(): Promise<void> {
  return new Promise((resolve) => {
    loading.value = false
    loadingMessage.value = '编辑器加载完成'
    loadingHint.value = ''

    bridge.value = createScratchBridge({
      iframe: scratchFrame.value,
      debug: true,
      callbacks: {
        onReady: (mode: EditorMode, state?: PlaybackState) => {
          bridgeReady.value = true
          console.log(`[Editor] TurboWarp ready - Mode: ${mode}, State: ${state}`)
          ElMessage.success('编辑器已就绪')

          if (project.value?.sb3Url) {
            bridge.value?.loadProject(project.value.sb3Url)
          }
        },
        onError: (errorMsg: string) => {
          console.error('[Editor] Bridge error:', errorMsg)
          ElMessage.error(`编辑器错误: ${errorMsg}`)
        },
        onProjectLoaded: (projectId?: number | string) => {
          console.log('[Editor] Project loaded:', projectId)
          bridgeReady.value = true
        },
        onProjectChanged: (projectId?: number | string, hasUnsaved?: boolean) => {
          console.log('[Editor] Project changed:', { projectId, hasUnsaved })
          if (hasUnsaved) {
            lastSaved.value = null
          }
        },
        onSaveResult: (success: boolean, errorMsg?: string) => {
          if (success) {
            lastSaved.value = new Date()
            ElMessage.success('项目已保存到 TurboWarp')
          } else {
            ElMessage.error(errorMsg || '保存到 TurboWarp 失败')
          }
        },
        onPlaybackStateChange: (state: PlaybackState) => {
          console.log('[Editor] Playback state:', state)
        }
      }
    })

    bridge.value.start()
    resolve()
  })
}

function handleIframeError(): void {
  loading.value = false
  error.value = '编辑器加载失败，请检查网络连接或刷新页面重试'
  ElMessage.error(error.value)
}

function handleRetry(): void {
  error.value = ''
  if (isNewProject.value) {
    loading.value = false
  } else if (projectId.value) {
    loadProject(projectId.value)
  }
}

function handleGoBack(): void {
  navigating.value = true
  if (window.history.length > 2) {
    router.back()
  } else {
    router.push('/')
  }
}

function handleTitleBlur(): void {
  if (project.value && projectTitle.value !== project.value.title) {
    handleSaveProject()
  }
}

function toggleFullscreen(): void {
  if (!editorContainer.value) return

  if (!document.fullscreenElement) {
    editorContainer.value.requestFullscreen().then(() => {
      isFullscreen.value = true
    }).catch((e) => {
      console.error('[Editor] Fullscreen error:', e)
    })
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false
    }).catch((e) => {
      console.error('[Editor] Exit fullscreen error:', e)
    })
  }
}

function onFullscreenChange(): void {
  isFullscreen.value = !!document.fullscreenElement
}

function handleKeyDown(e: KeyboardEvent): void {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveProject()
  }

  if ((e.ctrlKey || e.metaKey) && e.key === 'p') {
    e.preventDefault()
    if (project.value?.status === 'draft') {
      handlePublishProject()
    }
  }

  if (e.key === 'F11') {
    e.preventDefault()
    toggleFullscreen()
  }
}

watch(projectTitle, (newTitle, oldTitle) => {
  if (project.value && newTitle !== oldTitle && oldTitle !== undefined) {
    lastSaved.value = null
  }
})

onMounted(async () => {
  document.addEventListener('keydown', handleKeyDown)
  document.addEventListener('fullscreenchange', onFullscreenChange)

  if (isNewProject.value) {
    projectTitle.value = '新项目'
    loading.value = false
    loadingMessage.value = '准备创建新项目...'
    loadingHint.value = '点击「创建」按钮开始'
  } else if (projectId.value) {
    await loadProject(projectId.value)
  } else {
    error.value = '无效的项目ID'
    loading.value = false
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('fullscreenchange', onFullscreenChange)

  if (bridge.value) {
    bridge.value.destroy()
    bridge.value = null
  }
})
</script>

<style scoped>
.scratch-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #1e1e2e;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  z-index: 100;
  flex-shrink: 0;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.title-input {
  width: 220px;
  flex-shrink: 0;
}

.title-input :deep(.el-input__wrapper) {
  border-radius: 4px;
}

.title-input :deep(.el-input__inner) {
  font-weight: 500;
}

.status-tag {
  flex-shrink: 0;
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #2d2d3d;
}

.scratch-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

.scratch-iframe.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
}

.editor-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(30, 30, 46, 0.98);
  color: #cdd6f4;
  z-index: 100;
}

.loading-icon {
  font-size: 48px;
  color: #89b4fa;
}

.loading-text {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.loading-hint {
  font-size: 14px;
  opacity: 0.7;
  margin: 0;
}

.editor-error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(30, 30, 46, 0.98);
  color: #cdd6f4;
  z-index: 100;
}

.error-icon {
  font-size: 48px;
  color: #f38ba8;
}

.error-text {
  font-size: 16px;
  color: #f38ba8;
  margin: 0;
  text-align: center;
  max-width: 400px;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 16px;
  background: #181825;
  border-top: 1px solid #313244;
  font-size: 12px;
  color: #6c7086;
  flex-shrink: 0;
}

.footer-left,
.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shortcut-hint {
  display: flex;
  align-items: center;
  gap: 4px;
}

.shortcut-hint kbd {
  padding: 2px 6px;
  background: #313244;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
  border: 1px solid #45475a;
}

.last-saved {
  color: #a6e3a1;
}

.bridge-status {
  color: #89b4fa;
}

@media (max-width: 900px) {
  .editor-toolbar {
    flex-wrap: wrap;
    padding: 8px;
  }

  .toolbar-left,
  .toolbar-right {
    flex-wrap: wrap;
    gap: 6px;
  }

  .title-input {
    width: 160px;
  }

  .editor-footer {
    flex-direction: column;
    gap: 4px;
    padding: 4px 8px;
  }

  .footer-left,
  .footer-right {
    width: 100%;
    justify-content: center;
    gap: 8px;
  }
}
</style>
