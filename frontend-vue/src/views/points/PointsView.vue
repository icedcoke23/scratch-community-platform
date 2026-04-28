<template>
  <div>
    <h1 class="page-title">{{ t('points.title') }}</h1>
    <div v-if="loading" class="empty-state">加载中...</div>
    <template v-else-if="pointData">
      <div class="page-card" style="text-align: center; padding: 24px">
        <div style="font-size: 36px; font-weight: 700; color: var(--primary)">{{ pointData.points }}</div>
        <div style="font-size: 14px; color: var(--text2); margin: 4px 0">{{ t('points.total') }}</div>
        <div style="font-size: 18px; font-weight: 600; margin-top: 8px">Lv.{{ pointData.level }} {{ pointData.levelName }}</div>
        <el-progress :percentage="Math.min(pointData.progress || 0, 100)" :stroke-width="8" style="margin-top: 12px" />
        <div style="font-size: 12px; color: var(--text2); margin-top: 4px">{{ pointData.points }} / {{ pointData.nextLevelPoints }} 积分升级</div>
      </div>
      <div class="page-card" style="text-align: center">
        <el-button type="primary" :loading="checkinLoading" @click="doCheckin">📅 {{ t('points.checkin') }}</el-button>
      </div>
      <div class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">📋 {{ t('points.rules') }}</div>
        <div style="font-size: 13px; color: var(--text2); line-height: 2">
          📅 每日签到: <b>+5</b> | 📝 发布项目: <b>+10</b> | ❤️ 收到点赞: <b>+2</b> (每日上限50) | ✅ 判题通过: <b>+15</b> | 📚 完成作业: <b>+20</b>
        </div>
      </div>
      <div v-if="ranking.length > 0" class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">🏆 {{ t('points.ranking') }}</div>
        <div v-for="(r, i) in ranking" :key="r.id || i" style="display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--border)">
          <div style="font-size: 18px; font-weight: 700; width: 28px; text-align: center" :style="{ color: i < 3 ? 'var(--warning)' : 'var(--text2)' }">{{ i + 1 }}</div>
          <div style="flex: 1">
            <div style="font-weight: 600; font-size: 14px">{{ r.nickname || r.username }}</div>
            <div style="font-size: 12px; color: var(--text2)">Lv.{{ r.level }}</div>
          </div>
          <div style="font-weight: 600; color: var(--primary)">{{ r.points }}</div>
        </div>
      </div>
      <div v-if="logs.length > 0" class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">📜 {{ t('points.history') }}</div>
        <div v-for="r in logs" :key="r.id" style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border)">
          <div>
            <span style="margin-right: 6px">{{ typeIcon[r.type] || '📌' }}</span>
            <span style="font-size: 13px">{{ r.remark || r.type }}</span>
            <div style="font-size: 11px; color: var(--text2)">{{ timeAgo(r.createdAt) }}</div>
          </div>
          <div style="font-weight: 600" :style="{ color: r.points > 0 ? 'var(--success)' : 'var(--danger)' }">{{ r.points > 0 ? '+' : '' }}{{ r.points }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Points' })
import { ref, onMounted } from 'vue'
import { pointApi } from '@/api'
import { ElMessage } from 'element-plus'
import { timeAgo } from '@/utils'
import { useI18n } from '@/composables/useI18n'
import { getErrorMessage } from '@/utils/error'

const { t } = useI18n()

const loading = ref(true)
const checkinLoading = ref(false)
const pointData = ref<any>(null)
const ranking = ref<any[]>([])
const logs = ref<any[]>([])

const typeIcon: Record<string, string> = {
  DAILY_CHECKIN: '📅', PUBLISH_PROJECT: '📝', RECEIVE_LIKE: '❤️',
  AC_SUBMISSION: '✅', COMPLETE_HOMEWORK: '📚', ADMIN_ADJUST: '⚙️'
}

async function doCheckin() {
  checkinLoading.value = true
  try {
    const res = await pointApi.checkin()
    if (res.code === 0) {
      if (res.data?.checkedIn) { ElMessage.success(`签到成功！+${res.data.earned} 积分`); await loadData() }
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
@media (max-width: 480px) {
  :deep(.el-progress) { width: 80% !important; }
}
</style>
