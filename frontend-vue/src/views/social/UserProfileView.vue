<template>
  <div>
    <div class="back-link" @click="router.back()">← {{ t('common.back') }}</div>

    <LoadingSkeleton v-if="loading" variant="detail" />

    <template v-else-if="userProfile">
      <!-- 用户信息卡片 -->
      <div class="page-card profile-header">
        <div class="profile-avatar">
          {{ (userProfile.nickname || userProfile.username || '?')[0].toUpperCase() }}
        </div>
        <div class="profile-info">
          <h2 class="profile-name">{{ userProfile.nickname || userProfile.username }}</h2>
          <div class="profile-meta">
            <span class="profile-role">
              <el-tag :type="roleType(userProfile.role)" size="small">{{ roleLabel[userProfile.role] }}</el-tag>
            </span>
            <span v-if="userProfile.level" class="profile-level">Lv.{{ userProfile.level }}</span>
            <span v-if="userProfile.points" class="profile-points">⭐ {{ userProfile.points }}</span>
            <span class="profile-joined">{{ formatDate(userProfile.createdAt) }}</span>
          </div>
          <p v-if="userProfile.bio" class="profile-bio">{{ userProfile.bio }}</p>
        </div>
      </div>

      <!-- 统计数据 -->
      <div class="profile-stats">
        <div class="stat-item">
          <div class="stat-value">{{ projectCount }}</div>
          <div class="stat-label">{{ t('project.published') }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ totalLikes }}</div>
          <div class="stat-label">{{ t('project.like') }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ totalViews }}</div>
          <div class="stat-label">{{ t('project.viewCount') }}</div>
        </div>
      </div>

      <!-- 用户作品列表 -->
      <div class="section-title">📦 {{ t('project.published') }}</div>

      <LoadingSkeleton v-if="projectsLoading" :count="3" variant="card" />
      <EmptyState
        v-else-if="projects.length === 0"
        icon="🎨"
        :text="t('common.empty')"
      />
      <div v-else class="project-grid">
        <ProjectCard
          v-for="p in projects"
          :key="p.id"
          :project="p"
          @click="router.push(`/project/${p.id}`)"
        />
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button :loading="projectsLoading" @click="loadMore">加载更多</el-button>
      </div>
    </template>

    <EmptyState v-else icon="👤" text="用户不存在" />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'UserProfile' })

import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userApi, socialApi } from '@/api'
import type { User, Project } from '@/types'
import { formatDate } from '@/utils'
import { useI18n } from '@/composables/useI18n'
import ProjectCard from '@/components/ProjectCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const userProfile = ref<User | null>(null)
const projects = ref<Project[]>([])
const projectsLoading = ref(false)
const page = ref(1)
const total = ref(0)
const hasMore = computed(() => projects.value.length < total.value)

const roleLabel: Record<string, string> = {
  STUDENT: '学生',
  TEACHER: '教师',
  ADMIN: '管理员'
}

const projectCount = computed(() => total.value)
const totalLikes = computed(() => projects.value.reduce((sum, p) => sum + (p.likeCount || 0), 0))
const totalViews = computed(() => projects.value.reduce((sum, p) => sum + (p.viewCount || 0), 0))

function roleType(role: string) {
  return ({ ADMIN: 'danger', TEACHER: 'warning' } as Record<string, string>[role]) || 'info'
}

async function loadUserProjects(pageNum = 1) {
  projectsLoading.value = true
  try {
    const userId = Number(route.params.id)
    const res = await socialApi.getFeed('latest', pageNum, 12)
    if (res.code === 0 && res.data) {
      // 过滤出该用户的作品（后端应提供按用户查询的 API，这里临时过滤）
      const allProjects = res.data.records || []
      const userProjects = allProjects.filter((p: Project) => p.userId === userId)
      if (pageNum === 1) {
        projects.value = userProjects
      } else {
        projects.value.push(...userProjects)
      }
      total.value = res.data.total || 0
      page.value = pageNum
    }
  } catch { /* 忽略 */ }
  finally { projectsLoading.value = false }
}

function loadMore() {
  loadUserProjects(page.value + 1)
}

onMounted(async () => {
  const userId = Number(route.params.id)
  try {
    // 加载用户信息（通过搜索 API 临时获取）
    const res = await userApi.searchUsers(String(userId), 1, 1)
    if (res.code === 0 && res.data?.records?.length) {
      userProfile.value = res.data.records[0]
    }
  } catch { /* 忽略 */ }

  await loadUserProjects()
  loading.value = false
})
</script>

<style scoped>
.profile-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
}

.profile-info { flex: 1; min-width: 0; }

.profile-name {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
}

.profile-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text2);
}

.profile-level {
  font-weight: 600;
  color: var(--primary);
}

.profile-points {
  font-weight: 600;
  color: var(--warning);
}

.profile-bio {
  margin-top: 10px;
  font-size: 14px;
  color: var(--text2);
  line-height: 1.6;
}

.profile-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  flex: 1;
  text-align: center;
  padding: 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text2);
  margin-top: 4px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 14px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.load-more {
  text-align: center;
  padding: 20px;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  .profile-meta {
    justify-content: center;
  }
  .project-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .profile-avatar {
    width: 56px;
    height: 56px;
    font-size: 22px;
  }
  .profile-name {
    font-size: 18px;
  }
  .stat-item {
    padding: 12px;
  }
  .stat-value {
    font-size: 20px;
  }
}
</style>
