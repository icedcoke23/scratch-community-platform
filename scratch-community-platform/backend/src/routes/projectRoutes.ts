import { Router } from 'express';
import multer from 'multer';
import {
  handleCreateProject,
  handleGetProject,
  handleGetProjects,
  handleUpdateProject,
  handlePublishProject,
  handleDeleteProject,
  handleUploadSb3,
  handleDownloadSb3,
  handleAutoSaveSb3
} from '../controllers/projectController';

const router = Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 10 * 1024 * 1024
  }
});

router.post('/', handleCreateProject);
router.get('/', handleGetProjects);
router.get('/:id', handleGetProject);
router.put('/:id', handleUpdateProject);
router.post('/:id/publish', handlePublishProject);
router.delete('/:id', handleDeleteProject);
router.post('/:id/sb3/upload', upload.single('file'), handleUploadSb3);
router.get('/:id/sb3/download', handleDownloadSb3);
router.post('/:id/sb3/auto-save', handleAutoSaveSb3);

export default router;
