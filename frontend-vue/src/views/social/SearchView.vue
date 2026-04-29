<template>
  <div class="search-page">
    <h1 class="page-title">
      <span class="title-emoji">🔍</span>
      {{ t('nav.search') }}
    </h1>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="query"
        placeholder="搜索项目、创作者..."
        size="large"
        clearable
        @keyup.enter="doSearch"
        @clear="results = null"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="doSearch" :loading="loading" size="large">{{ t('common.search') }}</el-button>
        </template>
      </el-input>
    </div>

    <!-- 热门搜索 -->
    <div v-if="!results" class="hot-searches">
      <div class="section-label">🔥 热门搜索</div>
      <div class="hot-tags">
        <span
          v-for="tag in hotTags"
          :key="tag"
          class="hot-tag"
          @click="query = tag; doSearch()"
        >
          {{ tag }}
        </span>
      </div>
    </div>

    <!-- 搜索结果 -->
    <template v-if="results">
      <div class="result-info">
        🔍 找到 <strong>{{ total }}</strong> 个相关项目
        <span v-if="query" class="result-query">（关键词：{{ query }}）</span>
      </div>

      <EmptyState v-if="results.length === 0" icon="🔍" text="没有找到相关项目，换个关键词试试？" />

      <div v-else class="result-grid">
        <ProjectCard
          v-for="project in results"
          :key="project.id"
          :project="project"
          @click="router.push(`/project/${project.id}`)"
        />
      </div>

      <div v-if="total > 20" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="20"
          :total="total"
          layout="prev, pager, next"
          @current-change="doSearch"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SearchView' })
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { socialApi } from '@/api'
import { useI18n } from '@/composables/useI18n'
import ProjectCard from '@/components/ProjectCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { Project } from '@/types'

const { t } = useI18n()

const router = useRouter()

const query = ref('')
const loading = ref(false)
const results = ref<Project[] | null>(null)
const total = ref(0)
const page = ref(1)

const hotTags = [
  '🎮 游戏', '🎬 动画', '🎵 音乐', '🎨 画画', '🏰 迷宫',
  '🚀 射击', '⚔️ 冒险', '📖 教育', '🧮 数学', '📚 故事'
]

async function doSearch() {
  const q = query.value.trim()
  if (!q) return

  loading.value = true
  try {
    const res = await socialApi.search(q, page.value, 20)
    if (res.code === 0 && res.data) {
      results.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      results.value = []
      total.value = 0
    }
  } catch {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-page {
  max-width: 860px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 28px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.hot-searches {
  text-align: center;
  padding: 20px;
  background: var(--card);
  border-radius: 16px;
  border: 1px solid var(--border);
}

.section-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 16px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.hot-tag {
  cursor: pointer;
  padding: 8px 16px;
  background: var(--primary-bg);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--primary);
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.hot-tag:hover {
  background: var(--primary);
  color: #fff;
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.result-info {
  font-size: 15px;
  color: var(--text2);
  margin-bottom: 20px;
  padding: 10px 16px;
  background: var(--primary-bg);
  border-radius: 12px;
}

.result-query {
  color: var(--text3);
  font-size: 14px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.pagination-wrapper {
  margin-top: 24px;
  text-align: center;
}

@media (max-width: 768px) {
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
