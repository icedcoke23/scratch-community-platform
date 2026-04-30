export type ProjectStatus = 'draft' | 'published';

export interface Project {
  id: string;
  title: string;
  description: string;
  tags: string[];
  status: ProjectStatus;
  blockCount: number;
  complexityScore: number;
  createdAt: Date;
  updatedAt: Date;
  sb3Path?: string;
}

export interface CreateProjectDTO {
  title: string;
  description?: string;
  tags?: string[];
}

export interface UpdateProjectDTO {
  title?: string;
  description?: string;
  tags?: string[];
  blockCount?: number;
  complexityScore?: number;
}

export interface ApiResponse<T = unknown> {
  code: number;
  data?: T;
  msg: string;
  timestamp?: number;
}

export interface PageParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: ProjectStatus;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface PaginationQuery {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  order?: 'asc' | 'desc';
}

export interface PaginationResponse<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}
