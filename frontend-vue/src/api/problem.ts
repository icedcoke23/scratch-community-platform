import { get, post } from './request'
import type { Problem, Submission, PageResult } from '@/types'

export const problemApi = {
  list: (page = 1, size = 50) =>
    get<PageResult<Problem>>('/problem', { page, size }),
  getDetail: (id: number) =>
    get<Problem>(`/problem/${id}`),
  submit: (problemId: number, answer: string) =>
    post<Submission>('/judge/submit', { problemId, answer })
}
