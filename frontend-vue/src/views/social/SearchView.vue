<template>
  <div class="search-page">
    <h1 class="page-title">🔍 {{ t('nav.search') }}</h1>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="query"
        :placeholder="t('nav.search') + '...'"
        size="large"
        clearable
        @keyup.enter="doSearch"
        @clear="results = null"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="doSearch" :loading="loading">{{ t('common.search') }}</el-button>
        </template>
      </el-input>
    </div>

    <!-- 热门搜索 -->
    <div v-if="!results" class="hot-searches">
      <div class="section-label">{{ t('nav.search') }}</div>
      <div class="hot-tags">
        <el-tag
          v-for="tag in hotTags"
          :key="tag"
          class="hot-tag"
          @click="query = tag; doSearch()"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>

    <!-- 搜索结果 -->
    <template v-if="results">
      <div class="result-info">
        找到 <strong>{{ total }}</strong> 个相关项目
        <span v-if="query" class="result-query">（关键词：{{ query }}）</span>
      </div>

      <div v-if="results.length === 0" class="empty-state">
        <div style="font-size: 48px; margin-bottom: 12px">🔍</div>
        <p>{{ t('common.empty') }}</p>
      </div>

      <div v-else class="result-grid">
        <ProjectCard
          v-for="project in results"
          :key="project.id"
          :project="project"
          @click="router.push(`/project/${project.id}`)"
        />
      </div>

      <div v-if="total > 20" style="margin-top: 20px; text-align: center">
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
import type { Project } from '@/types'

const { t } = useI18n()

const router = useRouter()

const query = ref('')
const loading = ref(false)
const results = ref<Project[] | null>(null)
const total = ref(0)
const page = ref(1)

const hotTags = [
  '游戏', '动画', '音乐', '画画', '迷宫',
  '射击', '冒险', '教育', '数学', '故事'
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
  max-width: 800px;
  margin: 0 auto;
}

.search-bar {
  margin-bottom: 24px;
}

.hot-searches {
  text-align: center;
}

.section-label {
  font-size: 13px;
  color: var(--text2, #666);
  margin-bottom: 10px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.hot-tag {
  cursor: pointer;
  transition: transform 0.15s;
}

.hot-tag:hover {
  transform: scale(1.05);
}

.result-info {
  font-size: 14px;
  color: var(--text2, #666);
  margin-bottom: 16px;
}

.result-query {
  color: var(--text3, #999);
  font-size: 13px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

@media (max-width: 768px) {
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
