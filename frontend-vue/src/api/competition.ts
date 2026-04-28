import { get, post } from './request'
import type { Competition, CompetitionRanking, PageResult } from '@/types'

export const competitionApi = {
  list: (page = 1, size = 20) =>
    get<PageResult<Competition>>('/competition', { page, size }),
  getDetail: (id: number) =>
    get<Competition>(`/competition/${id}`),
  register: (id: number) =>
    post<void>(`/competition/${id}/register`),
  getRanking: (id: number, page = 1, size = 50) =>
    get<PageResult<CompetitionRanking>>(`/competition/${id}/ranking`, { page, size }),
  create: (data: {
    title: string; description?: string; type: string
    startTime: string; endTime: string; problemIds: number[]
  }) => post<Competition>('/competition', data)
}
