<template>
  <div class="achievements-page">
    <h1 class="page-title">
      <span class="title-emoji">🏆</span>
      {{ t('achievements.title') }}
    </h1>

    <!-- 成就概览 -->
    <div class="page-card achievement-hero">
      <div class="achievement-summary">
        <div class="summary-item">
          <div class="summary-icon">🎯</div>
          <div class="summary-value">{{ unlockedCount }}</div>
          <div class="summary-label">{{ t('achievements.unlocked') }}</div>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <div class="summary-icon">📦</div>
          <div class="summary-value">{{ totalCount }}</div>
          <div class="summary-label">{{ t('achievements.total') }}</div>
        </div>
        <div class="summary-divider"></div>
        <div class="summary-item">
          <div class="summary-icon">📈</div>
          <div class="summary-value">{{ progressPercent }}%</div>
          <div class="summary-label">{{ t('achievements.progress') }}</div>
        </div>
      </div>
      <el-progress
        :percentage="progressPercent"
        :stroke-width="12"
        :color="progressColors"
        style="margin-top: 16px"
      />
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <el-radio-group v-model="activeCategory" size="large">
        <el-radio-button value="all">🌟 {{ t('achievements.all') }}</el-radio-button>
        <el-radio-button value="create">🎨 {{ t('achievements.create') }}</el-radio-button>
        <el-radio-button value="social">👥 {{ t('achievements.social') }}</el-radio-button>
        <el-radio-button value="learn">📚 {{ t('achievements.learn') }}</el-radio-button>
        <el-radio-button value="special">✨ {{ t('achievements.special') }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 成就列表 -->
    <div class="achievement-grid">
      <div
        v-for="achievement in filteredAchievements"
        :key="achievement.id"
        class="achievement-card"
        :class="{ unlocked: achievement.unlocked, locked: !achievement.unlocked }"
      >
        <div class="achievement-icon-wrapper" :class="{ 'glow': achievement.unlocked }">
          <span class="achievement-icon">{{ achievement.icon }}</span>
        </div>
        <div class="achievement-info">
          <div class="achievement-name">{{ achievement.name }}</div>
          <div class="achievement-desc">{{ achievement.description }}</div>
          <div v-if="achievement.unlockedAt" class="achievement-time">
            ✅ {{ formatDate(achievement.unlockedAt) }}
          </div>
        </div>
        <div v-if="!achievement.unlocked" class="achievement-lock">🔒</div>
        <div v-else class="achievement-check">✅</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Achievements' })

import { ref, computed } from 'vue'
import { formatDate } from '@/utils'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

interface Achievement {
  id: string
  name: string
  description: string
  icon: string
  category: 'create' | 'social' | 'learn' | 'special'
  unlocked: boolean
  unlockedAt?: string
}

const activeCategory = ref('all')

const achievements = ref<Achievement[]>([
  { id: 'first-project', name: '初出茅庐', description: '创建第一个项目', icon: '🎮', category: 'create', unlocked: true, unlockedAt: '2026-04-20T10:00:00' },
  { id: 'five-projects', name: '小有成就', description: '创建 5 个项目', icon: '🎯', category: 'create', unlocked: true, unlockedAt: '2026-04-22T14:00:00' },
  { id: 'ten-projects', name: '创作达人', description: '创建 10 个项目', icon: '🌟', category: 'create', unlocked: false },
  { id: 'publish-first', name: '公之于众', description: '发布第一个项目', icon: '📢', category: 'create', unlocked: true, unlockedAt: '2026-04-21T09:00:00' },
  { id: 'remix-first', name: '站在巨人肩上', description: '第一次 Remix 项目', icon: '🔄', category: 'create', unlocked: false },
  { id: 'first-like', name: '初次心动', description: '收到第一个点赞', icon: '❤️', category: 'social', unlocked: true, unlockedAt: '2026-04-21T11:00:00' },
  { id: 'ten-likes', name: '人气新星', description: '累计收到 10 个点赞', icon: '💖', category: 'social', unlocked: false },
  { id: 'first-comment', name: '畅所欲言', description: '发表第一条评论', icon: '💬', category: 'social', unlocked: true, unlockedAt: '2026-04-22T16:00:00' },
  { id: 'share-first', name: '分享达人', description: '第一次分享项目', icon: '📤', category: 'social', unlocked: false },
  { id: 'first-submission', name: '勇敢尝试', description: '第一次提交判题', icon: '📝', category: 'learn', unlocked: true, unlockedAt: '2026-04-20T15:00:00' },
  { id: 'first-ac', name: '初战告捷', description: '第一次通过判题', icon: '✅', category: 'learn', unlocked: true, unlockedAt: '2026-04-20T15:30:00' },
  { id: 'ten-ac', name: '判题高手', description: '累计通过 10 道题', icon: '🏆', category: 'learn', unlocked: false },
  { id: 'first-homework', name: '好学生', description: '完成第一次作业', icon: '📚', category: 'learn', unlocked: false },
  { id: 'daily-checkin', name: '坚持不懈', description: '连续签到 7 天', icon: '📅', category: 'special', unlocked: false },
  { id: 'level-5', name: '小有级别', description: '达到 5 级', icon: '⭐', category: 'special', unlocked: false },
  { id: 'competition-first', name: '竞赛选手', description: '第一次参加竞赛', icon: '🏅', category: 'special', unlocked: false },
  { id: 'ai-review', name: 'AI 认证', description: '获得第一次 AI 点评', icon: '🤖', category: 'special', unlocked: true, unlockedAt: '2026-04-23T10:00:00' },
])

const progressColors = [
  { color: '#F59E0B', percentage: 30 },
  { color: '#3B82F6', percentage: 60 },
  { color: '#22C55E', percentage: 100 },
]

const filteredAchievements = computed(() => {
  if (activeCategory.value === 'all') return achievements.value
  return achievements.value.filter(a => a.category === activeCategory.value)
})

const unlockedCount = computed(() => achievements.value.filter(a => a.unlocked).length)
const totalCount = computed(() => achievements.value.length)
const progressPercent = computed(() => Math.round((unlockedCount.value / totalCount.value) * 100))
</script>

<style scoped>
.achievement-hero {
  padding: 24px;
  background: linear-gradient(135deg, var(--primary-bg), #FEF3C7);
}

.achievement-summary {
  display: flex;
  justify-content: space-around;
  align-items: center;
  text-align: center;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.summary-icon {
  font-size: 28px;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--primary);
  line-height: 1;
}

.summary-label {
  font-size: 13px;
  color: var(--text2);
  font-weight: 500;
}

.summary-divider {
  width: 1px;
  height: 48px;
  background: var(--border);
}

.category-tabs {
  margin: 20px 0;
}

.category-tabs :deep(.el-radio-button__inner) {
  border-radius: 10px;
  font-weight: 600;
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.achievement-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--card);
  border: 2px solid var(--border);
  border-radius: 16px;
  transition: all 0.2s ease;
}

.achievement-card.unlocked {
  border-color: var(--primary-light);
  background: var(--primary-bg);
}

.achievement-card.locked {
  opacity: 0.55;
}

.achievement-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.achievement-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bg);
}

.achievement-icon-wrapper.glow {
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  box-shadow: 0 2px 12px rgba(234, 179, 8, 0.3);
}

.achievement-icon {
  font-size: 28px;
}

.achievement-icon-wrapper.glow .achievement-icon {
  animation: icon-pulse 2s ease-in-out infinite;
}

@keyframes icon-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.achievement-info {
  flex: 1;
  min-width: 0;
}

.achievement-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.achievement-desc {
  font-size: 13px;
  color: var(--text2);
  margin-top: 2px;
}

.achievement-time {
  font-size: 12px;
  color: var(--success);
  margin-top: 4px;
  font-weight: 500;
}

.achievement-lock,
.achievement-check {
  font-size: 20px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .achievement-grid { grid-template-columns: 1fr; }
  .achievement-summary { gap: 12px; }
  .summary-value { font-size: 26px; }
  .summary-divider { height: 36px; }
}

@media (max-width: 480px) {
  .achievement-card { padding: 12px; gap: 10px; }
  .achievement-icon-wrapper { width: 40px; height: 40px; }
  .achievement-icon { font-size: 22px; }
}
</style>
