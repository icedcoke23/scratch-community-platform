<template>
  <el-config-provider :locale="zhCn">
    <!-- 路由加载进度条（Vue 组件化，替代原 DOM 操作方案） -->
    <RouteLoadingBar />
    <div class="app-layout">
      <!-- 顶部导航（管理后台有自己的布局，不显示） -->
      <AppHeader
        v-if="!isAdminRoute"
        :nav-links="navLinks"
        :current-path="route.path"
        :is-logged-in="userStore.isLoggedIn"
        :user="userStore.user"
        :unread-count="unreadCount"
        :is-dark="isDark"
        :current-locale="currentLocale"
        @navigate="(path: string) => router.push(path)"
        @logout="handleLogout"
        @toggle-theme="toggleTheme"
        @toggle-locale="toggleLocale"
        @show-login="openLoginDialog"
        @show-register="openRegisterDialog"
      />

      <!-- 主内容（keep-alive 缓存高频页面 + 路由切换动画） -->
      <main :class="['app-main', { 'admin-main-wrapper': isAdminRoute }]">
        <ErrorBoundary>
          <router-view v-slot="{ Component, route: currentRoute }">
            <transition name="fade-slide" mode="out-in">
              <keep-alive :include="cachedViews">
                <component :is="Component" :key="currentRoute.path" />
              </keep-alive>
            </transition>
          </router-view>
        </ErrorBoundary>
      </main>

      <!-- 移动端底部导航（管理后台不显示） -->
      <MobileNav
        v-if="!isAdminRoute"
        :links="mobileNavLinks"
        :current-path="route.path"
      />

      <!-- 登录/注册弹窗 -->
      <AuthDialog
        ref="authDialogRef"
        @login-success="onLoginSuccess"
      />

      <!-- 全局浮动创建按钮（管理后台不显示） -->
      <CreateProjectFab v-if="userStore.isLoggedIn && route.path !== '/editor' && !isAdminRoute" />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { pointApi } from '@/api'
import { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import AppHeader from '@/components/AppHeader.vue'
import MobileNav from '@/components/MobileNav.vue'
import AuthDialog from '@/components/AuthDialog.vue'
import RouteLoadingBar from '@/components/RouteLoadingBar.vue'
import CreateProjectFab from '@/components/CreateProjectFab.vue'
import { useTheme } from '@/composables/useTheme'
import { useI18n } from '@/composables/useI18n'
import { useNavigation } from '@/composables/useNavigation'

// 需要缓存的页面组件名称（keep-alive，从路由 meta.keepAlive 动态获取）
const cachedViews = computed(() => {
  return router.getRoutes()
    .filter(r => r.meta?.keepAlive)
    .map(r => r.name as string)
    .filter(Boolean)
})

const { isDark, toggleTheme } = useTheme()
const { t, locale: currentLocale, toggleLocale, initLocale } = useI18n()

onMounted(() => {
  initLocale()
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

// 判断是否为管理后台路由（使用独立布局）
const isAdminRoute = computed(() => route.path.startsWith('/admin'))

// 未读通知数（从 store 获取）
const unreadCount = computed(() => notificationStore.unreadCount)

// 导航链接（抽取到 useNavigation composable）
const { navLinks, mobileNavLinks } = useNavigation()

// AuthDialog ref
const authDialogRef = ref<InstanceType<typeof AuthDialog> | null>(null)

function openLoginDialog() {
  if (authDialogRef.value) authDialogRef.value.showLogin = true
}
function openRegisterDialog() {
  if (authDialogRef.value) authDialogRef.value.showRegister = true
}

// 登录成功回调
function onLoginSuccess(data: { token: string; userInfo: Record<string, unknown> }) {
  userStore.setAuth(data.token, data.userInfo)
}

// 退出
function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/feed')
}

// 加载积分
async function loadMyPoints() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await pointApi.getMyPoints()
    if (res.code === 0 && res.data && userStore.user) {
      userStore.user.points = res.data.points
      userStore.user.level = res.data.level
    }
  } catch { /* 忽略 */ }
}

// 监听登录状态变化：登录后开始轮询，登出后停止
watch(() => userStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadMyPoints()
    notificationStore.startPolling()
  } else {
    notificationStore.reset()
  }
}, { immediate: true })

onUnmounted(() => {
  notificationStore.stopPolling()
})
</script>

<style>
/* ============================================================
   Scratch 社区 — 少儿编程友好设计系统
   ============================================================ */

