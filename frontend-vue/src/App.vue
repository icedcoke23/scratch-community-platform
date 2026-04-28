<template>
  <el-config-provider :locale="zhCn">
    <!-- 路由加载进度条（Vue 组件化，替代原 DOM 操作方案） -->
    <RouteLoadingBar />
    <div class="app-layout">
      <!-- 顶部导航 -->
      <AppHeader
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
      <main class="app-main">
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

      <!-- 移动端底部导航 -->
      <MobileNav
        :links="mobileNavLinks"
        :current-path="route.path"
      />

      <!-- 登录/注册弹窗 -->
      <AuthDialog
        ref="authDialogRef"
        @login-success="onLoginSuccess"
      />
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
/* 全局样式 */
:root {
  --primary: #4F46E5;
  --primary-light: #818CF8;
  --primary-bg: #EEF2FF;
  --bg: #F9FAFB;
  --card: #FFF;
  --text: #111827;
  --text2: #6B7280;
  --border: #E5E7EB;
  --success: #10B981;
  --danger: #EF4444;
  --warning: #F59E0B;
  --radius: 8px;
  --shadow: 0 1px 3px rgba(0,0,0,.06);
}

/* 深色模式 */
:root.dark,
:root[data-theme="dark"] {
  --primary: #818CF8;
  --primary-light: #A5B4FC;
  --primary-bg: #1E1B4B;
  --bg: #0F172A;
  --card: #1E293B;
  --text: #E2E8F0;
  --text2: #94A3B8;
  --border: #334155;
  --success: #34D399;
  --danger: #F87171;
  --warning: #FBBF24;
  --shadow: 0 1px 3px rgba(0,0,0,.3);
}

/* 深色模式 Element Plus 覆盖 */
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
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: var(--bg);
  color: var(--text);
  line-height: 1.6;
}

/* 头部 */
.app-header {
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items:center;
  gap: 24px;
}

.header-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  cursor: pointer;
  white-space: nowrap;
}

.header-nav {
  display: flex;
  gap: 2px;
}

.nav-link {
  padding: 8px 14px;
  border-radius: var(--radius);
  font-size: 13px;
  color: var(--text2);
  text-decoration: none;
  transition: .15s;
}

.nav-link:hover { background: var(--bg); color: var(--text); }
.nav-link.active { background: var(--primary); color: #fff; }

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.user-points {
  font-size: 13px;
  color: var(--primary);
  cursor: pointer;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  color: var(--text2);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主内容 */
.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
  min-height: calc(100vh - 56px);
}

/* 移动端底部导航 */
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card);
  border-top: 1px solid var(--border);
  z-index: 100;
  padding: 6px 0 env(safe-area-inset-bottom, 0);
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
  padding: 4px 8px;
  font-size: 10px;
  color: var(--text2);
  text-decoration: none;
  transition: .15s;
}

.mobile-nav-item.active { color: var(--primary); }
.mobile-nav-icon { font-size: 18px; margin-bottom: 2px; }

/* 弹窗底部文字 */
.dialog-footer-text {
  text-align: center;
  font-size: 13px;
  color: var(--text2);
}

/* 响应式 */
@media (max-width: 768px) {
  .hidden-mobile { display: none !important; }
  .hidden-desktop { display: flex !important; }
  .header-inner { padding: 0 14px; height: 48px; }
  .header-logo { font-size: 16px; }
  .header-right { gap: 6px; }
  .user-points { font-size: 12px; }
  .user-name { max-width: 80px; font-size: 12px; }
  .app-main { padding: 14px; padding-bottom: calc(70px + env(safe-area-inset-bottom, 0)); }
  .mobile-nav {
    display: flex;
    padding-bottom: env(safe-area-inset-bottom, 0);
  }
}

@media (max-width: 480px) {
  .header-inner { padding: 0 10px; gap: 8px; }
  .header-right { gap: 4px; }
  .header-right .el-button { font-size: 12px; padding: 4px 8px; }
  .app-main { padding: 10px; padding-bottom: calc(70px + env(safe-area-inset-bottom, 0)); }
  .mobile-nav-item { padding: 4px 4px; font-size: 9px; }
  .mobile-nav-icon { font-size: 16px; }
}

@media (min-width: 769px) {
  .hidden-mobile { display: flex; }
  .hidden-desktop { display: none !important; }
}

/* 通用页面样式 */
.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.page-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px;
  margin-bottom: 14px;
  transition: box-shadow .15s;
}

.page-card:hover { box-shadow: var(--shadow); }

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.card-meta {
  font-size: 12px;
  color: var(--text2);
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text2);
}

.back-link {
  font-size: 13px;
  color: var(--text2);
  cursor: pointer;
  margin-bottom: 16px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.back-link:hover { color: var(--primary); }

.notification-badge {
  line-height: 1;
}

.notification-badge .el-badge__content {
  font-size: 10px;
}

/* 路由切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 图片懒加载 */
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
</style>
