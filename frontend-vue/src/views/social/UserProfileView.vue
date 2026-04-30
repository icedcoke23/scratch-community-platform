<template>
  <div class="profile-page">
    <div class="back-link" @click="router.back()">← {{ t('common.back') }}</div>

    <LoadingSkeleton v-if="loading" variant="detail" />

    <template v-else-if="userProfile">
      <!-- 用户信息卡片 - Hero 风格 -->
      <div class="profile-hero page-card">
        <div class="hero-bg" />
        <div class="hero-content">
          <div class="profile-avatar" :class="{ 'avatar-ring': isOwnProfile }">
            <span class="avatar-text">{{ avatarLetter }}</span>
            <span v-if="userProfile.level" class="avatar-badge">Lv.{{ userProfile.level }}</span>
          </div>
          <div class="profile-info">
            <div class="name-row">
              <h2 class="profile-name">{{ userProfile.nickname || userProfile.username }}</h2>
              <el-tag :type="roleType(userProfile.role)" size="small" effect="dark">{{ roleLabel[userProfile.role] }}</el-tag>
              <span v-if="userProfile.points" class="points-chip">⭐ {{ userProfile.points }}</span>
            </div>
            <p v-if="userProfile.bio" class="profile-bio">{{ userProfile.bio }}</p>
            <div class="profile-meta">
              <span class="meta-item">📅 {{ formatDate(userProfile.createdAt) }} 加入</span>
              <span v-if="userProfile.username" class="meta-item">@{{ userProfile.username }}</span>
            </div>
          </div>
          <div class="profile-actions">
            <template v-if="!isOwnProfile && userStore.isLoggedIn">
              <el-button
                :type="isFollowing ? 'info' : 'primary'"
                :icon="isFollowing ? 'Check' : 'Plus'"
                @click="toggleFollow"
                :loading="followLoading"
                round
              >
                {{ isFollowing ? '已关注' : '关注' }}
              </el-button>
              <el-button circle @click="router.push(`/project/${projects[0]?.id || ''}`)" title="私信">
                💬
              </el-button>
            </template>
            <el-button v-if="isOwnProfile" type="primary" plain round @click="router.push('/settings')">
              编辑资料
            </el-button>
          </div>
        </div>

        <!-- 统计数据 - 横向展示 -->
        <div class="stats-bar">
          <div class="stat-item" v-for="stat in statItems" :key="stat.label">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar page-card">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          <span class="tab-label">{{ tab.label }}</span>
          <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
        </div>
      </div>

      <!-- 作品列表 -->
      <template v-if="activeTab === 'projects'">
        <LoadingSkeleton v-if="projectsLoading" :count="3" variant="card" />
        <EmptyState v-else-if="projects.length === 0" icon="🎨" text="还没有发布作品" />
        <div v-else class="project-grid">
          <ProjectCard
            v-for="p in projects"
            :key="p.id"
            :project="p"
            @click="router.push(`/project/${p.id}`)"
          />
        </div>
        <div v-if="hasMore" class="load-more">
          <el-button :loading="projectsLoading" @click="loadMore" round>加载更多</el-button>
        </div>
      </template>

      <!-- 数据概览 Tab -->
      <template v-if="activeTab === 'stats'">
        <div class="page-card">
          <div class="section-title">📊 创作数据</div>
          <div class="stats-detail-grid">
            <div class="detail-card">
              <div class="detail-icon">🎮</div>
              <div class="detail-num">{{ totalProjects }}</div>
              <div class="detail-label">总作品数</div>
            </div>
            <div class="detail-card">
              <div class="detail-icon">❤️</div>
              <div class="detail-num">{{ totalLikes }}</div>
              <div class="detail-label">总获赞数</div>
            </div>
            <div class="detail-card">
              <div class="detail-icon">👀</div>
              <div class="detail-num">{{ totalViews }}</div>
              <div class="detail-label">总浏览量</div>
            </div>
            <div class="detail-card">
              <div class="detail-icon">💬</div>
              <div class="detail-num">{{ totalComments }}</div>
              <div class="detail-label">总评论数</div>
            </div>
          </div>
        </div>
      </template>
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
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ProjectCard from '@/components/ProjectCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const userProfile = ref<User | null>(null)
const projects = ref<Project[]>([])
const projectsLoading = ref(false)
const page = ref(1)
const total = ref(0)
const isFollowing = ref(false)
const followLoading = ref(false)
const activeTab = ref('projects')

const hasMore = computed(() => projects.value.length < total.value)
const isOwnProfile = computed(() => userStore.user?.id === Number(route.params.id))
const avatarLetter = computed(() => (userProfile.value?.nickname || userProfile.value?.username || '?')[0].toUpperCase())

const roleLabel: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
function roleType(role: string) {
  const map: Record<string, 'danger' | 'warning' | 'info'> = { ADMIN: 'danger', TEACHER: 'warning' }
  return map[role] || 'info'
}

const totalProjects = computed(() => total.value)
const totalLikes = computed(() => projects.value.reduce((s, p) => s + (p.likeCount || 0), 0))
const totalViews = computed(() => projects.value.reduce((s, p) => s + (p.viewCount || 0), 0))
const totalComments = computed(() => projects.value.reduce((s, p) => s + (p.commentCount || 0), 0))

const statItems = computed(() => [
  { value: totalProjects.value, label: '作品' },
  { value: totalLikes.value, label: '获赞' },
  { value: totalViews.value, label: '浏览' },
  { value: userProfile.value?.points || 0, label: '积分' },
])

