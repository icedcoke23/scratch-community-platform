import { get, post, put, del } from './request'
import type { Project, ProjectDetail, ApiResponse } from '@/types'
import api from './request'

export const projectApi = {
  getDetail: (id: number) =>
    get<ProjectDetail>(`/project/${id}`),
  create: (data: { title: string; description?: string; tags?: string }) =>
    post<Project>('/project', data),
  update: (id: number, data: { title?: string; description?: string; tags?: string }) =>
    put<void>(`/project/${id}`, data),
  delete: (id: number) =>
    del<void>(`/project/${id}`),
  publish: (id: number) =>
    post<void>(`/project/${id}/publish`),
  remix: (id: number) =>
    post<Project>(`/project/${id}/remix`),
  getRemixes: (id: number, page = 1, size = 20) =>
    get<Project>(`/project/${id}/remixes`, { page, size }),
  getSb3Url: (id: number) =>
    get<string>(`/project/${id}/sb3`),
  uploadSb3: async (id: number, file: File): Promise<ApiResponse<void>> => {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post(`/project/${id}/sb3`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return res.data as ApiResponse<void>
  }
}
