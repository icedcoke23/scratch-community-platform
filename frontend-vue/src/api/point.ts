import { get, post } from './request'
import type { PointLog, PointRankItem, PageResult } from '@/types'

export const pointApi = {
  getMyPoints: () =>
    get<{ points: number; level: number; levelName: string; nextLevelPoints: number; currentLevelPoints: number; progress: number }>('/points/me'),
  checkin: () =>
    post<{ checkedIn: boolean; earned: number }>('/points/checkin'),
  getLogs: (page = 1, size = 20) =>
    get<PageResult<PointLog>>('/points/logs', { page, size }),
  getRanking: (topN = 10) =>
    get<PointRankItem[]>('/points/ranking', { topN })
}
