<template>
  <div>
    <h1 class="page-title">
      <span class="title-emoji">🔔</span>
      {{ t('notification.title') }}
      <el-button
        v-if="notificationStore.unreadCount > 0"
        type="primary"
        size="default"
        text
        @click="handleMarkAllRead"
        :loading="markAllLoading"
        class="mark-all-btn"
      >
        {{ t('notification.markAllRead') }}
      </el-button>
    </h1>

    <LoadingSkeleton v-if="notificationStore.loading && notificationStore.notifications.length === 0" :count="5" variant="list" />
    <EmptyState
      v-else-if="notificationStore.notifications.length === 0"
      icon="🔔"
      text="暂时没有通知"
    />

    <template v-else>
      <div class="notification-list">
        <div
          v-for="n in notificationStore.notifications"
          :key="n.id"
          class="notification-item"
          :class="{ unread: !n.read }"
          @click="handleClick(n)"
        >
          <div class="notif-icon-wrapper" :class="`type-${n.type?.toLowerCase()}`">
            <span class="notif-icon">{{ typeIcon[n.type] || '📌' }}</span>
          </div>
          <div class="notif-body">
            <div class="notif-title">{{ n.title }}</div>
            <div v-if="n.content" class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ timeAgo(n.createdAt) }}</div>
          </div>
          <button
            v-if="!n.read"
            class="read-btn"
            @click.stop="handleMarkRead(n.id)"
          >
            ✓
          </button>
        </div>
      </div>

      <div v-if="notificationStore.notifications.length < notificationStore.total" class="load-more">
        <el-button size="large" :loading="notificationStore.loading" @click="loadMore">
          📦 加载更多
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Notifications' })

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import type { Notification } from '@/types'
import { timeAgo } from '@/utils'
import { useI18n } from '@/composables/useI18n'
import EmptyState from '@/components/EmptyState.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const { t } = useI18n()

const router = useRouter()
const notificationStore = useNotificationStore()
const markAllLoading = ref(false)

const typeIcon: Record<string, string> = {
  LIKE: '❤️',
  COMMENT: '💬',
  FOLLOW: '👤',
  HOMEWORK: '📚',
  GRADE: '✅',
  SYSTEM: '🔔',
  COMPETITION: '🏆',
  REMIX: '🔄'
}

function handleClick(n: Notification) {
  if (!n.read) {
    notificationStore.markRead(n.id)
  }
  if (n.relatedId) {
    switch (n.type) {
      case 'LIKE':
      case 'COMMENT':
      case 'REMIX':
        router.push(`/project/${n.relatedId}`)
        break
      case 'HOMEWORK':
      case 'GRADE':
        router.push(`/homework/${n.relatedId}`)
        break
      case 'COMPETITION':
        router.push(`/competition/${n.relatedId}`)
        break
      default:
        break
    }
  }
}

async function handleMarkRead(id: number) {
  await notificationStore.markRead(id)
}

async function handleMarkAllRead() {
  markAllLoading.value = true
  await notificationStore.markAllRead()
  markAllLoading.value = false
}

function loadMore() {
  notificationStore.fetchNotifications(notificationStore.page + 1)
}

onMounted(() => {
  notificationStore.fetchNotifications(1)
})
</script>

<style scoped>
.mark-all-btn {
  font-size: 14px;
  font-weight: 600;
  margin-left: 12px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: var(--card);
  border: 2px solid var(--border);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-item:hover {
  background: var(--bg);
  box-shadow: var(--shadow);
  transform: translateX(4px);
}

.notification-item.unread {
  border-color: var(--primary-light);
  background: var(--primary-bg);
  border-left: 4px solid var(--primary);
}

.notif-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
}

.type-like { background: #FEE2E2; }
.type-comment { background: #DBEAFE; }
.type-follow { background: #F3E8FF; }
.type-homework { background: #DCFCE7; }
.type-grade { background: #FEF3C7; }
.type-system { background: #F3F4F6; }
.type-competition { background: #FFEDD5; }
.type-remix { background: #E0E7FF; }

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
}

.notif-content {
  font-size: 14px;
  color: var(--text2);
  line-height: 1.5;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-time {
  font-size: 12px;
  color: var(--text3);
}

.read-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--primary);
  background: var(--primary-bg);
  color: var(--primary);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.read-btn:hover {
  background: var(--primary);
  color: #fff;
  transform: scale(1.1);
}

.load-more {
  text-align: center;
  padding: 24px;
}

@media (max-width: 480px) {
  .notification-item { padding: 12px; gap: 10px; }
  .notif-icon-wrapper { width: 34px; height: 34px; font-size: 18px; }
  .notif-title { font-size: 14px; }
}
</style>
