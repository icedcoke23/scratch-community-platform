<template>
  <div class="admin-layout" :class="{ collapsed: sidebarCollapsed }">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-area">
          <span class="logo-icon">⚙️</span>
          <transition name="fade">
            <span v-show="!sidebarCollapsed" class="logo-text">管理后台</span>
          </transition>
        </div>
        <button class="collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开' : '收起'">
          <span class="collapse-icon" :class="{ rotated: sidebarCollapsed }">◀</span>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <transition name="fade">
            <span v-show="!sidebarCollapsed" class="nav-label">{{ item.label }}</span>
          </transition>
          <transition name="fade">
            <span v-if="item.badge && !sidebarCollapsed" class="nav-badge">{{ item.badge }}</span>
          </transition>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/feed" class="nav-item back-item">
          <span class="nav-icon">🏠</span>
          <transition name="fade">
            <span v-show="!sidebarCollapsed" class="nav-label">返回前台</span>
          </transition>
        </router-link>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <button class="mobile-menu-btn" @click="toggleSidebar">☰</button>
          <div class="breadcrumb-auto">
            <router-link to="/admin" class="breadcrumb-link">管理后台</router-link>
            <span v-if="currentNavLabel && currentNavLabel !== '仪表盘'" class="breadcrumb-sep">/</span>
            <span v-if="currentNavLabel && currentNavLabel !== '仪表盘'" class="breadcrumb-current">{{ currentNavLabel }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="header-user">
            <span class="user-avatar">{{ avatarLetter }}</span>
            <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
        </div>
      </header>

      <div class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminLayout' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const currentNavLabel = computed(() => {
  const item = navItems.value.find(n => isActive(n.path))
  return item?.label || ''
})

const sidebarCollapsed = ref(false)
const isMobile = ref(false)

const avatarLetter = computed(() =>
  (userStore.user?.nickname || userStore.user?.username || 'A')[0].toUpperCase()
)

interface NavItem {
  path: string
  icon: string
  label: string
  badge?: string | number
}

const navItems = computed<NavItem[]>(() => [
  { path: '/admin', icon: '📊', label: '仪表盘' },
  { path: '/admin/users', icon: '👥', label: '用户管理' },
  { path: '/admin/projects', icon: '🎮', label: '作品管理' },
  { path: '/admin/audit', icon: '🔍', label: '内容审核' },
  { path: '/admin/comments', icon: '💬', label: '评论管理' },
  { path: '/admin/problems', icon: '🧩', label: '题目管理' },
  { path: '/admin/competitions', icon: '🏆', label: '竞赛管理' },
  { path: '/admin/classes', icon: '📚', label: '班级管理' },
  { path: '/admin/statistics', icon: '📈', label: '数据统计' },
  { path: '/admin/config', icon: '⚙️', label: '系统配置' },
])

function isActive(path: string): boolean {
  if (path === '/admin') {
    return route.path === '/admin'
  }
  return route.path.startsWith(path)
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  if (isMobile.value) {
    document.body.classList.toggle('admin-sidebar-open', !sidebarCollapsed.value)
  }
}

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    sidebarCollapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  document.body.classList.remove('admin-sidebar-open')
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg, #f0f2f5);
}

/* ==================== Sidebar ==================== */
.admin-sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1e1e2d 0%, #1a1a2e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.admin-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  min-height: 60px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}

.logo-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.collapse-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.collapse-icon {
  display: inline-block;
  font-size: 12px;
  transition: transform 0.3s;
}

.collapse-icon.rotated {
  transform: rotate(180deg);
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  margin-bottom: 2px;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.nav-label {
  white-space: nowrap;
}

.nav-badge {
  margin-left: auto;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.sidebar-footer {
  padding: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.back-item {
  color: rgba(255, 255, 255, 0.4);
}

.back-item:hover {
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.05);
}

/* ==================== Main Content ==================== */
.admin-main {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.collapsed .admin-main {
  margin-left: 64px;
}

.admin-header {
  height: 60px;
  background: var(--card, #fff);
  border-bottom: 1px solid var(--border, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  color: var(--text);
}

.mobile-menu-btn:hover {
  background: var(--bg);
}

.breadcrumb-auto {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.breadcrumb-link {
  color: var(--text2, #64748b);
  text-decoration: none;
  transition: color 0.15s;
}

.breadcrumb-link:hover {
  color: var(--primary, #3b82f6);
}

.breadcrumb-sep {
  color: var(--text3, #94a3b8);
}

.breadcrumb-current {
  color: var(--text, #1a1a2e);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.admin-content {
  flex: 1;
  padding: 24px;
}

/* ==================== Animations ==================== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-fade-enter-active {
  transition: opacity 0.3s, transform 0.3s;
}

.page-fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ==================== Responsive ==================== */
@media (max-width: 768px) {
  .admin-sidebar {
    transform: translateX(-100%);
    width: 240px;
  }

  .admin-sidebar:not(.collapsed) {
    transform: translateX(0);
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
  }

  .admin-main {
    margin-left: 0;
  }

  .collapsed .admin-main {
    margin-left: 0;
  }

  .mobile-menu-btn {
    display: block;
  }

  .admin-content {
    padding: 16px;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 480px) {
  .admin-content {
    padding: 12px;
  }
}
</style>
