<template>
  <div>
    <!-- 平台统计横幅 -->
    <div v-if="stats" class="stats-banner">
      <span class="stat-item"><strong>{{ stats.totalUsers }}</strong> 位创作者</span>
      <span class="stat-item"><strong>{{ stats.publishedProjects }}</strong> 个作品</span>
      <span class="stat-item"><strong>{{ stats.totalProjects }}</strong> 个项目</span>
    </div>

    <div class="feed-header">
      <h1 class="page-title">{{ t('feed.title') }}</h1>
      <div class="feed-controls">
        <el-input
          v-model="searchQuery"
          :placeholder="t('common.search') + '...'"
          clearable
          class="search-input"
          @keyup.enter="doSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-radio-group v-model="sort" size="small" @change="loadFeed(true)">
          <el-radio-button value="latest">{{ t('feed.latest') }}</el-radio-button>
          <el-radio-button value="hot">{{ t('feed.hot') }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 搜索结果提示 -->
    <div v-if="isSearching" class="search-hint">
      <span>搜索 "{{ lastSearch }}" 的结果（{{ total }} 条）</span>
      <el-button text type="primary" @click="clearSearch">清除搜索</el-button>
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
      <el-button :loading="loading" @click="loadMore">加载更多</el-button>
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
import ProjectCard from '@/components/ProjectCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()

const router = useRouter()
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

// 统计
const stats = ref<{ totalUsers: number; totalProjects: number; publishedProjects: number } | null>(null)

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

  // 取消上一次搜索请求
  const controller = getAbortController('feed-search')
  try {
    const res = await socialApi.search(q, 1, 20)
    if (res.code === 0 && res.data) {
      projects.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e: unknown) {
    if (e instanceof Error && e.name === 'CanceledError') return // 请求被取消，忽略
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
    // 搜索翻页
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
  gap: 32px;
  padding: 16px 24px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}
.stat-item { font-size: 14px; }
.stat-item strong { font-size: 20px; margin-right: 4px; }

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.feed-controls { display: flex; gap: 12px; align-items: center; }
.search-input { width: 220px; }

.search-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
.load-more { text-align: center; padding: 20px; }
@media (max-width: 768px) {
  .project-grid { grid-template-columns: 1fr; }
  .feed-header { flex-direction: column; align-items: stretch; }
  .feed-controls { flex-direction: column; }
  .search-input { width: 100%; }
  .stats-banner { flex-wrap: wrap; gap: 16px; }
}
</style>