/* --- 色彩方案：明亮活力 --- */
:root {
  --primary: #3B82F6;
  --primary-light: #60A5FA;
  --primary-dark: #2563EB;
  --primary-bg: #EFF6FF;

  /* 辅助色：多彩活力 */
  --accent-orange: #F97316;
  --accent-green: #22C55E;
  --accent-purple: #A855F7;
  --accent-pink: #EC4899;
  --accent-yellow: #EAB308;
  --accent-cyan: #06B6D4;

  /* 背景 */
  --bg: #F0F9FF;
  --bg-warm: #FFFBEB;
  --card: #FFFFFF;

  /* 文字 */
  --text: #1E293B;
  --text2: #64748B;
  --text3: #94A3B8;

  /* 边框与圆角 */
  --border: #E2E8F0;
  --radius: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;

  /* 阴影 */
  --shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
  --shadow-lg: 0 4px 16px rgba(59, 130, 246, 0.12);
  --shadow-xl: 0 8px 32px rgba(59, 130, 246, 0.16);

  /* 动画 */
  --bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
  --smooth: cubic-bezier(0.4, 0, 0.2, 1);

  /* 成功/危险/警告 */
  --success: #22C55E;
  --danger: #EF4444;
  --warning: #F59E0B;
}

/* --- 深色模式 --- */
:root.dark,
:root[data-theme="dark"] {
  --primary: #60A5FA;
  --primary-light: #93C5FD;
  --primary-dark: #3B82F6;
  --primary-bg: #1E3A5F;
  --bg: #0F172A;
  --card: #1E293B;
  --text: #E2E8F0;
  --text2: #94A3B8;
  --text3: #64748B;
  --border: #334155;
  --success: #4ADE80;
  --danger: #F87171;
  --warning: #FBBF24;
  --shadow: 0 2px 8px rgba(0,0,0,0.3);
  --shadow-lg: 0 4px 16px rgba(0,0,0,0.4);
  --shadow-xl: 0 8px 32px rgba(0,0,0,0.5);
}

/* --- 深色模式 Element Plus 覆盖 --- */
:root.dark .el-dialog,
:root[data-theme="dark"] .el-dialog {
  background: var(--card);
}

:root.dark .el-input__wrapper,
:root[data-theme="dark"] .el-input__wrapper {
  background: var(--bg);
  box-shadow: 0 0 0 1px var(--border) inset;
}

:root.dark .el-input__inner,
:root[data-theme="dark"] .el-input__inner {
  color: var(--text);
}

:root.dark .el-table,
:root[data-theme="dark"] .el-table {
  background: var(--card);
  color: var(--text);
}

:root.dark .el-table th,
:root[data-theme="dark"] .el-table th {
  background: var(--bg);
}

:root.dark .el-table td,
:root.dark .el-table tr,
:root[data-theme="dark"] .el-table td,
:root[data-theme="dark"] .el-table tr {
  background: var(--card);
  color: var(--text);
}

:root.dark .el-table--striped .el-table__body tr.el-table__row--striped td,
:root[data-theme="dark"] .el-table--striped .el-table__body tr.el-table__row--striped td {
  background: var(--bg);
}

:root.dark .el-drawer,
:root[data-theme="dark"] .el-drawer {
  background: var(--card);
}

:root.dark .el-drawer__header,
:root[data-theme="dark"] .el-drawer__header {
  color: var(--text);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei',
               'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  background: var(--bg);
  color: var(--text);
  line-height: 1.7;
  font-size: 16px; /* 少儿友好基础字号 */
  -webkit-font-smoothing: antialiased;
}

