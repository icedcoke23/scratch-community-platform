import { get, put } from './request'
import type { PageResult } from '@/types'

export const notificationApi = {
  getMyNotifications: (page = 1, size = 20) =>
    get<PageResult<Record<string, unknown>>>('/notification', { page, size }),
  getUnreadCount: () =>
    get<number>('/notification/unread-count'),
  markRead: (id: number) =>
    put<void>(`/notification/${id}/read`),
  markAllRead: () =>
    put<void>('/notification/read-all')
}
