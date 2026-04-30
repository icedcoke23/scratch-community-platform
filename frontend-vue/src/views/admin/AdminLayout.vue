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
  width: 260px;
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
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
}

.admin-sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6, #ec4899);
  z-index: 1;
}

.admin-sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  min-height: 72px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.logo-icon {
  font-size: 32px;
  flex-shrink: 0;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  white-space: nowrap;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #fff, #a5b4fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.collapse-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  transition: all 0.3s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.collapse-icon {
  display: inline-block;
  font-size: 14px;
  transition: transform 0.3s;
}

.collapse-icon.rotated {
  transform: rotate(180deg);
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
  width: 5px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  margin-bottom: 4px;
  position: relative;
  overflow: hidden;
  white-space: nowrap;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, #6366f1, #8b5cf6);
  border-radius: 0 3px 3px 0;
  transition: height 0.3s ease;
}

.nav-item:hover::before,
.nav-item.active::before {
  height: 60%;
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(139, 92, 246, 0.2));
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.2);
  border: 1px solid rgba(99, 102, 241, 0.3);
}

.nav-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 28px;
  text-align: center;
  transition: transform 0.3s ease;
}

.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  transform: scale(1.15);
}

.nav-label {
  white-space: nowrap;
}

.nav-badge {
  margin-left: auto;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: 12px;
  min-width: 22px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4);
  animation: badgePulse 2s ease-in-out infinite;
}

@keyframes badgePulse {
  0%, 100% { box-shadow: 0 2px 8px rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 2px 12px rgba(239, 68, 68, 0.6); }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.back-item {
  color: rgba(255, 255, 255, 0.5);
  border: 1px dashed rgba(255, 255, 255, 0.2);
}

.back-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.3);
}

/* ==================== Main Content ==================== */
.admin-main {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.collapsed .admin-main {
  margin-left: 72px;
}

.admin-header {
  height: 72px;
  background: var(--card, #fff);
  border-bottom: 1px solid var(--border, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mobile-menu-btn {
  display: none;
  background: var(--primary-bg);
  border: none;
  font-size: 22px;
  cursor: pointer;
  padding: 10px;
  border-radius: 12px;
  color: var(--primary);
  transition: all 0.3s ease;
}

.mobile-menu-btn:hover {
  background: var(--primary);
  color: white;
  transform: scale(1.05);
}

.breadcrumb-auto {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}

.breadcrumb-link {
  color: var(--text2, #64748b);
  text-decoration: none;
  transition: all 0.2s ease;
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: 500;
}

.breadcrumb-link:hover {
  color: var(--primary, #3b82f6);
  background: var(--primary-bg);
}

.breadcrumb-sep {
  color: var(--text3, #94a3b8);
  font-weight: 600;
}

.breadcrumb-current {
  color: var(--text, #1a1a2e);
  font-weight: 700;
  font-size: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: var(--primary-bg);
  border-radius: 16px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.header-user:hover {
  background: var(--primary);
  transform: scale(1.02);
}

.header-user:hover .user-avatar,
.header-user:hover .user-name {
  color: white;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  transition: all 0.3s ease;
}

.user-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  transition: color 0.3s ease;
}

.admin-content {
  flex: 1;
  padding: 32px;
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
