<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">📊</span>
      {{ t('analytics.title') }}
    </h1>
    <LoadingSkeleton v-if="loading" :count="3" variant="card" />
    <EmptyState v-else-if="classes.length === 0" icon="📊" text="还没有班级数据" />
    <template v-else>
      <div class="class-selector">
        <el-select v-model="selectedClassId" :placeholder="t('analytics.selectClass')" size="large" style="width: 100%" @change="loadAnalytics">
          <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
      <div v-if="analytics" :key="selectedClassId || undefined">
        <!-- 核心指标 -->
        <div class="stats-grid">
          <div class="stat-card">
            <span class="stat-emoji">👦👧</span>
            <div class="stat-content">
              <div class="stat-number">{{ analytics.studentCount }}</div>
              <div class="stat-label">学生人数</div>
            </div>
          </div>
          <div class="stat-card">
            <span class="stat-emoji">📝</span>
            <div class="stat-content">
              <div class="stat-number">{{ analytics.homeworkCount }}</div>
              <div class="stat-label">作业总数</div>
            </div>
          </div>
          <div class="stat-card">
            <span class="stat-emoji">🔥</span>
            <div class="stat-content">
              <div class="stat-number">{{ analytics.activeStudents7d }}</div>
              <div class="stat-label">7日活跃</div>
            </div>
          </div>
          <div class="stat-card">
            <span class="stat-emoji">📊</span>
            <div class="stat-content">
              <div class="stat-number">{{ Number(analytics.avgSubmitRate?.toFixed(1)) }}%</div>
              <div class="stat-label">平均提交率</div>
            </div>
          </div>
          <div class="stat-card">
            <span class="stat-emoji">⭐</span>
            <div class="stat-content">
              <div class="stat-number">{{ Number(analytics.avgScore?.toFixed(1)) }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </div>
        </div>

        <!-- 题目类型通过率 -->
        <div v-if="analytics.typePassRates && Object.keys(analytics.typePassRates).length > 0" class="page-card">
          <h3 class="section-title">📊 题目类型通过率</h3>
          <div class="pass-rate-list">
            <div v-for="(rate, type) in analytics.typePassRates" :key="type" class="pass-rate-item">
              <span class="rate-name">{{ type }}</span>
              <div class="rate-bar-wrapper">
                <div class="rate-bar">
                  <div class="rate-fill" :class="{ 'rate-good': (rate as number) >= 60, 'rate-bad': (rate as number) < 60 }" :style="{ width: Math.min(rate as number, 100) + '%' }"></div>
                </div>
                <span class="rate-value" :class="{ 'rate-good-text': (rate as number) >= 60, 'rate-bad-text': (rate as number) < 60 }">{{ (rate as number).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 作业统计 -->
        <div v-if="analytics.homeworkStats?.length" class="page-card">
          <h3 class="section-title">📝 作业统计</h3>
          <el-table :data="analytics.homeworkStats" stripe size="large" style="border-radius: 12px; overflow: hidden">
            <el-table-column prop="title" label="作业" />
            <el-table-column label="提交率" width="120">
              <template #default="{ row }">
                <el-tag :type="row.submitRate >= 80 ? 'success' : row.submitRate >= 50 ? 'info' : 'danger'" size="default" style="border-radius: 8px">
                  {{ row.submitRate?.toFixed(1) }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="平均分" width="100">
              <template #default="{ row }">{{ row.avgScore?.toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="已批改" width="100">
              <template #default="{ row }">{{ row.gradedCount }}/{{ row.submitCount }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 学生排名 -->
        <div v-if="analytics.studentRanks?.length" class="page-card">
          <h3 class="section-title">🏅 学生排名（按平均分）</h3>
          <el-table :data="analytics.studentRanks" stripe size="large" style="border-radius: 12px; overflow: hidden">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column label="学生">
              <template #default="{ row }">{{ row.nickname || row.username }}</template>
            </el-table-column>
            <el-table-column label="等级" width="100">
              <template #default="{ row }">
                <span style="font-weight: 700; color: var(--primary)">Lv.{{ row.level }}</span>
              </template>
            </el-table-column>
            <el-table-column label="提交率" width="120">
              <template #default="{ row }">{{ row.submitRate?.toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column label="平均分" width="100">
              <template #default="{ row }">{{ row.avgScore?.toFixed(1) }}</template>
            </el-table-column>
            <el-table-column label="积分" width="100">
              <template #default="{ row }">
                <span style="font-weight: 700; color: var(--warning)">⭐ {{ row.totalPoints }}</span>
              </template>
            </el-table-column>
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
.class-selector {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--card);
  border: 2px solid var(--border);
  border-radius: 14px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: var(--primary-light);
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.stat-emoji {
  font-size: 28px;
}

.stat-number {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text2);
  font-weight: 500;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.pass-rate-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pass-rate-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rate-name {
  width: 80px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  flex-shrink: 0;
}

.rate-bar-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.rate-bar {
  flex: 1;
  height: 10px;
  background: var(--border);
  border-radius: 5px;
  overflow: hidden;
}

.rate-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease;
}

.rate-fill.rate-good { background: linear-gradient(90deg, var(--success), #4ADE80); }
.rate-fill.rate-bad { background: linear-gradient(90deg, var(--danger), #F87171); }

.rate-value {
  font-size: 14px;
  font-weight: 700;
  width: 50px;
  text-align: right;
}

.rate-good-text { color: var(--success); }
.rate-bad-text { color: var(--danger); }

@media (max-width: 768px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .rate-name { width: 60px; font-size: 13px; }
}
</style>
