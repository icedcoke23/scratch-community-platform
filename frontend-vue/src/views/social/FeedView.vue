<template>
  <div>
    <!-- 轮播图 -->
    <Carousel 
      :slides="carouselSlides" 
      :autoplay="true" 
      :interval="6000"
      @button-click="handleCarouselButtonClick"
    />
    
    <!-- 平台统计横幅 -->
    <div v-if="stats" class="stats-banner">
      <div class="stat-highlight">
        <span class="stat-emoji">👦👧</span>
        <div class="stat-content">
          <span class="stat-number">{{ stats.totalUsers }}</span>
          <span class="stat-label">位小创作者</span>
        </div>
      </div>
      <div class="stat-highlight">
        <span class="stat-emoji">🎨</span>
        <div class="stat-content">
          <span class="stat-number">{{ stats.publishedProjects }}</span>
          <span class="stat-label">个精彩作品</span>
        </div>
      </div>
      <div class="stat-highlight">
        <span class="stat-emoji">🚀</span>
        <div class="stat-content">
          <span class="stat-number">{{ stats.totalProjects }}</span>
          <span class="stat-label">个项目诞生</span>
        </div>
      </div>
    </div>

    <div class="feed-header">
      <h1 class="page-title">
        <span class="title-emoji">🌟</span>
        {{ t('feed.title') }}
      </h1>
      <div class="feed-controls">
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          size="large"
          class="create-btn"
          @click="router.push('/editor')"
        >
          ✏️ 创建作品
        </el-button>
        <el-input
          v-model="searchQuery"
          :placeholder="t('common.search') + '...'"
          clearable
          class="search-input"
          size="large"
          @keyup.enter="doSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-radio-group v-model="sort" size="large" @change="loadFeed(true)" class="sort-group">
          <el-radio-button value="latest">🆕 {{ t('feed.latest') }}</el-radio-button>
          <el-radio-button value="hot">🔥 {{ t('feed.hot') }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 搜索结果提示 -->
    <div v-if="isSearching" class="search-hint">
      <span>🔍 搜索 "{{ lastSearch }}" 的结果（{{ total }} 条）</span>
      <el-button text type="primary" size="large" @click="clearSearch">清除搜索</el-button>
    </div>

    <LoadingSkeleton v-if="loading && projects.length === 0" :count="6" variant="card" />
    <EmptyState v-else-if="projects.length === 0 && !loading" icon="🎨" :text="isSearching ? t('common.empty') : t('feed.empty')" />

    <div v-else class="project-grid">
      <ProjectCard
        v-for="p in projects"
        :key="p.id"
        :project="p"
        @click="router.push(`/project/${p.id}`)"
      />
    </div>

    <div v-if="hasMore" class="load-more">
      <el-button size="large" :loading="loading" @click="loadMore" class="load-more-btn">
        {{ loading ? '加载中...' : '📦 加载更多作品' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Feed' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { socialApi, statsApi, getAbortController, clearAbortController } from '@/api'
import { Search } from '@element-plus/icons-vue'
import type { Project } from '@/types'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores/user'
import ProjectCard from '@/components/ProjectCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import Carousel from '@/components/Carousel.vue'

const { t } = useI18n()

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const projects = ref<Project[]>([])
const sort = ref('latest')
const page = ref(1)
const total = ref(0)
const hasMore = computed(() => projects.value.length < total.value)

// 搜索
const searchQuery = ref('')
const lastSearch = ref('')
const isSearching = ref(false)

// 轮播图数据
interface CarouselSlide {
  image: string
  title: string
  description: string
  buttonText?: string
  link?: string
}

const carouselSlides = ref<CarouselSlide[]>([
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=coding%20scratch%20kids%20learning%20programming%20colorful%20fun%20educational%20technology&image_size=landscape_16_9',
    title: '🎉 欢迎来到 Scratch 社区',
    description: '在这里，你可以发挥想象力，创造令人惊叹的项目，与全球小创作者一起学习编程！',
    buttonText: '开始创作',
    link: '/editor'
  },
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=competition%20programming%20contest%20awards%20trophy%20winner%20celebration%20kids%20coding&image_size=landscape_16_9',
    title: '🏆 编程竞赛火热进行中',
    description: '参加我们的月度编程竞赛，展示你的创意，赢取丰厚奖励！',
    buttonText: '立即参赛',
    link: '/competition'
  },
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=classroom%20teacher%20students%20learning%20together%20education%20school%20collaboration&image_size=landscape_16_9',
    title: '📚 加入学习班级',
    description: '创建或加入学习班级，与老师和同学们一起学习，共同进步！',
    buttonText: '探索班级',
    link: '/class'
  },
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=friends%20collaboration%20teamwork%20coding%20together%20happy%20kids%20programming%20group&image_size=landscape_16_9',
    title: '👥 多人协作编辑',
    description: '邀请小伙伴一起合作开发项目，实时协作，激发无限创意！',
    buttonText: '了解更多',
    link: '/feed'
  }
])