/* --- 全局按钮放大 --- */
.el-button {
  min-height: 40px;
  padding: 10px 20px;
  font-size: 15px;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.2s var(--smooth);
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-button:active {
  transform: translateY(0);
}

.el-button--primary {
  min-height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  border: none;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.el-button--primary:hover {
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* --- 输入框放大 --- */
.el-input__wrapper {
  border-radius: 10px;
  min-height: 40px;
}

.el-input__inner {
  font-size: 15px;
}

/* --- 头部导航 --- */
.app-header {
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.06);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px; /* 从 56px 增加到 64px */
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-logo {
  font-size: 20px;
  font-weight: 800;
  color: var(--primary);
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.2s var(--bounce);
}

.header-logo:hover {
  transform: scale(1.03);
}

.header-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 10px 16px;
  border-radius: var(--radius);
  font-size: 15px; /* 从 13px 增加到 15px */
  font-weight: 500;
  color: var(--text2);
  text-decoration: none;
  transition: all 0.2s var(--smooth);
  position: relative;
}

.nav-link:hover {
  background: var(--primary-bg);
  color: var(--primary);
}

.nav-link.active {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-points {
  font-size: 15px;
  color: var(--primary);
  cursor: pointer;
  font-weight: 700;
  background: var(--primary-bg);
  padding: 6px 14px;
  border-radius: 20px;
  transition: all 0.2s var(--smooth);
}

.user-points:hover {
  background: var(--primary);
  color: #fff;
  transform: scale(1.05);
}

.user-name {
  font-size: 15px;
  color: var(--text2);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

/* --- 主内容 --- */
.app-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 64px);
}

/* 管理后台使用独立布局，不需要默认的 max-width 和 padding */
.app-main.admin-main-wrapper {
  max-width: none;
  margin: 0;
  padding: 0;
  min-height: 100vh;
}

/* --- 移动端底部导航 --- */
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card);
  border-top: 1px solid var(--border);
  z-index: 100;
  padding: 8px 0 env(safe-area-inset-bottom, 0);
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
}

.mobile-nav-inner, .mobile-nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.mobile-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 10px;
  font-size: 11px;
  color: var(--text2);
  text-decoration: none;
  transition: all 0.2s var(--smooth);
  border-radius: var(--radius);
  min-width: 56px;
}

.mobile-nav-item.active {
  color: var(--primary);
  background: var(--primary-bg);
  transform: scale(1.05);
}

.mobile-nav-icon {
  font-size: 22px;
  margin-bottom: 3px;
}

/* --- 弹窗底部文字 --- */
.dialog-footer-text {
  text-align: center;
  font-size: 15px;
  color: var(--text2);
}

/* --- 响应式 --- */
@media (max-width: 768px) {
  .hidden-mobile { display: none !important; }
  .hidden-desktop { display: flex !important; }
  .header-inner { padding: 0 16px; height: 56px; }
  .header-logo { font-size: 18px; }
  .header-right { gap: 8px; }
  .user-points { font-size: 13px; padding: 5px 10px; }
  .user-name { max-width: 80px; font-size: 13px; }
  .app-main { padding: 16px; padding-bottom: calc(80px + env(safe-area-inset-bottom, 0)); }
  .mobile-nav {
    display: flex;
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
}

@media (max-width: 480px) {
  .header-inner { padding: 0 12px; gap: 8px; }
  .header-right { gap: 6px; }
  .header-right .el-button { font-size: 13px; padding: 6px 10px; }
  .app-main { padding: 12px; padding-bottom: calc(80px + env(safe-area-inset-bottom, 0)); }
  .mobile-nav-item { padding: 6px 6px; font-size: 10px; }
  .mobile-nav-icon { font-size: 20px; }
}

@media (min-width: 769px) {
  .hidden-mobile { display: flex; }
  .hidden-desktop { display: none !important; }
}

/* --- 通用页面样式 --- */
.page-title {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 24px;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 16px;
  transition: all 0.2s var(--smooth);
}

.page-card:hover {
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-light);
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text);
}

.card-meta {
  font-size: 14px;
  color: var(--text2);
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 48px;
  color: var(--text2);
  font-size: 16px;
}

.back-link {
  font-size: 15px;
  color: var(--text2);
  cursor: pointer;
  margin-bottom: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius);
  transition: all 0.2s var(--smooth);
  font-weight: 500;
}

.back-link:hover {
  color: var(--primary);
  background: var(--primary-bg);
}

.notification-badge {
  line-height: 1;
}

.notification-badge .el-badge__content {
  font-size: 11px;
}

/* --- 路由切换动画 --- */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s var(--smooth), transform 0.25s var(--smooth);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* --- 图片懒加载 --- */
.lazy-img {
  transition: opacity 0.3s ease;
  opacity: 0.6;
}

.lazy-img.lazy-loaded {
  opacity: 1;
}

.lazy-img.lazy-error {
  opacity: 0.5;
  filter: grayscale(1);
}

/* --- 滚动条美化 --- */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text3);
}

/* --- 选中文字颜色 --- */
::selection {
  background: var(--primary-bg);
  color: var(--primary-dark);
}

/* === 增强动画和微交互 === */

