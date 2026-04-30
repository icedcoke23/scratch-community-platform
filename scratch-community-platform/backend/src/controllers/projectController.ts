import { Request, Response } from 'express';
import { projectService } from '../services/projectService';
import { CreateProjectDTO, UpdateProjectDTO, ApiResponse } from '../types/project';
import path from 'path';
import fs from 'fs';

function successResponse<T>(data: T): ApiResponse<T> {
  return {
    code: 0,
    data,
    msg: 'success'
  };
}

function errorResponse(msg: string, code: number = 1): ApiResponse<null> {
  return {
    code,
    msg
  };
}

export const handleCreateProject = async (req: Request, res: Response) => {
  try {
    const dto: CreateProjectDTO = req.body;
    if (!dto.title) {
      return res.status(400).json(errorResponse('Title is required'));
    }

    const project = await projectService.createProject(dto);
    res.status(201).json(successResponse(project));
  } catch (error) {
    console.error('Create project error:', error);
    res.status(500).json(errorResponse('Failed to create project'));
  }
};

export const handleGetProject = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const project = await projectService.getProject(id);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    res.json(successResponse(project));
  } catch (error) {
    console.error('Get project error:', error);
    res.status(500).json(errorResponse('Failed to get project'));
  }
};

export const handleGetProjects = async (req: Request, res: Response) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const pageSize = parseInt(req.query.pageSize as string) || 10;
    const status = req.query.status as string;
    const search = req.query.search as string;
    const sortBy = req.query.sortBy as string || 'createdAt';
    const sortOrder = (req.query.sortOrder as string) || 'desc';

    const result = await projectService.getProjectsWithPagination({
      page,
      pageSize,
      status,
      search,
      sortBy,
      sortOrder: sortOrder === 'asc' ? 'asc' : 'desc'
    });

    res.json(successResponse(result));
  } catch (error) {
    console.error('Get projects error:', error);
    res.status(500).json(errorResponse('Failed to get projects'));
  }
};

export const handleUpdateProject = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const dto: UpdateProjectDTO = req.body;

    const project = await projectService.updateProject(id, dto);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    res.json(successResponse(project));
  } catch (error) {
    console.error('Update project error:', error);
    res.status(500).json(errorResponse('Failed to update project'));
  }
};

export const handlePublishProject = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const project = await projectService.publishProject(id);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    res.json(successResponse(project));
  } catch (error) {
    console.error('Publish project error:', error);
    res.status(500).json(errorResponse('Failed to publish project'));
  }
};

export const handleDeleteProject = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const deleted = await projectService.deleteProject(id);

    if (!deleted) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    res.json(successResponse({ deleted: true }));
  } catch (error) {
    console.error('Delete project error:', error);
    res.status(500).json(errorResponse('Failed to delete project'));
  }
};

export const handleUploadSb3 = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const project = await projectService.getProject(id);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    if (!req.file) {
      return res.status(400).json(errorResponse('No file uploaded'));
    }

    const sb3Path = `/sb3/${id}.sb3`;
    const uploadDir = path.join(__dirname, '../../public/sb3');
    
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const destPath = path.join(__dirname, '../../public/sb3', `${id}.sb3`);
    fs.writeFileSync(destPath, req.file.buffer);

    await projectService.updateSb3Path(id, sb3Path);

    res.json(successResponse({
      sb3Path,
      filename: req.file.originalname,
      size: req.file.size
    }));
  } catch (error) {
    console.error('Upload SB3 error:', error);
    res.status(500).json(errorResponse('Failed to upload SB3 file'));
  }
};

export const handleDownloadSb3 = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const project = await projectService.getProject(id);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    const sb3Path = path.join(__dirname, '../../public/sb3', `${id}.sb3`);

    if (!fs.existsSync(sb3Path)) {
      return res.status(404).json(errorResponse('SB3 file not found'));
    }

    res.download(sb3Path, `${project.title}.sb3`);
  } catch (error) {
    console.error('Download SB3 error:', error);
    res.status(500).json(errorResponse('Failed to download SB3 file'));
  }
};

export const handleAutoSaveSb3 = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const project = await projectService.getProject(id);

    if (!project) {
      return res.status(404).json(errorResponse('Project not found'));
    }

    if (!req.body.sb3Data) {
      return res.status(400).json(errorResponse('SB3 data is required'));
    }

    const uploadDir = path.join(__dirname, '../../public/sb3');
    
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const sb3Path = path.join(uploadDir, `${id}_autosave.sb3`);
    const sb3Data = Buffer.from(req.body.sb3Data, 'base64');
    fs.writeFileSync(sb3Path, sb3Data);

    res.json(successResponse({
      autoSavedAt: new Date().toISOString(),
      path: `/sb3/${id}_autosave.sb3`
    }));
  } catch (error) {
    console.error('Auto-save SB3 error:', error);
    res.status(500).json(errorResponse('Failed to auto-save SB3 file'));
  }
};
