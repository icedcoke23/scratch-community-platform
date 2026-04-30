<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">⭐</span>
      {{ t('points.title') }}
    </h1>
    <LoadingSkeleton v-if="loading" :count="3" variant="card" />
    <template v-else-if="pointData">
      <!-- 积分概览卡片 -->
      <div class="page-card points-hero">
        <div class="points-circle">
          <span class="points-number">{{ pointData.points }}</span>
          <span class="points-label">积分</span>
        </div>
        <div class="level-info">
          <div class="level-badge">
            <span class="level-icon">⭐</span>
            <span class="level-text">Lv.{{ pointData.level }}</span>
            <span class="level-name">{{ pointData.levelName }}</span>
          </div>
          <div class="level-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: Math.min(pointData.progress || 0, 100) + '%' }"></div>
            </div>
            <span class="progress-text">{{ pointData.points }} / {{ pointData.nextLevelPoints }} 升级</span>
          </div>
        </div>
      </div>

      <!-- 签到按钮 -->
      <div class="page-card checkin-card">
        <el-button
          type="primary"
          size="large"
          :loading="checkinLoading"
          @click="doCheckin"
          class="checkin-btn"
          :class="{ checked: alreadyChecked }"
        >
          {{ alreadyChecked ? '✅ 今日已签到' : '📅 每日签到 +5 积分' }}
        </el-button>
      </div>

      <!-- 积分规则 -->
      <div class="page-card rules-card">
        <h3 class="section-title">📋 积分规则</h3>
        <div class="rules-grid">
          <div class="rule-item">
            <span class="rule-icon">📅</span>
            <span class="rule-name">每日签到</span>
            <span class="rule-points">+5</span>
          </div>
          <div class="rule-item">
            <span class="rule-icon">📝</span>
            <span class="rule-name">发布项目</span>
            <span class="rule-points">+10</span>
          </div>
          <div class="rule-item">
            <span class="rule-icon">❤️</span>
            <span class="rule-name">收到点赞</span>
            <span class="rule-points">+2</span>
          </div>
          <div class="rule-item">
            <span class="rule-icon">✅</span>
            <span class="rule-name">判题通过</span>
            <span class="rule-points">+15</span>
          </div>
          <div class="rule-item">
            <span class="rule-icon">📚</span>
            <span class="rule-name">完成作业</span>
            <span class="rule-points">+20</span>
          </div>
        </div>
      </div>

      <!-- 排行榜 -->
      <div v-if="ranking.length > 0" class="page-card">
        <h3 class="section-title">🏆 积分排行榜</h3>
        <div class="ranking-list">
          <div v-for="(r, i) in ranking" :key="r.id || i" class="rank-item" :class="{ 'rank-top': i < 3 }">
            <div class="rank-medal" :class="medalClass(i)">{{ medalEmoji(i) }}</div>
            <div class="rank-avatar" :class="`avatar-${i % 5}`">
              {{ (r.nickname || r.username || '?')[0] }}
            </div>
            <div class="rank-info">
              <div class="rank-name">{{ r.nickname || r.username }}</div>
              <div class="rank-level">Lv.{{ r.level }}</div>
            </div>
            <div class="rank-points">⭐ {{ r.points }}</div>
          </div>
        </div>
      </div>

      <!-- 积分历史 -->
      <div v-if="logs.length > 0" class="page-card">
        <h3 class="section-title">📜 积分记录</h3>
        <div class="log-list">
          <div v-for="r in logs" :key="r.id" class="log-item">
            <div class="log-icon">{{ typeIcon[r.type] || '📌' }}</div>
            <div class="log-info">
              <div class="log-text">{{ r.remark || r.type }}</div>
              <div class="log-time">{{ timeAgo(r.createdAt) }}</div>
            </div>
            <div class="log-points" :class="r.points > 0 ? 'positive' : 'negative'">
              {{ r.points > 0 ? '+' : '' }}{{ r.points }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Points' })
import { ref, computed, onMounted } from 'vue'
import { pointApi } from '@/api'
import { ElMessage } from 'element-plus'
import { timeAgo } from '@/utils'
import { useI18n } from '@/composables/useI18n'
import { getErrorMessage } from '@/utils/error'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()

interface PointData {
  points: number
  level: number
  levelName: string
  progress?: number
  nextLevelPoints: number
}

interface PointRankItem {
  id: number
  username: string
  nickname: string
  points: number
  level: number
}

interface PointLogItem {
  id: number
  type: string
  points: number
  remark?: string
  createdAt: string
}

const loading = ref(true)
const checkinLoading = ref(false)
const pointData = ref<PointData | null>(null)
const ranking = ref<PointRankItem[]>([])
const logs = ref<PointLogItem[]>([])

const alreadyChecked = computed(() => {
  // 检查今日是否已签到
  const today = new Date().toDateString()
  return logs.value.some(l => l.type === 'DAILY_CHECKIN' && new Date(l.createdAt).toDateString() === today)
})

const typeIcon: Record<string, string> = {
  DAILY_CHECKIN: '📅', PUBLISH_PROJECT: '📝', RECEIVE_LIKE: '❤️',
  AC_SUBMISSION: '✅', COMPLETE_HOMEWORK: '📚', ADMIN_ADJUST: '⚙️'
}

function medalClass(i: number): string {
  if (i === 0) return 'medal-gold'
  if (i === 1) return 'medal-silver'
  if (i === 2) return 'medal-bronze'
  return 'medal-default'
}

function medalEmoji(i: number): string {
  if (i === 0) return '🥇'
  if (i === 1) return '🥈'
  if (i === 2) return '🥉'
  return `${i + 1}`
}

async function doCheckin() {
  if (alreadyChecked.value) { ElMessage.warning('今日已签到'); return }
  checkinLoading.value = true
  try {
    const res = await pointApi.checkin()
    if (res.code === 0) {
      if (res.data?.checkedIn) { ElMessage.success(`🎉 签到成功！+${res.data.earned} 积分`); await loadData() }
      else ElMessage.warning('今日已签到')
    } else ElMessage.error(res.msg)
  } catch (e: unknown) { ElMessage.error(getErrorMessage(e)) }
  finally { checkinLoading.value = false }
}

async function loadData() {
  const [pr, lr, rr] = await Promise.all([
    pointApi.getMyPoints().catch(() => ({ code: -1, data: null })),
    pointApi.getLogs(1, 20).catch(() => ({ code: -1, data: null })),
    pointApi.getRanking(10).catch(() => ({ code: -1, data: null }))
  ])
  if (pr.code === 0) pointData.value = pr.data
  if (lr.code === 0) logs.value = lr.data?.records || []
  if (rr.code === 0) ranking.value = rr.data || []
}

onMounted(async () => { await loadData(); loading.value = false })
</script>

<style scoped>
.points-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 28px;
  background: linear-gradient(135deg, var(--primary-bg), #FEF3C7);
}

.points-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
}

