<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-logo" @click="$emit('navigate', '/feed')">
        <span class="logo-icon">🐱</span>
        <span class="logo-text">Scratch 社区</span>
      </div>
      <nav class="header-nav hidden-mobile">
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="nav-link"
          :class="{ active: currentPath === link.path }"
        >
          {{ link.label }}
        </router-link>
      </nav>
      <div class="header-right">
        <button class="icon-btn theme-toggle" @click="$emit('toggle-theme')" :title="isDark ? '切换到亮色' : '切换到暗色'">
          {{ isDark ? '🌙' : '☀️' }}
        </button>
        <button class="icon-btn lang-toggle" @click="$emit('toggle-locale')" :title="currentLocale === 'zh-CN' ? 'Switch to English' : '切换到中文'">
          {{ currentLocale === 'zh-CN' ? 'EN' : '中' }}
        </button>
        <template v-if="isLoggedIn">
          <div class="user-badge" @click="$emit('navigate', '/points')" title="我的积分">
            <span class="badge-icon">⭐</span>
            <span class="badge-text">Lv.{{ user?.level ?? '?' }}</span>
            <span class="badge-points">{{ user?.points ?? 0 }}</span>
          </div>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <button class="icon-btn" @click="$emit('navigate', '/notifications')" title="通知">🔔</button>
          </el-badge>
          <span class="user-name" @click="$emit('navigate', '/settings')">{{ user?.nickname || user?.username }}</span>
          <button class="icon-btn" @click="$emit('navigate', '/settings')" title="设置">⚙️</button>
          <el-button size="default" @click="$emit('logout')" class="logout-btn">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="default" @click="$emit('show-login')" class="login-btn">登录</el-button>
          <el-button size="default" @click="$emit('show-register')" class="register-btn">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  navLinks: Array<{ path: string; label: string }>
  currentPath: string
  isLoggedIn: boolean
  user: { nickname?: string; username?: string; points?: number; level?: number } | null
  unreadCount: number
  isDark: boolean
  currentLocale: string
}>()

defineEmits(['navigate', 'logout', 'toggle-theme', 'toggle-locale', 'show-login', 'show-register'])
</script>

<style scoped>
.header-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 28px;
  animation: logo-bounce 2s ease-in-out infinite;
}

@keyframes logo-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.logo-text {
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary), var(--accent-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 36px;
}

.icon-btn:hover {
  background: var(--primary-bg);
  transform: scale(1.1);
}

.theme-toggle, .lang-toggle {
  font-size: 18px;
}

.lang-toggle {
  font-weight: 700;
  font-size: 14px;
  color: var(--primary);
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, var(--primary-bg), #FEF3C7);
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 600;
}

.user-badge:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow);
}

.badge-icon {
  font-size: 16px;
}

.badge-text {
  font-size: 13px;
  color: var(--accent-orange);
  font-weight: 700;
}

.badge-points {
  font-size: 13px;
  color: var(--primary);
}

.user-name {
  font-size: 15px;
  color: var(--text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.user-name:hover {
  background: var(--primary-bg);
  color: var(--primary);
}

.login-btn {
  font-weight: 600;
  padding: 8px 24px;
}

.register-btn {
  font-weight: 500;
  padding: 8px 20px;
}

.logout-btn {
  font-size: 14px;
  padding: 8px 16px;
}

@media (max-width: 768px) {
  .logo-icon { font-size: 24px; }
  .logo-text { font-size: 16px; }
  .user-badge { padding: 4px 10px; }
  .badge-text, .badge-points { font-size: 12px; }
  .user-name { display: none; }
}
</style>
