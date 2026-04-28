<template>
  <div>
    <h1 class="page-title">📚 {{ t('homework.title') }}</h1>

    <LoadingSkeleton v-if="loading" :count="4" variant="list" />
    <EmptyState v-else-if="classes.length === 0" icon="📚" :text="t('homework.empty')" />

    <template v-else>
      <el-select v-model="selectedClassId" :placeholder="t('analytics.selectClass')" style="width: 100%; margin-bottom: 14px" @change="loadHomework">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>

      <EmptyState v-if="homeworkList.length === 0" icon="📋" :text="t('homework.empty')" />

      <div v-for="h in homeworkList" :key="h.id" class="page-card" style="cursor: pointer" @click="router.push(`/homework/${h.id}`)">
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div class="card-title">{{ h.title }}</div>
          <el-tag :type="h.status === 'published' ? 'success' : h.status === 'closed' ? 'danger' : 'info'" size="small">
            {{ h.status === 'published' ? t('homework.pending') : h.status === 'closed' ? t('competition.status.ended') : t('competition.status.draft') }}
          </el-tag>
        </div>
        <div class="card-meta">
          <span>{{ t('competition.totalScore', { score: h.totalScore }) }}</span>
          <span>{{ t('homework.submitted') }} {{ h.submitCount }} / {{ t('homework.graded') }} {{ h.gradedCount }}</span>
          <span v-if="h.deadline">⏰ {{ t('homework.deadline') }} {{ new Date(h.deadline).toLocaleDateString() }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Homework' })

import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi, homeworkApi } from '@/api'
import type { ClassRoom, Homework } from '@/types'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const router = useRouter()
const loading = ref(true)
const classes = ref<ClassRoom[]>([])
const selectedClassId = ref<number | null>(null)
const homeworkList = ref<Homework[]>([])

async function loadHomework(classId: number) {
  try {
    const res = await homeworkApi.listByClass(classId)
    if (res.code === 0) homeworkList.value = res.data?.records || []
  } catch { /* 忽略 */ }
}

onMounted(async () => {
  try {
    const res = await userApi.getMyClasses()
    if (res.code === 0 && res.data && res.data.length > 0) {
      classes.value = res.data
      selectedClassId.value = res.data[0].id
      await loadHomework(res.data[0].id)
    }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}
</style>