/* 卡片入场动画 */
@keyframes card-enter {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-card {
  animation: card-enter 0.4s var(--smooth) both;
}

.page-card:nth-child(2) { animation-delay: 0.05s; }
.page-card:nth-child(3) { animation-delay: 0.1s; }
.page-card:nth-child(4) { animation-delay: 0.15s; }
.page-card:nth-child(5) { animation-delay: 0.2s; }

/* 数字跳动效果 */
@keyframes number-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.stat-value,
.stat-number,
.stat-num {
  transition: transform 0.3s var(--bounce);
}

.stat-value:hover,
.stat-number:hover,
.stat-num:hover {
  animation: number-pop 0.4s var(--bounce);
}

/* 渐变文字效果 */
.gradient-text {
  background: linear-gradient(135deg, var(--primary), var(--accent-purple), var(--accent-pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 浮动效果 */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.float-anim {
  animation: float 3s ease-in-out infinite;
}

/* 脉冲效果 */
@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(59, 130, 246, 0); }
}

.pulse-anim {
  animation: pulse-glow 2s ease-in-out infinite;
}

/* 标题波浪 */
@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(15deg); }
  75% { transform: rotate(-15deg); }
}

.title-emoji {
  display: inline-block;
  animation: wave 2s ease-in-out infinite;
  transform-origin: 70% 70%;
}

/* 骨架屏闪烁 */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(90deg, var(--bg) 25%, var(--border) 50%, var(--bg) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius);
}

/* 按钮点击波纹 */
.el-button {
  position: relative;
  overflow: hidden;
}

.el-button::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at var(--ripple-x, 50%) var(--ripple-y, 50%), rgba(255,255,255,0.3) 0%, transparent 60%);
  opacity: 0;
  transition: opacity 0.3s;
}

.el-button:active::after {
  opacity: 1;
}

/* 表格行悬停增强 */
.el-table tr {
  transition: all 0.2s ease;
}

.el-table tr:hover > td {
  background: var(--primary-bg) !important;
}

/* Tag 微交互 */
.el-tag {
  transition: all 0.2s var(--bounce);
}

.el-tag:hover {
  transform: scale(1.05);
}

/* 头像悬停效果 */
.profile-avatar,
.avatar-circle {
  transition: transform 0.3s var(--bounce), box-shadow 0.3s ease;
}

.profile-avatar:hover,
.avatar-circle:hover {
  transform: scale(1.08) rotate(3deg);
}

/* 进度条动画 */
.progress-fill {
  transition: width 0.8s var(--smooth);
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: progress-shine 2s ease-in-out infinite;
}

@keyframes progress-shine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* 空状态动画 */
.empty-state {
  animation: card-enter 0.5s var(--smooth) both;
}

/* 响应式表格 */
@media (max-width: 768px) {
  .el-table {
    font-size: 12px;
  }
  .el-table .cell {
    padding: 6px 4px;
  }
  .el-dialog {
    width: 90% !important;
    margin: 16px auto;
  }
}

@media (max-width: 480px) {
  .el-table {
    font-size: 11px;
  }
  .el-form-item__label {
    font-size: 13px;
  }
}

/* 用户徽章动画 */
.user-badge {
  transition: all 0.2s var(--bounce);
}

.user-badge:hover {
  transform: scale(1.08);
  background: var(--primary);
  color: #fff;
}

/* 通知图标摇动 */
@keyframes bell-shake {
  0%, 100% { transform: rotate(0); }
  15% { transform: rotate(15deg); }
  30% { transform: rotate(-15deg); }
  45% { transform: rotate(10deg); }
  60% { transform: rotate(-10deg); }
  75% { transform: rotate(5deg); }
}

.notification-badge:hover .icon-btn {
  animation: bell-shake 0.6s ease-in-out;
}

/* 搜索框聚焦增强 */
.search-input .el-input__wrapper:focus-within {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2), var(--shadow);
  border-color: var(--primary);
}

/* Tab 切换下划线动画 */
.tab-item {
  position: relative;
}

.tab-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 3px;
  background: var(--primary);
  border-radius: 2px;
  transition: all 0.3s var(--bounce);
  transform: translateX(-50%);
}

.tab-item.active::after {
  width: 60%;
}

.tab-item:hover::after {
  width: 40%;
}

/* 项目卡片悬停效果增强 */
.project-card {
  transition: all 0.3s var(--bounce);
}

.project-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: var(--shadow-xl);
}

/* 成就卡片解锁动画 */
@keyframes unlock {
  0% { transform: scale(0.8); opacity: 0; }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}

.achievement-unlocked {
  animation: unlock 0.5s var(--bounce);
}

/* 加载更多按钮脉冲 */
.load-more-btn {
  position: relative;
}

.load-more-btn::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s;
  filter: blur(8px);
}

.load-more-btn:hover::before {
  opacity: 0.3;
}
</style>
