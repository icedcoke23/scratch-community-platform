<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">📚</span>
      {{ t('homework.title') }}
    </h1>

    <LoadingSkeleton v-if="loading" :count="4" variant="list" />
    <EmptyState v-else-if="classes.length === 0" icon="📚" text="还没有加入任何班级" />

    <template v-else>
      <div class="class-selector">
        <el-select v-model="selectedClassId" :placeholder="t('analytics.selectClass')" size="large" style="width: 100%" @change="loadHomework">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <EmptyState v-if="homeworkList.length === 0" icon="📋" text="老师还没有布置作业哦" />

      <div v-else class="homework-list">
        <div
          v-for="h in homeworkList"
          :key="h.id"
          class="page-card homework-card"
          @click="router.push(`/homework/${h.id}`)"
        >
          <div class="homework-header">
            <div class="homework-info">
              <div class="card-title">{{ h.title }}</div>
              <span class="status-badge" :class="`status-${h.status}`">
                {{ statusIcon[h.status] }} {{ statusLabel[h.status] }}
              </span>
            </div>
            <div class="homework-score">
              <span class="score-num">{{ h.totalScore }}</span>
              <span class="score-label">满分</span>
            </div>
          </div>
          <div class="homework-stats">
            <div class="hw-stat">
              <span class="hw-stat-icon">📤</span>
              <span>已提交 {{ h.submitCount }}</span>
            </div>
            <div class="hw-stat">
              <span class="hw-stat-icon">✅</span>
              <span>已批改 {{ h.gradedCount }}</span>
            </div>
            <div v-if="h.deadline" class="hw-stat deadline" :class="{ 'deadline-urgent': isUrgent(h.deadline) }">
              <span class="hw-stat-icon">⏰</span>
              <span>截止 {{ formatDate(h.deadline) }}</span>
            </div>
          </div>
          <div class="homework-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: h.submitCount ? Math.round((h.gradedCount / h.submitCount) * 100) : 0 + '%' }"></div>
            </div>
            <span class="progress-text">批改进度 {{ h.submitCount ? Math.round((h.gradedCount / h.submitCount) * 100) : 0 }}%</span>
          </div>
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

const statusIcon: Record<string, string> = {
  draft: '📝',
  published: '📢',
  closed: '✅'
}

const statusLabel: Record<string, string> = {
  draft: '草稿',
  published: '进行中',
  closed: '已结束'
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function isUrgent(deadline: string): boolean {
  const diff = new Date(deadline).getTime() - Date.now()
  return diff > 0 && diff < 3 * 24 * 60 * 60 * 1000 // 3天内
}

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
.class-selector {
  margin-bottom: 20px;
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.homework-card {
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.homework-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.homework-info {
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
.status-published { background: #DCFCE7; color: #16A34A; }
.status-closed { background: #FEE2E2; color: #DC2626; }

.homework-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 14px;
  background: linear-gradient(135deg, var(--primary-bg), #FEF3C7);
  border-radius: 12px;
}

.score-num {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.score-label {
  font-size: 11px;
  color: var(--text2);
  font-weight: 500;
}

.homework-stats {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.hw-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text2);
}

.hw-stat-icon {
  font-size: 16px;
}

.deadline.deadline-urgent {
  color: var(--danger);
  font-weight: 600;
}

.homework-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--border);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--accent-green));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text2);
  font-weight: 500;
  white-space: nowrap;
}

@media (max-width: 480px) {
  .homework-stats { gap: 10px; }
  .hw-stat { font-size: 13px; }
}
</style>
