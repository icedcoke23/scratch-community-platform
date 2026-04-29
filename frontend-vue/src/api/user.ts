import { get, post, put } from './request'
import type { LoginVO, ClassRoom, User, PageResult } from '@/types'

export const userApi = {
  login: (data: { username: string; password: string }) =>
    post<LoginVO>('/user/login', data),
  register: (data: { username: string; password: string; nickname: string; email?: string; role?: string }) =>
    post<void>('/user/register', data),
  logout: () =>
    post<void>('/user/logout'),
  refreshToken: () =>
    post<LoginVO>('/user/refresh'),
  getMyClasses: () =>
    get<ClassRoom[]>('/class'),
  searchUsers: (q: string, page = 1, size = 20) =>
    get<PageResult<User>>('/user/search', { q, page, size }),
  getMyInfo: () =>
    get<User>('/user/me'),
  updateProfile: (data: { nickname?: string; email?: string; bio?: string }) =>
    put<void>('/user/profile', data),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    post<void>('/user/change-password', data)
}
