import { get, post, put, del } from './request'
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
  createClass: (data: { name: string; description?: string; grade?: string }) =>
    post<ClassRoom>('/class', data),
  joinClass: (code: string) =>
    post<void>(`/class/join?code=${encodeURIComponent(code)}`),
  getClassMembers: (classId: number) =>
    get<User[]>(`/class/${classId}/members`),
  removeClassMember: (classId: number, userId: number) =>
    del<void>(`/class/${classId}/member/${userId}`),
  searchUsers: (q: string, page = 1, size = 20) =>
    get<PageResult<User>>('/user/search', { q, page, size }),
  getMyInfo: () =>
    get<User>('/user/me'),
  getUserProfile: (userId: number) =>
    get<User>(`/user/${userId}/profile`),
  updateProfile: (data: { nickname?: string; email?: string; bio?: string }) =>
    put<void>('/user/me', data),
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    put<void>('/user/password', data),
  followUser: (userId: number) =>
    post<void>(`/user/${userId}/follow`),
  unfollowUser: (userId: number) =>
    del<void>(`/user/${userId}/follow`)
}
