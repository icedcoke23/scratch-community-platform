import { get, post, put, del } from './request'
import type { User, DashboardData, PageResult } from '@/types'

export const adminApi = {
  // ==================== Dashboard ====================
  getDashboard: () =>
    get<DashboardData>('/admin/dashboard'),

  // ==================== 用户管理 ====================
  listUsers: (page = 1, size = 20) =>
    get<PageResult<User>>('/admin/user', { page, size }),
  disableUser: (id: number) =>
    post<void>(`/admin/user/${id}/disable`),
  enableUser: (id: number) =>
    post<void>(`/admin/user/${id}/enable`),
  updateUser: (id: number, data: { role?: string; status?: string }) =>
    put<void>(`/admin/user/${id}`, data),

  // ==================== 作品管理 ====================
  listProjects: (params: { keyword?: string; status?: string; page?: number; size?: number } = {}) =>
    get('/admin/project', params),
  getProjectStats: () =>
    get('/admin/project/stats'),
  updateProjectStatus: (id: number, status: string) =>
    put<void>(`/admin/project/${id}/status`, { status }),
  deleteProject: (id: number) =>
    del<void>(`/admin/project/${id}`),

  // ==================== 评论管理 ====================
  listComments: (params: { keyword?: string; page?: number; size?: number } = {}) =>
    get('/admin/comment', params),
  deleteComment: (id: number) =>
    del<void>(`/admin/comment/${id}`),
}
