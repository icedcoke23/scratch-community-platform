// API 类型定义

export interface User {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  bio?: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  status: number
  points?: number
  level?: number
  createdAt: string
}

export interface LoginVO {
  token: string
  refreshToken: string
  userInfo: User
}

export interface Project {
  id: number
  userId: number
  username?: string
  nickname?: string
  authorAvatar?: string
  title: string
  description?: string
  coverUrl?: string
  status: string
  blockCount?: number
  complexityScore?: number
  likeCount: number
  commentCount: number
  viewCount: number
  tags?: string
  remixProjectId?: number
  remixCount?: number
  createdAt: string
}

export interface ProjectDetail extends Project {
  sb3Url?: string
  parseResult?: string
  isLiked?: boolean
}

export interface Comment {
  id: number
  userId: number
  username?: string
  nickname?: string
  content: string
  createdAt: string
}

export interface Problem {
  id: number
  title: string
  description?: string
  type: 'choice' | 'true_false' | 'scratch_algo'
  difficulty: 'easy' | 'medium' | 'hard'
  score: number
  submitCount: number
  acceptCount: number
  options?: { key: string; text: string }[]
}

export interface Submission {
  id: number
  userId: number
  problemId: number
  verdict: 'PENDING' | 'AC' | 'WA' | 'TLE' | 'RE'
  judgeDetail?: string
  runtimeMs?: number
  createdAt: string
}

export interface Homework {
  id: number
  classId: number
  title: string
  description?: string
  totalScore: number
  submitCount: number
  gradedCount: number
  status: 'draft' | 'published' | 'closed'
  deadline?: string
  createdAt: string
}

export interface HomeworkSubmission {
  id: number
  homeworkId: number
  studentId: number
  username?: string
  nickname?: string
  projectId?: number
  score?: number
  comment?: string
  status: 'submitted' | 'graded' | 'returned'
  createdAt: string
}

export interface ClassRoom {
  id: number
  name: string
  description?: string
  teacherId: number
  inviteCode: string
  grade?: string
  studentCount: number
  createdAt: string
}

export interface PointLog {
  id: number
  userId: number
  type: string
  points: number
  totalPoints: number
  remark?: string
  createdAt: string
}

export interface Competition {
  id: number
  title: string
  description?: string
  creatorId: number
  type: 'TIMED' | 'RATED'
  startTime: string
  endTime: string
  problemCount: number
  participantCount: number
  totalScore: number
  status: 'DRAFT' | 'PUBLISHED' | 'RUNNING' | 'ENDED'
  registered?: boolean
  remainingSeconds?: number
  createdAt: string
}

export interface CompetitionRanking {
  rank: number
  userId: number
  username?: string
  nickname?: string
  totalScore: number
  solvedCount: number
  penalty: number
}

export interface AiReview {
  id: number
  projectId: number
  summary?: string
  starDisplay?: string
  dimensionScores?: Record<string, number>
  strengths?: string[]
  suggestions?: string[]
  blockCount?: number
  spriteCount?: number
  complexityScore?: number
  provider?: string
  createdAt: string
}

export interface DashboardData {
  totalUsers: number
  todayNewUsers: number
  totalProjects: number
  publishedProjects: number
  totalProblems: number
  totalSubmissions: number
  acSubmissions: number
  totalClasses: number
  totalHomework: number
  pendingAudits: number
}

export interface AnalyticsData {
  studentCount: number
  homeworkCount: number
  avgSubmitRate: number
  avgScore: number
  activeStudents7d: number
  typePassRates?: Record<string, number>
  homeworkStats?: {
    title: string
    submitRate: number
    avgScore: number
    submitCount: number
    gradedCount: number
  }[]
  studentRanks?: {
    username: string
    nickname?: string
    level: number
    submitRate: number
    avgScore: number
    totalPoints: number
  }[]
}

export interface PointRankItem {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  points: number
  level?: number
}

export interface RankItem {
  id: number
  userId: number
  username?: string
  nickname?: string
  avatarUrl?: string
  likeCount?: number
  projectCount?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Notification {
  id: number
  userId: number
  type: string
  title: string
  content?: string
  relatedId?: number
  read: boolean
  createdAt: string
}

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  timestamp?: number
}

export type { ErrorInfo, ApiError, ErrorContext, ErrorCodeType, ErrorCode } from './errors'
export { isApiError, getErrorTypeFromCode, formatErrorMessage } from './errors'

export interface PaginationParams {
  page?: number
  pageSize?: number
  sortBy?: string
  order?: 'asc' | 'desc'
}

export interface PaginationResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface ListQuery {
  page?: number
  pageSize?: number
  keyword?: string
}

export interface ListResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
