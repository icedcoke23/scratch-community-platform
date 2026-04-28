<template>
  <div>
    <h1 class="page-title">
      🔔 {{ t('notification.title') }}
      <el-button
        v-if="notificationStore.unreadCount > 0"
        type="primary"
        size="small"
        text
        @click="handleMarkAllRead"
        :loading="markAllLoading"
      >
        {{ t('notification.markAllRead') }}
      </el-button>
    </h1>

    <LoadingSkeleton v-if="notificationStore.loading && notificationStore.notifications.length === 0" :count="5" variant="list" />
    <EmptyState
      v-else-if="notificationStore.notifications.length === 0"
      icon="🔔"
      :text="t('notification.empty')"
    />

    <template v-else>
      <div
        v-for="n in notificationStore.notifications"
        :key="n.id"
        class="notification-item"
        :class="{ unread: !n.read }"
        @click="handleClick(n)"
      >
        <div class="notification-icon">{{ typeIcon[n.type] || '📌' }}</div>
        <div class="notification-body">
          <div class="notification-title">{{ n.title }}</div>
          <div v-if="n.content" class="notification-content">{{ n.content }}</div>
          <div class="notification-time">{{ timeAgo(n.createdAt) }}</div>
        </div>
        <el-button
          v-if="!n.read"
          type="primary"
          size="small"
          text
          @click.stop="handleMarkRead(n.id)"
        >
          {{ t('notification.markRead') }}
        </el-button>
      </div>

      <div v-if="notificationStore.notifications.length < notificationStore.total" class="load-more">
        <el-button :loading="notificationStore.loading" @click="loadMore">加载更多</el-button>
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
  // 标记已读
  if (!n.read) {
    notificationStore.markRead(n.id)
  }

  // 根据类型跳转
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
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
}

.notification-item:hover {
  background: var(--bg);
  box-shadow: var(--shadow);
}

.notification-item.unread {
  border-left: 3px solid var(--primary);
  background: var(--primary-bg);
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
}

.notification-content {
  font-size: 13px;
  color: var(--text2);
  line-height: 1.5;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: 12px;
  color: var(--text2);
}

.load-more {
  text-align: center;
  padding: 20px;
}

@media (max-width: 768px) {
  .notification-item {
    padding: 12px;
  }
  .notification-title {
    font-size: 13px;
  }
}
</style>
