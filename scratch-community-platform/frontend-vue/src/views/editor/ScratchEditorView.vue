<template>
  <div class="scratch-editor-page">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <el-input
          v-model="projectTitle"
          class="title-input"
          placeholder="项目标题"
          maxlength="50"
        />
        <el-tag v-if="project?.status === 'draft'" class="status-draft">草稿</el-tag>
        <el-tag v-else type="success">已发布</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button-group>
          <el-button @click="fullscreen">
            <el-icon><FullScreen /></el-icon>
            全屏
          </el-button>
        </el-button-group>
        <el-button type="primary" @click="saveProject" :loading="saving">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
        <el-button @click="publishProject" :loading="publishing" v-if="project?.status === 'draft'">
          发布
        </el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleSb3Import"
          accept=".sb3"
        >
          <el-button>导入
            <el-icon><Upload /></el-icon>
            导入
          </el-button>
        </el-upload>
      </div>
    </div>
    <div class="editor-container" ref="editorContainer">
      <div v-if="loading" class="editor-loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <p>正在加载编辑器...</p>
        <p class="loading-hint">首次加载可能需要 3-5 秒</p>
      </div>
      <iframe
        ref="scratchFrame"
        :src="editorUrl"
        class="scratch-iframe"
        :class="{ 'fullscreen': isFullscreen }"
        @load="onIframeLoad"
        allow="clipboard-read; clipboard-write"
        sandbox="allow-scripts allow-same-origin allow-popups allow-forms allow-downloads"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, FullScreen, Check, Loading, Upload
} from '@element-plus/icons-vue'
import { projectApi, type ProjectDetail } from '@/api'
import { createScratchBridge } from '@/utils/scratchBridge'
import { buildEditorUrl, getDownloadUrl } from '@/utils/turbowarpConfig'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const publishing = ref(false)
const isFullscreen = ref(false)
const project = ref<ProjectDetail | null>(null)
const projectTitle = ref('新项目')
const scratchFrame = ref<HTMLIFrameElement>()
const editorContainer = ref<HTMLDivElement>()
const bridge = ref<ReturnType<typeof createScratchBridge>>()

const isNewProject = computed(() => !route.params.id || route.params.id === 'new')

const editorUrl = computed(() => {
  if (isNewProject.value) {
    return buildEditorUrl()
  }
  if (project.value?.id) {
    const url = getDownloadUrl(project.value.id)
    return buildEditorUrl(url)
  }
  return buildEditorUrl()
})

async function loadProject() {
  if (isNewProject.value) {
    loading.value = false
    return
  }

  try {
    const res = await projectApi.getDetail(Number(route.params.id))
    if (res.code === 0 && res.data) {
      project.value = res.data
      projectTitle.value = res.data.title || '新项目'
    }
  } catch (e) {
    ElMessage.error('加载项目失败')
  } finally {
    loading.value = false
  }
}

function onIframeLoad() {
  loading.value = false
  bridge.value = createScratchBridge({
    iframe: scratchFrame.value || null,
    onReady: () => {
      console.log('TurboWarp 准备就绪')
    }
  })
  bridge.value.start()
}

async function saveProject() {
  if (!project.value && isNewProject.value) {
    try {
      saving.value = true
      const res = await projectApi.create({
        title: projectTitle.value,
        description: ''
      })
      if (res.code === 0 && res.data) {
        project.value = res.data as unknown as ProjectDetail
        ElMessage.success('项目创建成功')
        router.replace(`/editor/${res.data.id}`)
      }
    } catch (e) {
      ElMessage.error('创建失败')
    } finally {
      saving.value = false
    }
    return
  }

  if (!project.value) return

  try {
    saving.value = true
    await projectApi.update(project.value.id, {
      title: projectTitle.value,
      description: ''
    })
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function publishProject() {
  if (!project.value) return
  try {
    await ElMessageBox.confirm('确定要发布这个项目吗？', '发布确认')
    publishing.value = true
    await projectApi.publish(project.value.id)
    project.value.status = 'published'
    ElMessage.success('发布成功')
  } catch (e) {
    // 用户取消或出错
  } finally {
    publishing.value = false
  }
}

async function handleSb3Import(file: File) {
  if (isNewProject.value && !project.value) {
    try {
      saving.value = true
      const res = await projectApi.create({
        title: projectTitle.value,
        description: ''
      })
      if (res.code === 0 && res.data) {
        project.value = res.data as unknown as ProjectDetail
        router.replace(`/editor/${res.data.id}`)
      }
    } catch (e) {
      ElMessage.error('创建失败')
      saving.value = false
      return false
    }
  }

  if (!project.value) {
    ElMessage.error('项目尚未准备好')
    return false
  }

  try {
    saving.value = true
    const uploadRes = await projectApi.uploadSb3(project.value.id, file)
    if (uploadRes.code === 0) {
      ElMessage.success('项目导入成功')
      // 重新加载页面
      router.go(0)
    } else {
      ElMessage.error(uploadRes.msg || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入 SB3 失败')
  } finally {
    saving.value = false
  }
  return false
}

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

function goBack() {
  router.back()
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  loadProject()
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  bridge.value?.destroy()
  document.removeEventListener('fullscreenchange', onFullscreenChange)
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
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
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

.status-draft {
  background: #f0ad4e;
  color: #ffffff;
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
  background: rgba(30, 30, 46, 0.95);
  color: #cdd6f4;
  z-index: 100;
}

.loading-icon {
  font-size: 32px;
  animation: spin 1s linear infinite;
}

.loading-hint {
  font-size: 13px;
  opacity: 0.7;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .toolbar-left, .toolbar-right {
    flex-wrap: wrap;
    gap: 4px;
  }
  .title-input {
    width: 150px;
  }
}
</style>
