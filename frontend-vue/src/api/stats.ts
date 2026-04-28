import { get } from './request'

export const statsApi = {
  getStats: () =>
    get<{ totalUsers: number; totalProjects: number; publishedProjects: number }>('/stats')
}
