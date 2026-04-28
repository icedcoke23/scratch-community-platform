import { get, post } from './request'
import type { PointLog, PageResult } from '@/types'

export const pointApi = {
  getMyPoints: () =>
    get<unknown>('/points/me'),
  checkin: () =>
    post<unknown>('/points/checkin'),
  getLogs: (page = 1, size = 20) =>
    get<PageResult<PointLog>>('/points/logs', { page, size }),
  getRanking: (topN = 10) =>
    get<unknown[]>('/points/ranking', { topN })
}
