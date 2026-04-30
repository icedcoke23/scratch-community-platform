<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="80%"
    class="preview-dialog"
    :close-on-click-modal="true"
    destroy-on-close
    @close="onClose"
  >
    <div class="preview-content">
      <ScratchPreview
        v-if="projectId"
        :project-id="projectId"
        :title="title"
        :author="author"
        :like-count="likes"
        :cover-url="coverUrl"
        size="large"
        :auto-load="true"
        :show-controls="true"
        :show-edit-btn="true"
        :show-info="true"
      />
    </div>
    <template #footer>
      <div class="preview-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button type="primary" @click="goToDetail">查看详情</el-button>
        <el-button type="success" @click="goToEditor">在编辑器中打开</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ScratchPreview from './ScratchPreview.vue'

const props = defineProps<{
  modelValue: boolean
  projectId?: number
  title?: string
  author?: string
  likes?: number
  coverUrl?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const router = useRouter()
const visible = ref(props.modelValue)

watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => { emit('update:modelValue', v) })

function onClose() {
  visible.value = false
}

function goToDetail() {
  if (props.projectId) {
    visible.value = false
    router.push(`/project/${props.projectId}`)
  }
}

function goToEditor() {
  if (props.projectId) {
    visible.value = false
    router.push(`/editor/${props.projectId}`)
  }
}
</script>

<style scoped>
.preview-dialog :deep(.el-dialog) {
  border-radius: 20px;
  overflow: hidden;
}

.preview-dialog :deep(.el-dialog__header) {
  padding: 16px 24px;
  background: var(--bg, #f5f5f5);
  border-bottom: 1px solid var(--border, #e2e8f0);
}

.preview-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.preview-content {
  min-height: 400px;
  background: #1e1e2e;
}

.preview-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 24px;
}

@media (max-width: 768px) {
  .preview-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 10px auto;
  }
}
</style>