// 统计
const stats = ref<{ totalUsers: number; totalProjects: number; publishedProjects: number } | null>(null)

// 轮播图按钮点击处理
function handleCarouselButtonClick(slide: CarouselSlide) {
  if (slide.link && slide.link.startsWith('/')) {
    router.push(slide.link)
  }
}

async function loadFeed(reset = false) {
  if (reset) {
    page.value = 1
    projects.value = []
  }
  loading.value = true
  try {
    const res = await socialApi.getFeed(sort.value, page.value, 20)
    if (res.code === 0 && res.data) {
      if (reset) {
        projects.value = res.data.records || []
      } else {
        projects.value.push(...(res.data.records || []))
      }
      total.value = res.data.total || 0
    }
  } catch { /* 忽略 */ }
  finally { loading.value = false }
}

async function doSearch() {
  const q = searchQuery.value.trim()
  if (!q) return clearSearch()
  lastSearch.value = q
  isSearching.value = true
  page.value = 1
  loading.value = true

  const controller = getAbortController('feed-search')
  try {
    const res = await socialApi.search(q, 1, 20)
    if (res.code === 0 && res.data) {
      projects.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e: unknown) {
    if (e instanceof Error && e.name === 'CanceledError') return
  } finally {
    loading.value = false
    clearAbortController('feed-search')
  }
}

function clearSearch() {
  searchQuery.value = ''
  lastSearch.value = ''
  isSearching.value = false
  loadFeed(true)
}

function loadMore() {
  page.value++
  if (isSearching.value) {
    loading.value = true
    socialApi.search(lastSearch.value, page.value, 20)
      .then(res => {
        if (res.code === 0 && res.data) {
          projects.value.push(...(res.data.records || []))
          total.value = res.data.total || 0
        }
      })
      .finally(() => { loading.value = false })
  } else {
    loadFeed()
  }
}

async function loadStats() {
  try {
    const res = await statsApi.getStats()
    if (res.code === 0 && res.data) {
      stats.value = res.data
    }
  } catch { /* 忽略 */ }
}

onMounted(() => {
  loadFeed()
  loadStats()
})

onUnmounted(() => {
  clearAbortController('feed-search')
})
</script>

<style scoped>
.stats-banner {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 24px 32px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #3B82F6 0%, #8B5CF6 50%, #EC4899 100%);
  border-radius: 20px;
  color: #fff;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.3);
  flex-wrap: wrap;
}

.stat-highlight {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(255,255,255,0.15);
  border-radius: 14px;
  backdrop-filter: blur(8px);
  transition: transform 0.2s ease;
}

.stat-highlight:hover {
  transform: scale(1.05);
}

.stat-emoji {
  font-size: 32px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  opacity: 0.9;
  font-weight: 500;
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-emoji {
  font-size: 28px;
  animation: title-wave 2s ease-in-out infinite;
}

@keyframes title-wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(10deg); }
  75% { transform: rotate(-10deg); }
}

.feed-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.create-btn {
  font-weight: 700;
  border-radius: 14px;
  min-height: 44px;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple, #8B5CF6));
  border: none;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.search-input {
  width: 260px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.sort-group :deep(.el-radio-button__inner) {
  border-radius: 10px;
  font-weight: 600;
}

.search-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: var(--text2);
  font-size: 15px;
  padding: 12px 16px;
  background: var(--primary-bg);
  border-radius: 12px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.load-more {
  text-align: center;
  padding: 28px;
}

.load-more-btn {
  min-width: 200px;
  border-radius: 14px;
  font-weight: 600;
  font-size: 16px;
}

@media (max-width: 768px) {
  .project-grid { grid-template-columns: 1fr; }
  .feed-header { flex-direction: column; align-items: stretch; }
  .feed-controls { flex-direction: column; }
  .search-input { width: 100%; }
  .stats-banner { gap: 12px; padding: 16px; }
  .stat-emoji { font-size: 24px; }
  .stat-number { font-size: 20px; }
  .stat-highlight { padding: 6px 12px; }
}

@media (max-width: 480px) {
  .stats-banner { flex-direction: column; gap: 8px; }
  .stat-highlight { justify-content: center; }
}
</style>
