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
      <div v-if="loading" class="editor-loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>{{ t('editor.loading') }}</p>
        <p class="loading-hint">{{ t('editor.loadingHint') }}</p>
      </div>
      <iframe
        ref="scratchFrame"
        :src="editorUrl"
        class="scratch-iframe"
        :class="{ 'fullscreen': isFullscreen }"
        @load="onIframeLoad"
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
  ArrowLeft, Edit, FullScreen, Check, Download, Loading, Upload
} from '@element-plus/icons-vue'
import { projectApi, getPresignedUrl } from '@/api'
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
const saving = ref(false)
const publishing = ref(false)
const isDirty = ref(false)
const isFullscreen = ref(false)
const showInfo = ref(false)
const isEditor = ref(true)
const autoSaving = ref(false)

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

// ScratchBridge 实例
let scratchBridge: ScratchBridge | null = null

// 是否新建项目
const isNewProject = computed(() => !route.params.id || route.params.id === 'new')

/**
 * Scratch 编辑器 URL
 *
 * 使用 turbowarp.org/embed（iframe 专用页面，支持完整编辑器功能）。
 * 通过 postMessage 桥接协议可在播放器/编辑器模式间切换。
 *
 * 对于已有项目，通过 ?project_url= 参数传入 sb3 presigned URL（MinIO 直接访问，无需 CORS）。
 */
const editorUrl = ref('about:blank')

/**
 * 加载项目信息 + 构建编辑器 URL
 */
async function loadProject() {
  if (isNewProject.value) {
    projectTitle.value = t('editor.importedProject')
    loading.value = false
    return
  }

  try {
    const res = await projectApi.getDetail(Number(route.params.id))
    if (res.code === 0 && res.data) {
      project.value = res.data
      projectTitle.value = res.data.title
      projectDescription.value = res.data.description || ''
      projectTags.value = res.data.tags || ''

      // 构建带 project_url 的编辑器 URL（异步获取 presigned URL）
      await buildEditorUrl()
    }
  } catch (e) {
    ElMessage.error(t('editor.loadFailed'))
  } finally {
    loading.value = false
  }
}

/**
 * 构建编辑器 URL
 *
 * 使用 TurboWarp embed 页面 + presigned URL 加载 sb3 文件。
 * presigned URL 直接指向 MinIO，无需 CORS 配置。
 * 通过 postMessage 可切换播放器/编辑器模式。
 */
async function buildEditorUrl() {
  if (!project.value) return

  if (project.value.sb3Url) {
    try {
      const res = await getPresignedUrl(project.value.id)
      if (res.code === 0 && res.data) {
        editorUrl.value = `https://turbowarp.org/embed?project_url=${encodeURIComponent(res.data)}&autostart=false`
        logger.log('编辑器 URL（presigned）:', editorUrl.value)
        return
      }
    } catch (e) {
      logger.warn('获取 presigned URL 失败，回退到直接下载:', e)
    }
    // 回退：使用后端直接下载端点
    const downloadUrl = `${window.location.origin}/api/v1/project/${project.value.id}/sb3/download`
    editorUrl.value = `https://turbowarp.org/embed?project_url=${encodeURIComponent(downloadUrl)}&autostart=false`
    logger.log('编辑器 URL（回退）:', editorUrl.value)
  } else {
    // 无 sb3 文件：空白 embed
    editorUrl.value = 'https://turbowarp.org/embed'
    logger.log('编辑器 URL（空白项目）')
  }
}

// iframe 加载完成
function onIframeLoad() {
  loading.value = false
  setupEditorCommunication()
}

// 设置与 Scratch 编辑器的通信
function setupEditorCommunication() {
  scratchBridge?.destroy()

  scratchBridge = createScratchBridge({
    iframe: scratchFrame.value || null,
    onProjectChanged() {
      isDirty.value = true
    },
    onProjectSave(base64Data) {
      handleSb3AutoSave(base64Data)
    },
    onError(error) {
      logger.warn('Scratch 错误:', error)
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
    const response = await fetch(`/api/v1/project/${project.value.id}/sb3/auto-save`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({ data: base64Data })
    })
    const res = await response.json()
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
 * 请求 TurboWarp 导出当前项目数据
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
        title: projectTitle.value,
        description: projectDescription.value,
        tags: projectTags.value
      })
      if (res.code === 0 && res.data) {
        project.value = res.data as unknown as ProjectDetail
        ElMessage.success(t('editor.projectCreated'))
        router.replace(`/editor/${res.data.id}`)
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

<style scoped>
.scratch-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #1e1e2e;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  z-index: 10;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-input {
  width: 240px;
}

.title-input :deep(.el-input__inner) {
  font-weight: 600;
  font-size: 15px;
  border: none;
  background: transparent;
}

.title-input :deep(.el-input__inner:focus) {
  background: var(--bg);
  border-radius: var(--radius-sm);
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.scratch-iframe {
  width: 100%;
  height: 100%;
  border: none;
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
  gap: 12px;
  background: #1e1e2e;
  color: #cdd6f4;
}

.loading-icon {
  font-size: 48px;
  animation: spin 1s linear infinite;
}

.loading-hint {
  font-size: 12px;
  color: #6c7086;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .editor-toolbar {
    padding: 6px 10px;
    flex-wrap: wrap;
    gap: 6px;
  }

  .title-input {
    width: 150px;
  }

  .toolbar-right {
    gap: 4px;
  }
}
</style>
