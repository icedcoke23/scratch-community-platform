import { get, post, del } from './request'
import type { Project, Comment, PageResult } from '@/types'

export const socialApi = {
  getFeed: (sort = 'latest', page = 1, size = 20) =>
    get<PageResult<Project>>('/social/feed', { sort, page, size }),
  search: (q: string, page = 1, size = 20) =>
    get<PageResult<Project>>('/social/search', { q, page, size }),
  toggleLike: (projectId: number, liked: boolean) =>
    liked
      ? del<void>(`/social/project/${projectId}/like`)
      : post<void>(`/social/project/${projectId}/like`),
  isLiked: (projectId: number) =>
    get<boolean>(`/social/project/${projectId}/liked`),
  getComments: (projectId: number, page = 1, size = 50) =>
    get<PageResult<Comment>>(`/social/project/${projectId}/comments`, { page, size }),
  addComment: (projectId: number, content: string) =>
    post<void>('/social/comment', { projectId, content }),
  deleteComment: (commentId: number) =>
    del<void>(`/social/comment/${commentId}`),
  getWeeklyRank: (topN = 10) =>
    get<unknown[]>('/social/rank/like/weekly', { topN }),
  getMonthlyRank: (topN = 10) =>
    get<unknown[]>('/social/rank/like/monthly', { topN })
}
