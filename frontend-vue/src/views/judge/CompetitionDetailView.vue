<template>
  <div class="competition-detail-page">
    <div class="back-link" @click="router.push('/competition')">← {{ t('common.back') }}</div>
    <LoadingSkeleton v-if="loading" variant="detail" />
    <template v-else-if="comp">
      <!-- 竞赛头部信息 -->
      <div class="hero-card page-card">
        <div class="hero-top">
          <div class="hero-info">
            <h1 class="comp-title">{{ comp.title }}</h1>
            <div class="comp-tags">
              <el-tag :type="statusType(comp.status)" size="small" effect="dark">{{ statusLabel[comp.status] }}</el-tag>
              <el-tag size="small" :type="comp.type === 'RATED' ? 'warning' : 'info'">
                {{ comp.type === 'RATED' ? '📊 排名赛' : '⏱️ 限时赛' }}
              </el-tag>
            </div>
          </div>
          <div class="hero-action">
            <el-button v-if="userStore.isLoggedIn && comp.status === 'PUBLISHED' && !comp.registered" type="primary" size="large" @click="doRegister" :loading="registerLoading">
              📢 立即报名
            </el-button>
            <el-button v-else-if="comp.registered && comp.status === 'RUNNING'" type="success" size="large" @click="goToProblems">
              🚀 进入答题
            </el-button>
            <div v-else-if="comp.registered" class="registered-badge">✅ 已报名</div>
          </div>
        </div>
        <p v-if="comp.description" class="comp-desc">{{ comp.description }}</p>

        <!-- 统计数据 -->
        <div class="comp-stats">
          <div class="stat-item">
            <span class="stat-num">{{ comp.problemCount }}</span>
            <span class="stat-label">题目</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ comp.participantCount }}</span>
            <span class="stat-label">参赛者</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ comp.totalScore }}</span>
            <span class="stat-label">满分</span>
          </div>
          <div class="stat-item" v-if="timeInfo">
            <span class="stat-num" :class="timeInfo.urgent ? 'text-red' : ''">{{ timeInfo.text }}</span>
            <span class="stat-label">{{ timeInfo.label }}</span>
          </div>
        </div>

        <!-- 时间轴 -->
        <div class="time-bar">
          <div class="time-item">
            <span class="time-icon">🟢</span>
            <span>开始：{{ new Date(comp.startTime).toLocaleString('zh-CN') }}</span>
          </div>
          <div class="time-progress" v-if="progressPercent >= 0">
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: Math.min(progressPercent, 100) + '%' }" :class="{ 'almost-end': progressPercent > 80 }" />
            </div>
          </div>
          <div class="time-item">
            <span class="time-icon">🔴</span>
            <span>结束：{{ new Date(comp.endTime).toLocaleString('zh-CN') }}</span>
          </div>
        </div>
      </div>

      <!-- 排行榜 -->
      <div class="page-card" v-if="rankings.length > 0">
        <div class="section-title">🏅 实时排名</div>
        <el-table :data="rankings" stripe size="small" :row-class-name="tableRowClass">
          <el-table-column label="#" width="60">
            <template #default="{ $index }">
              <span class="rank-num" :class="{ 'top-1': $index === 0, 'top-2': $index === 1, 'top-3': $index === 2 }">
                {{ $index === 0 ? '🥇' : $index === 1 ? '🥈' : $index === 2 ? '🥉' : $index + 1 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="选手" min-width="120">
            <template #default="{ row }">
              <div class="player-cell">
                <span class="player-name">{{ row.nickname || row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="80">
            <template #default="{ row }">
              <span class="score-val">{{ row.totalScore }}</span>
            </template>
          </el-table-column>
          <el-table-column label="通过" width="80">
            <template #default="{ row }">
              <span>{{ row.solvedCount }}/{{ comp.problemCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="罚时" width="80">
            <template #default="{ row }">
              <span class="penalty-val">{{ row.penalty }}min</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 无排名时 -->
      <div v-else-if="comp.status === 'ENDED'" class="page-card">
        <div class="empty-state">
          <div class="empty-icon">🏁</div>
          <div>竞赛已结束，暂无排名数据</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CompetitionDetailView' })
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { competitionApi } from '@/api'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/utils/error'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const comp = ref<any>(null)
const rankings = ref<any[]>([])
const registerLoading = ref(false)
let timer: ReturnType<typeof setInterval> | null = null
const now = ref(Date.now())

const statusLabel: Record<string, string> = {
  DRAFT: t('competition.status.draft'),
  PUBLISHED: t('competition.status.published'),
  RUNNING: t('competition.status.running'),
  ENDED: t('competition.status.ended')
}

function statusType(s: string): 'success' | '' | 'danger' | 'info' {
  const map: Record<string, 'success' | '' | 'danger'> = { RUNNING: 'success', PUBLISHED: '', ENDED: 'danger' }
  return map[s] || 'info'
}

const compId = Number(route.params.id)

const timeInfo = computed(() => {
  if (!comp.value) return null
  const start = new Date(comp.value.startTime).getTime()
  const end = new Date(comp.value.endTime).getTime()
  if (comp.value.status === 'PUBLISHED' && now.value < start) {
    const diff = start - now.value
    return { text: formatDuration(diff), label: '距开始', urgent: diff < 3600000 }
  }
  if (comp.value.status === 'RUNNING' && now.value < end) {
    const diff = end - now.value
    return { text: formatDuration(diff), label: '剩余时间', urgent: diff < 1800000 }
  }
  return null
})

const progressPercent = computed(() => {
  if (!comp.value) return -1
  const start = new Date(comp.value.startTime).getTime()
  const end = new Date(comp.value.endTime).getTime()
  if (now.value < start) return 0
  if (now.value > end) return 100
  return ((now.value - start) / (end - start)) * 100
})

function formatDuration(ms: number): string {
  if (ms <= 0) return '已结束'
  const h = Math.floor(ms / 3600000)
  const m = Math.floor((ms % 3600000) / 60000)
  const s = Math.floor((ms % 60000) / 1000)
  if (h > 0) return `${h}时${m}分`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

function tableRowClass({ rowIndex }: { rowIndex: number }) {
  if (rowIndex < 3) return 'top-row'
  return ''
}

function goToProblems() {
  router.push('/problems')
}

async function doRegister() {
  registerLoading.value = true
  try {
    const res = await competitionApi.register(compId)
    if (res.code === 0) {
      ElMessage.success('报名成功！')
      comp.value.registered = true
      comp.value.participantCount++
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { registerLoading.value = false }
}

onMounted(async () => {
  try {
    const [cr, rr] = await Promise.all([
      competitionApi.getDetail(compId),
      competitionApi.getRanking(compId).catch(() => ({ code: -1, data: null }))
    ])
    if (cr.code === 0) comp.value = cr.data
    else { ElMessage.error(cr.msg); router.push('/competition'); return }
    rankings.value = rr.code === 0 ? (rr.data?.records || []) : []
  } catch { /* ignore */ }
  finally { loading.value = false }

  // Countdown timer
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.competition-detail-page {
  max-width: 1024px;
  margin: 0 auto;
}

.hero-card {
  position: relative;
  overflow: hidden;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.comp-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 10px 0;
  color: var(--text, #1a1a2e);
}

.comp-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.comp-desc {
  font-size: 14px;
  color: var(--text2, #64748b);
  line-height: 1.8;
  margin: 16px 0;
}

.registered-badge {
  font-size: 16px;
  font-weight: 600;
  color: var(--success, #10B981);
  padding: 10px 20px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 12px;
}

.comp-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 16px;
  margin: 20px 0;
  padding: 20px;
  background: var(--bg, #f5f5f5);
  border-radius: 12px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: var(--primary, #3B82F6);
}

.stat-num.text-red {
  color: #EF4444;
}

.stat-label {
  font-size: 12px;
  color: var(--text2, #64748b);
  margin-top: 4px;
}

.time-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text2, #64748b);
  white-space: nowrap;
}

.time-icon {
  font-size: 10px;
}

.time-progress {
  flex: 1;
  min-width: 100px;
}

.progress-track {
  height: 8px;
  background: var(--border, #e5e7eb);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10B981, #3B82F6);
  border-radius: 4px;
  transition: width 1s linear;
}

.progress-fill.almost-end {
  background: linear-gradient(90deg, #F59E0B, #EF4444);
}

.rank-num {
  font-weight: 700;
  font-size: 16px;
}

.top-1, .top-2, .top-3 {
  font-size: 20px;
}

.score-val {
  font-weight: 700;
  color: var(--primary, #3B82F6);
}

.penalty-val {
  font-size: 13px;
  color: var(--text2, #64748b);
}

.section-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

:deep(.top-row) {
  background: rgba(59, 130, 246, 0.05) !important;
}

@media (max-width: 768px) {
  .comp-title {
    font-size: 22px;
  }
  .comp-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
}
</style>
