<template>
  <div>
    <div class="back-link" @click="router.push('/competition')">← {{ t('common.back') }}</div>
    <LoadingSkeleton v-if="loading" variant="detail" />
    <template v-else-if="comp">
      <div class="page-card">
        <div style="font-size: 20px; font-weight: 700; margin-bottom: 8px">{{ comp.title }}</div>
        <div class="card-meta" style="margin-bottom: 10px">
          <el-tag :type="statusType(comp.status)" size="small">{{ statusLabel[comp.status] }}</el-tag>
          <span>{{ comp.type === 'RATED' ? '📊 排名赛' : '⏱️ 限时赛' }}</span>
          <span>📝 {{ comp.problemCount }} 题</span>
          <span>👥 {{ comp.participantCount }} 人</span>
          <span>满分 {{ comp.totalScore }}</span>
        </div>
        <p v-if="comp.description" style="font-size: 14px; color: var(--text2)">{{ comp.description }}</p>
        <div style="font-size: 13px; color: var(--text2); margin-top: 8px">
          ⏰ {{ new Date(comp.startTime).toLocaleString() }} ~ {{ new Date(comp.endTime).toLocaleString() }}
        </div>
      </div>
      <div v-if="userStore.isLoggedIn && comp.status === 'PUBLISHED' && !comp.registered" class="page-card" style="text-align: center">
        <el-button type="primary" @click="doRegister" :loading="registerLoading">📢 {{ t('competition.register') }}</el-button>
      </div>
      <div v-else-if="comp.registered && comp.status !== 'ENDED'" class="page-card" style="text-align: center; color: var(--success)">✅ {{ t('competition.registered') }}</div>
      <div v-if="rankings.length > 0" class="page-card">
        <div style="font-weight: 600; margin-bottom: 10px">🏅 {{ t('competition.ranking') }}</div>
        <el-table :data="rankings" stripe size="small">
          <el-table-column label="#" width="50">
            <template #default="{ $index }"><span :style="{ color: $index < 3 ? 'var(--warning)' : 'var(--text2)', fontWeight: 700 }">{{ $index + 1 }}</span></template>
          </el-table-column>
          <el-table-column label="选手"><template #default="{ row }">{{ row.nickname || row.username }}</template></el-table-column>
          <el-table-column label="得分" width="80"><template #default="{ row }"><span style="font-weight: 600; color: var(--primary)">{{ row.totalScore }}</span></template></el-table-column>
          <el-table-column label="通过" width="80"><template #default="{ row }">{{ row.solvedCount }}/{{ comp.problemCount }}</template></el-table-column>
          <el-table-column label="罚时" width="80"><template #default="{ row }">{{ row.penalty }}min</template></el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CompetitionDetailView' })
import { ref, onMounted } from 'vue'
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

const statusLabel: Record<string, string> = { DRAFT: t('competition.status.draft'), PUBLISHED: t('competition.status.published'), RUNNING: t('competition.status.running'), ENDED: t('competition.status.ended') }
function statusType(s: string): 'success' | '' | 'danger' | 'info' {
  const map: Record<string, 'success' | '' | 'danger'> = { RUNNING: 'success', PUBLISHED: '', ENDED: 'danger' }
  return map[s] || 'info'
}
const compId = Number(route.params.id)

async function doRegister() {
  registerLoading.value = true
  try {
    const res = await competitionApi.register(compId)
    if (res.code === 0) { ElMessage.success('报名成功！'); comp.value.registered = true; comp.value.participantCount++ }
    else ElMessage.error(res.msg)
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
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
  :deep(.el-table-column--selection) { width: 40px !important; }
}

@media (max-width: 480px) {
  :deep(.el-table) { font-size: 11px; }
  :deep(.el-table .el-table__header-wrapper) { overflow-x: auto; }
}
</style>
