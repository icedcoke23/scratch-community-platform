import { get } from './request'
import type { AnalyticsData } from '@/types'

export const analyticsApi = {
  getClassAnalytics: (classId: number) =>
    get<AnalyticsData>(`/analytics/class/${classId}`)
}
