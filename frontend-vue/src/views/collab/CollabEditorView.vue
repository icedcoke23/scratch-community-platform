<template>
  <div class="collab-editor-view">
    <!-- 协作工具栏 -->
    <CollabToolbar
      :connected="connected"
      :participants="participants"
      :version="currentVersion"
      @toggle-chat="showChat = !showChat"
      @leave="handleLeave"
    />

    <!-- 主编辑区 -->
    <div class="editor-main">
      <!-- Scratch 编辑器 (嵌入已有的编辑器页面) -->
      <div class="editor-container" ref="editorContainer">
        <iframe
          v-if="projectId"
          ref="editorFrame"
          :src="`/editor/${projectId}?collab=true&sessionId=${sessionId}`"
          class="editor-iframe"
          @mousemove="onMouseMove"
        ></iframe>

        <!-- 远程光标层 -->
        <CollabCursors :participants="participants" />
      </div>

      <!-- 聊天面板 -->
      <CollabChat
        :visible="showChat"
        :messages="chatMessages"
        @send="sendChat"
        @close="showChat = false"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCollabWebSocket } from '@/composables/useCollabWebSocket'
import { createCollabSession, getActiveCollabSession } from '@/api/collab'
import CollabToolbar from '@/components/CollabToolbar.vue'
import CollabCursors from '@/components/CollabCursors.vue'
import CollabChat from '@/components/CollabChat.vue'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => Number(route.params.projectId))
const sessionId = ref<number | null>(null)
const showChat = ref(false)

const {
  connected,
  participants,
  currentVersion,
  chatMessages,
  connect,
  disconnect,
  sendCursor,
  sendChat,
} = useCollabWebSocket(() => sessionId.value)

onMounted(async () => {
  if (!projectId.value) {
    router.push('/feed')
    return
  }

  // 查找或创建协作会话
  try {
    const existing = await getActiveCollabSession(projectId.value)
    if (existing.data) {
      sessionId.value = existing.data.id
    } else {
      const created = await createCollabSession(projectId.value)
      if (created.data) {
        sessionId.value = created.data.id
      }
    }
    // WebSocket 会在 composable 的 onMounted 中自动连接
  } catch (err) {
    console.error('创建协作会话失败:', err)
  }
})

function handleLeave() {
  disconnect()
  router.push(`/project/${projectId.value}`)
}

/** 鼠标移动时发送光标位置（节流） */
let cursorThrottle = 0
function onMouseMove(e: MouseEvent) {
  const now = Date.now()
  if (now - cursorThrottle < 50) return // 50ms 节流
  cursorThrottle = now
  sendCursor(e.offsetX, e.offsetY)
}
</script>

<style scoped>
.collab-editor-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.editor-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.editor-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
