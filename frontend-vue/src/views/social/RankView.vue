<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">🏅</span>
      {{ t('nav.rank') }}
    </h1>

    <LoadingSkeleton v-if="loading" :count="10" variant="rank" />

    <template v-else>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div class="page-card rank-card">
            <h3 class="rank-card-title">
              <span class="rank-card-icon">🔥</span>
              本周人气榜
            </h3>
            <EmptyState v-if="weeklyRank.length === 0" icon="📊" :text="t('common.empty')" />
            <div v-for="(r, i) in weeklyRank" :key="r.id || i" class="rank-item" :class="{ 'rank-top-3': i < 3 }">
              <div class="rank-medal" :class="medalClass(i)">
                {{ medalEmoji(i) }}
              </div>
              <div class="rank-avatar" :class="avatarColorClass(i)">
                {{ (r.nickname || r.username || '?')[0] }}
              </div>
              <div class="rank-info">
                <div class="rank-name">{{ r.nickname || r.username }}</div>
                <div class="rank-detail">
                  <span class="rank-likes">❤️ {{ r.likeCount ?? 0 }}</span>
                </div>
              </div>
              <div v-if="i < 3" class="rank-sparkle">✨</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="page-card rank-card">
            <h3 class="rank-card-title">
              <span class="rank-card-icon">🌟</span>
              本月人气榜
            </h3>
            <EmptyState v-if="monthlyRank.length === 0" icon="📊" :text="t('common.empty')" />
            <div v-for="(r, i) in monthlyRank" :key="r.id || i" class="rank-item" :class="{ 'rank-top-3': i < 3 }">
              <div class="rank-medal" :class="medalClass(i)">
                {{ medalEmoji(i) }}
              </div>
              <div class="rank-avatar" :class="avatarColorClass(i)">
                {{ (r.nickname || r.username || '?')[0] }}
              </div>
              <div class="rank-info">
                <div class="rank-name">{{ r.nickname || r.username }}</div>
                <div class="rank-detail">
                  <span class="rank-likes">❤️ {{ r.likeCount ?? 0 }}</span>
                </div>
              </div>
              <div v-if="i < 3" class="rank-sparkle">✨</div>
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

function medalClass(index: number): string {
  if (index === 0) return 'medal-gold'
  if (index === 1) return 'medal-silver'
  if (index === 2) return 'medal-bronze'
  return 'medal-default'
}

function medalEmoji(index: number): string {
  if (index === 0) return '🥇'
  if (index === 1) return '🥈'
  if (index === 2) return '🥉'
  return `${index + 1}`
}

function avatarColorClass(index: number): string {
  const colors = ['avatar-gold', 'avatar-silver', 'avatar-bronze', 'avatar-blue', 'avatar-green']
  return colors[Math.min(index, colors.length - 1)]
}

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
.rank-card {
  padding: 20px;
}

.rank-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
}

.rank-card-icon {
  font-size: 24px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.rank-item:hover {
  background: var(--primary-bg);
  transform: translateX(4px);
}

.rank-item.rank-top-3 {
  background: linear-gradient(135deg, rgba(234, 179, 8, 0.05), rgba(234, 179, 8, 0.02));
  border-color: rgba(234, 179, 8, 0.2);
}

.rank-medal {
  font-size: 28px;
  width: 40px;
  text-align: center;
  flex-shrink: 0;
}

.medal-default {
  font-size: 16px;
  font-weight: 700;
  color: var(--text3);
}

.rank-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.avatar-gold { background: linear-gradient(135deg, #F59E0B, #D97706); }
.avatar-silver { background: linear-gradient(135deg, #9CA3AF, #6B7280); }
.avatar-bronze { background: linear-gradient(135deg, #D97706, #92400E); }
.avatar-blue { background: linear-gradient(135deg, #3B82F6, #2563EB); }
.avatar-green { background: linear-gradient(135deg, #22C55E, #16A34A); }

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-weight: 700;
  font-size: 15px;
  color: var(--text);
  margin-bottom: 2px;
}

.rank-detail {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--text2);
}

.rank-likes {
  font-weight: 600;
  color: var(--danger);
}

.rank-sparkle {
  font-size: 20px;
  animation: sparkle 1.5s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}

@media (max-width: 480px) {
  .rank-medal { font-size: 22px; width: 32px; }
  .rank-avatar { width: 32px; height: 32px; font-size: 14px; }
  .rank-name { font-size: 14px; }
  .rank-item { padding: 10px; gap: 8px; }
}
</style>
