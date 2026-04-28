<template>
  <div>
    <h1 class="page-title">🏅 {{ t('nav.rank') }}</h1>

    <LoadingSkeleton v-if="loading" :count="10" variant="rank" />

    <template v-else>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div class="page-card">
            <h3 style="margin-bottom: 12px">🏆 {{ t('points.ranking') }}</h3>
            <EmptyState v-if="weeklyRank.length === 0" icon="📊" :text="t('common.empty')" />
            <div v-for="(r, i) in weeklyRank" :key="r.id || i" class="rank-item">
              <div class="rank-num" :class="{ 'rank-top': i < 3 }">{{ i + 1 }}</div>
              <el-avatar :size="32" :src="r.avatarUrl">{{ (r.nickname || r.username || '?')[0] }}</el-avatar>
              <div style="flex: 1">
                <div class="rank-name">{{ r.nickname || r.username }}</div>
              </div>
              <div class="rank-score">❤️ {{ r.likeCount ?? 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="page-card">
            <h3 style="margin-bottom: 12px">🏅 {{ t('points.ranking') }}</h3>
            <EmptyState v-if="monthlyRank.length === 0" icon="📊" :text="t('common.empty')" />
            <div v-for="(r, i) in monthlyRank" :key="r.id || i" class="rank-item">
              <div class="rank-num" :class="{ 'rank-top': i < 3 }">{{ i + 1 }}</div>
              <el-avatar :size="32" :src="r.avatarUrl">{{ (r.nickname || r.username || '?')[0] }}</el-avatar>
              <div style="flex: 1">
                <div class="rank-name">{{ r.nickname || r.username }}</div>
              </div>
              <div class="rank-score">❤️ {{ r.likeCount ?? 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Rank' })

import { ref, onMounted } from 'vue'
import { socialApi } from '@/api'
import { useI18n } from '@/composables/useI18n'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const { t } = useI18n()

const loading = ref(true)
const weeklyRank = ref<any[]>([])
const monthlyRank = ref<any[]>([])

onMounted(async () => {
  try {
    const [w, m] = await Promise.all([socialApi.getWeeklyRank(10), socialApi.getMonthlyRank(10)])
    weeklyRank.value = w.code === 0 ? (w.data || []) : []
    monthlyRank.value = m.code === 0 ? (m.data || []) : []
  } catch { /* 忽略 */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}

.rank-num {
  font-size: 16px;
  font-weight: 700;
  width: 28px;
  text-align: center;
  color: var(--text2);
}

.rank-num.rank-top {
  color: var(--warning);
}

.rank-name {
  font-weight: 600;
  font-size: 14px;
}

.rank-score {
  font-weight: 600;
  color: var(--primary);
  font-size: 14px;
}

@media (max-width: 480px) {
  .rank-name { font-size: 13px; }
  .rank-score { font-size: 13px; }
}
</style>
