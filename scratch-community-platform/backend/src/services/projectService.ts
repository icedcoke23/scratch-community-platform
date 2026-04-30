import { Project, CreateProjectDTO, UpdateProjectDTO } from '../types/project';

interface PaginationParams {
  page: number;
  pageSize: number;
  status?: string;
  search?: string;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

interface PaginatedResult {
  list: Project[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

class ProjectService {
  private projects: Map<string, Project> = new Map();

  private generateId(): string {
    return `project_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  async createProject(dto: CreateProjectDTO): Promise<Project> {
    const project: Project = {
      id: this.generateId(),
      title: dto.title,
      description: dto.description || '',
      tags: dto.tags || [],
      status: 'draft',
      blockCount: 0,
      complexityScore: 0,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    this.projects.set(project.id, project);
    return project;
  }

  async getProject(id: string): Promise<Project | undefined> {
    return this.projects.get(id);
  }

  async getAllProjects(): Promise<Project[]> {
    return Array.from(this.projects.values()).sort(
      (a, b) => b.createdAt.getTime() - a.createdAt.getTime()
    );
  }

  async getProjectsWithPagination(params: PaginationParams): Promise<PaginatedResult> {
    let projects = Array.from(this.projects.values());

    if (params.status) {
      projects = projects.filter(p => p.status === params.status);
    }

    if (params.search) {
      const searchLower = params.search.toLowerCase();
      projects = projects.filter(p => 
        p.title.toLowerCase().includes(searchLower) ||
        p.description.toLowerCase().includes(searchLower)
      );
    }

    projects.sort((a, b) => {
      const aTime = params.sortBy === 'updatedAt' ? a.updatedAt.getTime() : a.createdAt.getTime();
      const bTime = params.sortBy === 'updatedAt' ? b.updatedAt.getTime() : b.createdAt.getTime();
      return params.sortOrder === 'asc' ? aTime - bTime : bTime - aTime;
    });

    const total = projects.length;
    const totalPages = Math.ceil(total / params.pageSize);
    const start = (params.page - 1) * params.pageSize;
    const list = projects.slice(start, start + params.pageSize);

    return {
      list,
      total,
      page: params.page,
      pageSize: params.pageSize,
      totalPages
    };
  }

  async updateProject(id: string, dto: UpdateProjectDTO): Promise<Project | null> {
    const project = this.projects.get(id);
    if (!project) {
      return null;
    }

    const updatedProject: Project = {
      ...project,
      title: dto.title !== undefined ? dto.title : project.title,
      description: dto.description !== undefined ? dto.description : project.description,
      tags: dto.tags !== undefined ? dto.tags : project.tags,
      blockCount: dto.blockCount !== undefined ? dto.blockCount : project.blockCount,
      complexityScore: dto.complexityScore !== undefined ? dto.complexityScore : project.complexityScore,
      updatedAt: new Date()
    };

    this.projects.set(id, updatedProject);
    return updatedProject;
  }

  async publishProject(id: string): Promise<Project | null> {
    const project = this.projects.get(id);
    if (!project) {
      return null;
    }

    const publishedProject: Project = {
      ...project,
      status: 'published',
      updatedAt: new Date()
    };

    this.projects.set(id, publishedProject);
    return publishedProject;
  }

  async deleteProject(id: string): Promise<boolean> {
    return this.projects.delete(id);
  }

  async updateSb3Path(id: string, sb3Path: string): Promise<Project | null> {
    const project = this.projects.get(id);
    if (!project) {
      return null;
    }

    const updatedProject: Project = {
      ...project,
      sb3Path,
      updatedAt: new Date()
    };

    this.projects.set(id, updatedProject);
    return updatedProject;
  }
}

export const projectService = new ProjectService();
