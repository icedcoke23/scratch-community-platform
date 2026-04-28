import { get, post } from './request'
import type { Homework, HomeworkSubmission, PageResult } from '@/types'

export const homeworkApi = {
  listByClass: (classId: number, page = 1, size = 50) =>
    get<PageResult<Homework>>(`/homework/class/${classId}`, { page, size }),
  getDetail: (id: number) =>
    get<Homework>(`/homework/${id}`),
  submit: (homeworkId: number, projectId: number) =>
    post<void>('/homework/submit', { homeworkId, projectId }),
  getSubmissions: (homeworkId: number, page = 1, size = 50) =>
    get<PageResult<HomeworkSubmission>>(`/homework/${homeworkId}/submissions`, { page, size }),
  grade: (submissionId: number, score: number, comment?: string) =>
    post<void>('/homework/grade', { submissionId, score, comment })
}