const tabs = computed(() => [
  { key: 'projects', icon: '🎨', label: '作品', count: totalProjects.value },
  { key: 'stats', icon: '📊', label: '数据' },
])

async function loadUserProjects(pageNum = 1) {
  projectsLoading.value = true
  try {
    const userId = Number(route.params.id)
    const res = await socialApi.getFeed('latest', pageNum, 12)
    if (res.code === 0 && res.data) {
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
  } catch { /* ignore */ }
  finally { projectsLoading.value = false }
}

function loadMore() {
  loadUserProjects(page.value + 1)
}

async function toggleFollow() {
  const userId = Number(route.params.id)
  followLoading.value = true
  try {
    if (isFollowing.value) {
      const res = await userApi.unfollowUser(userId)
      if (res.code === 0) {
        isFollowing.value = false
        ElMessage.success('已取消关注')
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } else {
      const res = await userApi.followUser(userId)
      if (res.code === 0) {
        isFollowing.value = true
        ElMessage.success('关注成功')
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    }
  } catch { ElMessage.error('操作失败') }
  finally { followLoading.value = false }
}

onMounted(async () => {
  const userId = Number(route.params.id)
  try {
    const res = await userApi.getUserProfile(userId)
    if (res.code === 0 && res.data) {
      userProfile.value = res.data
    }
  } catch { /* ignore */ }
  await loadUserProjects()
  loading.value = false
})
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
}

/* Hero Card */
.profile-hero {
  position: relative;
  overflow: hidden;
  padding: 0;
  margin-bottom: 16px;
}

.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  opacity: 0.9;
}

.hero-content {
  position: relative;
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 80px 28px 20px;
}

.profile-avatar {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 800;
  flex-shrink: 0;
  border: 4px solid var(--card, #fff);
  box-shadow: 0 8px 24px rgba(0,0,0,0.2);
  transition: transform 0.3s ease;
}

.profile-avatar:hover {
  transform: scale(1.05) rotate(3deg);
}

.avatar-ring {
  box-shadow: 0 0 0 3px var(--primary), 0 8px 24px rgba(0,0,0,0.2);
}

.avatar-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: linear-gradient(135deg, #F59E0B, #EF4444);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  border: 2px solid var(--card, #fff);
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.profile-name {
  font-size: 26px;
  font-weight: 800;
  margin: 0;
  color: var(--text, #1a1a2e);
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.points-chip {
  font-size: 13px;
  font-weight: 700;
  color: #D97706;
  background: #FEF3C7;
  padding: 3px 10px;
  border-radius: 12px;
}

.profile-bio {
  font-size: 14px;
  color: var(--text2, #64748b);
  line-height: 1.6;
  margin: 0 0 8px;
}

.profile-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 13px;
  color: var(--text2, #64748b);
}

.profile-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 60px;
}

/* Stats Bar */
.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--border, #e5e7eb);
  padding: 0;
}

.stats-bar .stat-item {
  text-align: center;
  padding: 18px 12px;
  border-right: 1px solid var(--border, #e5e7eb);
  transition: background 0.2s ease;
  cursor: default;
}

.stats-bar .stat-item:last-child {
  border-right: none;
}

.stats-bar .stat-item:hover {
  background: var(--bg, #f9fafb);
}

.stats-bar .stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary, #3B82F6);
  line-height: 1.2;
}

.stats-bar .stat-label {
  font-size: 12px;
  color: var(--text2, #64748b);
  margin-top: 2px;
}

/* Tabs */
.tab-bar {
  display: flex;
  gap: 0;
  padding: 0;
  margin-bottom: 16px;
  overflow-x: auto;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text2, #64748b);
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tab-item:hover {
  color: var(--primary, #3B82F6);
  background: var(--bg, #f9fafb);
}

.tab-item.active {
  color: var(--primary, #3B82F6);
  border-bottom-color: var(--primary, #3B82F6);
}

.tab-icon {
  font-size: 16px;
}

.tab-count {
  font-size: 11px;
  background: var(--primary, #3B82F6);
  color: #fff;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 700;
}

/* Stats Detail */
.stats-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.detail-card {
  text-align: center;
  padding: 24px 16px;
  background: var(--bg, #f5f5f5);
  border-radius: 16px;
  transition: all 0.2s ease;
}

.detail-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.detail-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.detail-num {
  font-size: 28px;
  font-weight: 800;
  color: var(--text, #1a1a2e);
}

.detail-label {
  font-size: 13px;
  color: var(--text2, #64748b);
  margin-top: 4px;
}

/* Projects */
.section-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 16px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.load-more {
  text-align: center;
  padding: 24px;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 70px 20px 16px;
  }
  .name-row {
    justify-content: center;
  }
  .profile-meta {
    justify-content: center;
  }
  .profile-actions {
    align-self: center;
    margin-top: 12px;
  }
  .stats-bar {
    grid-template-columns: repeat(2, 1fr);
  }
  .stats-bar .stat-item:nth-child(2) {
    border-right: none;
  }
  .stats-bar .stat-item:nth-child(1),
  .stats-bar .stat-item:nth-child(2) {
    border-bottom: 1px solid var(--border, #e5e7eb);
  }
  .project-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .profile-avatar {
    width: 72px;
    height: 72px;
    font-size: 30px;
  }
  .profile-name {
    font-size: 20px;
  }
  .tab-item {
    padding: 12px 16px;
    font-size: 13px;
  }
}
</style>
