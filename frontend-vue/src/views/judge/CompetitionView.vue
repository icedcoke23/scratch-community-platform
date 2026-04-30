<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">🏆</span>
      {{ t('competition.title') }}
    </h1>
    <div class="create-bar" v-if="userStore.isTeacher">
      <el-button type="primary" size="large" @click="showCreateDialog = true" class="create-btn">
        ➕ {{ t('competition.create') }}
      </el-button>
    </div>

    <LoadingSkeleton v-if="loading && competitions.length === 0" :count="4" variant="card" />
    <EmptyState v-else-if="competitions.length === 0 && !loading" icon="🏆" text="还没有竞赛，敬请期待！" />

    <div v-else class="competition-list">
      <div
        v-for="c in competitions"
        :key="c.id"
        class="page-card competition-card"
        @click="router.push(`/competition/${c.id}`)"
      >
        <div class="comp-header">
          <div class="comp-info">
            <div class="card-title">{{ statusEmoji[c.status] }} {{ c.title }}</div>
            <span class="status-badge" :class="`status-${c.status?.toLowerCase()}`">
              {{ t(`competition.status.${c.status?.toLowerCase()}`) }}
            </span>
          </div>
          <div v-if="c.status === 'RUNNING' && c.remainingSeconds" class="comp-timer">
            <span class="timer-icon">⏱️</span>
            <span class="timer-text">
              {{ t('competition.remaining', { h: Math.floor(c.remainingSeconds / 3600), m: Math.floor((c.remainingSeconds % 3600) / 60) }) }}
            </span>
          </div>
        </div>
        <div class="comp-meta">
          <span class="meta-item">
            <span class="meta-icon">{{ c.type === 'RATED' ? '📊' : '⏱️' }}</span>
            {{ c.type === 'RATED' ? t('competition.type.rated') : t('competition.type.timed') }}
          </span>
          <span class="meta-item">
            <span class="meta-icon">📝</span>
            {{ t('competition.problems', { count: c.problemCount || 0 }) }}
          </span>
          <span class="meta-item">
            <span class="meta-icon">👥</span>
            {{ t('competition.participants', { count: c.participantCount || 0 }) }}
          </span>
          <span class="meta-item">
            <span class="meta-icon">🎯</span>
            {{ t('competition.totalScore', { score: c.totalScore }) }}
          </span>
        </div>
        <div class="comp-time">
          📅 {{ new Date(c.startTime).toLocaleString() }} ~ {{ new Date(c.endTime).toLocaleString() }}
        </div>
      </div>
    </div>

    <!-- 创建竞赛弹窗 -->
    <el-dialog v-model="showCreateDialog"  width="520px" class="create-dialog">
      <div class="dialog-header-section">
        <span class="dialog-mascot">🏆</span>
        <h3>创建新竞赛</h3>
      </div>
      <el-form :model="createForm" label-position="top" class="create-form">
        <el-form-item :label="t('competition.createTitle')">
          <el-input v-model="createForm.title" placeholder="竞赛名称" size="large" />
        </el-form-item>
        <el-form-item :label="t('competition.createDesc')">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="竞赛描述" size="large" />
        </el-form-item>
        <el-form-item :label="t('competition.createType')">
          <div class="type-cards">
            <div class="type-card" :class="{ selected: createForm.type === 'TIMED' }" @click="createForm.type = 'TIMED'">
              <span class="type-icon">⏱️</span>
              <span class="type-name">计时赛</span>
            </div>
            <div class="type-card" :class="{ selected: createForm.type === 'RATED' }" @click="createForm.type = 'RATED'">
              <span class="type-icon">📊</span>
              <span class="type-name">积分赛</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('competition.createStart')">
          <el-date-picker v-model="createForm.startTime" type="datetime" style="width: 100%" size="large" />
        </el-form-item>
        <el-form-item :label="t('competition.createEnd')">
          <el-date-picker v-model="createForm.endTime" type="datetime" style="width: 100%" size="large" />
        </el-form-item>
        <el-form-item :label="t('competition.createProblems')">
          <el-input v-model="createForm.problemIds" :placeholder="t('competition.createProblemsHint')" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="createLoading" @click="doCreate" size="large" style="width: 100%">
          🚀 {{ t('common.create') }}
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
.create-bar {
  margin-bottom: 20px;
}

.create-btn {
  font-weight: 600;
  border-radius: 12px;
}

.competition-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.competition-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.competition-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.comp-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.comp-info {
  flex: 1;
  min-width: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  margin-top: 6px;
}

.status-draft { background: #F3F4F6; color: #6B7280; }
.status-published { background: #DBEAFE; color: #2563EB; }
.status-running { background: #DCFCE7; color: #16A34A; }
.status-ended { background: #FEE2E2; color: #DC2626; }

.comp-timer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #FEF2F2;
  border-radius: 12px;
  border: 1px solid #FECACA;
}

.timer-icon {
  font-size: 16px;
}

.timer-text {
  font-size: 14px;
  font-weight: 700;
  color: var(--danger);
}

.comp-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text2);
}

.meta-icon {
  font-size: 16px;
}

.comp-time {
  font-size: 13px;
  color: var(--text3);
  padding: 6px 10px;
  background: var(--bg);
  border-radius: 8px;
  display: inline-block;
}

/* 创建弹窗 */
.create-dialog :deep(.el-dialog) {
  border-radius: 20px;
}

.create-dialog :deep(.el-dialog__header) {
  display: none;
}

.dialog-header-section {
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 16px;
}

.dialog-mascot {
  font-size: 40px;
  display: block;
  margin-bottom: 8px;
}

.dialog-header-section h3 {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

.create-form :deep(.el-form-item__label) {
  font-size: 15px;
  font-weight: 600;
}

.type-cards {
  display: flex;
  gap: 12px;
  width: 100%;
}

.type-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px;
  border: 2px solid var(--border);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.type-card:hover {
  border-color: var(--primary-light);
  background: var(--primary-bg);
}

.type-card.selected {
  border-color: var(--primary);
  background: var(--primary-bg);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.type-icon {
  font-size: 28px;
}

.type-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

@media (max-width: 480px) {
  .comp-meta { gap: 10px; }
  .meta-item { font-size: 13px; }
  .comp-header { flex-direction: column; gap: 8px; }
}
</style>
