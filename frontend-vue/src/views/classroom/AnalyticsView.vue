<template>
  <div>
    <h1 class="page-title">📊 {{ t('analytics.title') }}</h1>
    <LoadingSkeleton v-if="loading" :count="3" variant="card" />
    <EmptyState v-else-if="classes.length === 0" icon="📊" :text="t('common.empty')" />
    <template v-else>
      <el-select v-model="selectedClassId" :placeholder="t('analytics.selectClass')" style="width: 100%; margin-bottom: 14px" @change="loadAnalytics">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <div v-if="analytics" :key="selectedClassId || undefined">
        <el-row :gutter="12" style="margin-bottom: 14px">
          <el-col :span="8"><el-statistic title="学生人数" :value="analytics.studentCount" /></el-col>
          <el-col :span="8"><el-statistic title="作业总数" :value="analytics.homeworkCount" /></el-col>
          <el-col :span="8"><el-statistic title="7日活跃" :value="analytics.activeStudents7d" /></el-col>
        </el-row>
        <el-row :gutter="12" style="margin-bottom: 14px">
          <el-col :span="12"><el-statistic title="平均提交率" :value="Number(analytics.avgSubmitRate?.toFixed(1))" suffix="%" /></el-col>
          <el-col :span="12"><el-statistic title="平均分" :value="Number(analytics.avgScore?.toFixed(1))" /></el-col>
        </el-row>
        <div v-if="analytics.typePassRates && Object.keys(analytics.typePassRates).length > 0" class="page-card">
          <div style="font-weight: 600; margin-bottom: 10px">📊 题目类型通过率</div>
          <div v-for="(rate, type) in analytics.typePassRates" :key="type" style="display: flex; justify-content: space-between; padding: 6px 0">
            <span>{{ type }}</span>
            <el-tag :type="(rate as number) >= 60 ? 'success' : 'danger'" size="small">{{ (rate as number).toFixed(1) }}%</el-tag>          </div>
        </div>
        <div v-if="analytics.homeworkStats?.length" class="page-card">
          <div style="font-weight: 600; margin-bottom: 10px">📝 作业统计</div>
          <el-table :data="analytics.homeworkStats" stripe size="small">
            <el-table-column prop="title" label="作业" />
            <el-table-column label="提交率" width="100"><template #default="{ row }"><el-tag :type="row.submitRate >= 80 ? 'success' : row.submitRate >= 50 ? 'info' : 'danger'" size="small">{{ row.submitRate?.toFixed(1) }}%</el-tag></template></el-table-column>
            <el-table-column label="平均分" width="80"><template #default="{ row }">{{ row.avgScore?.toFixed(1) }}</template></el-table-column>
            <el-table-column label="已批改" width="80"><template #default="{ row }">{{ row.gradedCount }}/{{ row.submitCount }}</template></el-table-column>
          </el-table>
        </div>
        <div v-if="analytics.studentRanks?.length" class="page-card">
          <div style="font-weight: 600; margin-bottom: 10px">🏅 学生排名（按平均分）</div>
          <el-table :data="analytics.studentRanks" stripe size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column label="学生"><template #default="{ row }">{{ row.nickname || row.username }}</template></el-table-column>
            <el-table-column label="等级" width="80"><template #default="{ row }">Lv.{{ row.level }}</template></el-table-column>
            <el-table-column label="提交率" width="100"><template #default="{ row }">{{ row.submitRate?.toFixed(1) }}%</template></el-table-column>
            <el-table-column label="平均分" width="80"><template #default="{ row }">{{ row.avgScore?.toFixed(1) }}</template></el-table-column>
            <el-table-column label="积分" width="80"><template #default="{ row }">{{ row.totalPoints }}</template></el-table-column>
          </el-table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AnalyticsView' })
import { ref, onMounted } from 'vue'
import { userApi, analyticsApi } from '@/api'
import type { AnalyticsData, ClassRoom } from '@/types'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()

const loading = ref(true)
const classes = ref<ClassRoom[]>([])
const selectedClassId = ref<number | null>(null)
const analytics = ref<AnalyticsData | null>(null)

async function loadAnalytics(classId: number) {
  try {
    const res = await analyticsApi.getClassAnalytics(classId)
    if (res.code === 0) analytics.value = res.data || null
  } catch { /* 忽略 */ }
}

onMounted(async () => {
  try {
    const res = await userApi.getMyClasses()
    if (res.code === 0 && res.data && res.data.length > 0) {
      classes.value = res.data
      selectedClassId.value = res.data[0].id
      await loadAnalytics(res.data[0].id)
    }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
@media (max-width: 768px) {
  :deep(.el-table) { font-size: 12px; }
  :deep(.el-table .cell) { padding: 6px 4px; }
  :deep(.el-col) { margin-bottom: 8px; }
}
</style>