.points-number {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.points-label {
  font-size: 12px;
  opacity: 0.9;
}

.level-info {
  flex: 1;
}

.level-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.level-icon {
  font-size: 24px;
}

.level-text {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary);
}

.level-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text2);
}

.level-progress {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-bar {
  height: 10px;
  background: var(--border);
  border-radius: 5px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--accent-green));
  border-radius: 5px;
  transition: width 0.5s ease;
}

.progress-text {
  font-size: 13px;
  color: var(--text2);
}

.checkin-card {
  text-align: center;
  padding: 20px;
}

.checkin-btn {
  min-width: 240px;
  height: 52px;
  font-size: 17px;
  font-weight: 700;
  border-radius: 14px;
}

.checkin-btn.checked {
  background: var(--success);
  border-color: var(--success);
}

.rules-card {
  padding: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.rule-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px;
  background: var(--bg);
  border-radius: 14px;
  transition: transform 0.2s ease;
}

.rule-item:hover {
  transform: translateY(-2px);
}

.rule-icon {
  font-size: 28px;
}

.rule-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.rule-points {
  font-size: 18px;
  font-weight: 800;
  color: var(--success);
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.rank-item:hover {
  background: var(--primary-bg);
  transform: translateX(4px);
}

.rank-item.rank-top {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.05), rgba(234, 179, 8, 0.02));
}

.rank-medal {
  font-size: 24px;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
}

.medal-default {
  font-size: 14px;
  font-weight: 700;
  color: var(--text3);
}

.rank-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.avatar-0 { background: linear-gradient(135deg, #3B82F6, #2563EB); }
.avatar-1 { background: linear-gradient(135deg, #22C55E, #16A34A); }
.avatar-2 { background: linear-gradient(135deg, #F97316, #EA580C); }
.avatar-3 { background: linear-gradient(135deg, #A855F7, #9333EA); }
.avatar-4 { background: linear-gradient(135deg, #EC4899, #DB2777); }

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--text);
}

.rank-level {
  font-size: 12px;
  color: var(--text2);
}

.rank-points {
  font-weight: 700;
  color: var(--primary);
  font-size: 16px;
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.log-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  transition: background 0.2s ease;
}

.log-item:hover {
  background: var(--bg);
}

.log-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.log-info {
  flex: 1;
  min-width: 0;
}

.log-text {
  font-size: 14px;
  color: var(--text);
  font-weight: 500;
}

.log-time {
  font-size: 12px;
  color: var(--text3);
  margin-top: 2px;
}

.log-points {
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.log-points.positive { color: var(--success); }
.log-points.negative { color: var(--danger); }

@media (max-width: 768px) {
  .points-hero { flex-direction: column; text-align: center; padding: 20px; }
  .level-badge { justify-content: center; }
  .rules-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 480px) {
  .points-circle { width: 80px; height: 80px; }
  .points-number { font-size: 22px; }
  .rules-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
