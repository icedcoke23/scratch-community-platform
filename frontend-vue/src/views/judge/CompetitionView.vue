<template>
  <div>
    <h1 class="page-title">🏆 {{ t('competition.title') }}</h1>
    <div style="margin-bottom: 14px" v-if="userStore.isTeacher">
      <el-button type="primary" @click="showCreateDialog = true">➕ {{ t('competition.create') }}</el-button>
    </div>

    <LoadingSkeleton v-if="loading && competitions.length === 0" :count="4" variant="card" />
    <EmptyState v-else-if="competitions.length === 0 && !loading" icon="🏆" :text="t('common.empty')" />

    <div v-else>
      <div v-for="c in competitions" :key="c.id" class="page-card" style="cursor: pointer" @click="router.push(`/competition/${c.id}`)">
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div class="card-title">{{ statusEmoji[c.status] }} {{ c.title }}</div>
          <el-tag :type="statusType(c.status)" size="small">{{ t(`competition.status.${c.status.toLowerCase()}`) }}</el-tag>
        </div>
        <div class="card-meta">
          <span>{{ c.type === 'RATED' ? '📊 ' + t('competition.type.rated') : '⏱️ ' + t('competition.type.timed') }}</span>
          <span>📝 {{ t('competition.problems', { count: c.problemCount || 0 }) }}</span>
          <span>👥 {{ t('competition.participants', { count: c.participantCount || 0 }) }}</span>
          <span>{{ t('competition.totalScore', { score: c.totalScore }) }}</span>
          <span v-if="c.status === 'RUNNING' && c.remainingSeconds" style="color: var(--danger); font-weight: 600">
            {{ t('competition.remaining', { h: Math.floor(c.remainingSeconds / 3600), m: Math.floor((c.remainingSeconds % 3600) / 60) }) }}
          </span>
        </div>
        <div style="font-size: 12px; color: var(--text2); margin-top: 4px">
          {{ new Date(c.startTime).toLocaleString() }} ~ {{ new Date(c.endTime).toLocaleString() }}
        </div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" :title="t('competition.create')" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item :label="t('competition.createTitle')"><el-input v-model="createForm.title" /></el-form-item>
        <el-form-item :label="t('competition.createDesc')"><el-input v-model="createForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item :label="t('competition.createType')">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option :label="'⏱️ ' + t('competition.type.timed')" value="TIMED" />
            <el-option :label="'📊 ' + t('competition.type.rated')" value="RATED" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('competition.createStart')"><el-date-picker v-model="createForm.startTime" type="datetime" style="width: 100%" /></el-form-item>
        <el-form-item :label="t('competition.createEnd')"><el-date-picker v-model="createForm.endTime" type="datetime" style="width: 100%" /></el-form-item>
        <el-form-item :label="t('competition.createProblems')">
          <el-input v-model="createForm.problemIds" :placeholder="t('competition.createProblemsHint')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="createLoading" @click="doCreate" style="width: 100%">
          {{ t('common.create') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Competition' })

import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { competitionApi } from '@/api'
import type { Competition } from '@/types'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const competitions = ref<Competition[]>([])
const showCreateDialog = ref(false)
const createLoading = ref(false)

const statusEmoji: Record<string, string> = { DRAFT: '📝', PUBLISHED: '📢', RUNNING: '🔴', ENDED: '✅' }
function statusType(s: string) {
  return ({ RUNNING: 'success', PUBLISHED: '', ENDED: 'danger' } as Record<string, string>)[s] || 'info'
}

const createForm = reactive({ title: '', description: '', type: 'TIMED', startTime: '', endTime: '', problemIds: '' })

async function doCreate() {
  if (!createForm.title || !createForm.startTime || !createForm.endTime || !createForm.problemIds) {
    ElMessage.warning(t('competition.fillAll')); return
  }
  createLoading.value = true
  try {
    const res = await competitionApi.create({
      title: createForm.title, description: createForm.description, type: createForm.type,
      startTime: new Date(createForm.startTime).toISOString(),
      endTime: new Date(createForm.endTime).toISOString(),
      problemIds: createForm.problemIds.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
    })
    if (res.code === 0) {
      showCreateDialog.value = false
      ElMessage.success(t('competition.createSuccess'))
      router.push(`/competition/${res.data?.id}`)
    } else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { createLoading.value = false }
}

onMounted(async () => {
  try { const res = await competitionApi.list(); if (res.code === 0) competitions.value = res.data?.records || [] }
  catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}
</style>
