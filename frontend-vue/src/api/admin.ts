import { get, post } from './request'
import type { User, DashboardData, PageResult } from '@/types'

export const adminApi = {
  getDashboard: () =>
    get<DashboardData>('/admin/dashboard'),
  listUsers: (page = 1, size = 20) =>
    get<PageResult<User>>('/admin/user', { page, size }),
  disableUser: (id: number) =>
    post<void>(`/admin/user/${id}/disable`),
  enableUser: (id: number) =>
    post<void>(`/admin/user/${id}/enable`)
}
