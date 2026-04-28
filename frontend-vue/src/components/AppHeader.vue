<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-logo" @click="$emit('navigate', '/feed')">🧩 Scratch 社区</div>
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
        <el-button size="small" text @click="$emit('toggle-theme')" :title="isDark ? '切换到亮色' : '切换到暗色'">
          {{ isDark ? '🌙' : '☀️' }}
        </el-button>
        <el-button size="small" text @click="$emit('toggle-locale')" :title="currentLocale === 'zh-CN' ? 'Switch to English' : '切换到中文'">
          {{ currentLocale === 'zh-CN' ? '🌐 EN' : '🌐 中' }}
        </el-button>
        <template v-if="isLoggedIn">
          <span class="user-points" @click="$emit('navigate', '/points')" title="我的积分">
            ⭐ {{ user?.points ?? '--' }} Lv.{{ user?.level ?? '--' }}
          </span>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
            <el-button size="small" text @click="$emit('navigate', '/notifications')" title="通知">🔔</el-button>
          </el-badge>
          <span class="user-name">{{ user?.nickname || user?.username }}</span>
          <el-button size="small" text @click="$emit('navigate', '/settings')" title="设置">⚙️</el-button>
          <el-button size="small" @click="$emit('logout')">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" size="small" @click="$emit('show-login')">登录</el-button>
          <el-button size="small" @click="$emit('show-register')">注册</el-button>
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
