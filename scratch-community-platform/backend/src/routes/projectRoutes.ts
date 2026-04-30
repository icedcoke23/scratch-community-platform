import { Router } from 'express';
import multer from 'multer';
import path from 'path';
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

const ALLOWED_MIMETYPES = [
  'application/zip',
  'application/x-zip-compressed',
  'application/sb3',
  'application/x-scratch-project',
  'application/vnd.scratch.sb3'
];

const MAX_FILE_SIZE = 50 * 1024 * 1024;

const upload = multer({
  storage: multer.diskStorage({
    destination: './public/sb3',
    filename: (req, file, cb) => {
      const projectId = req.params.id;
      if (!projectId || !/^[a-zA-Z0-9-_]+$/.test(projectId)) {
        cb(new Error('Invalid project ID'));
        return;
      }
      const ext = path.extname(file.originalname).toLowerCase() || '.sb3';
      cb(null, `${projectId}${ext}`);
    }
  }),
  limits: {
    fileSize: MAX_FILE_SIZE,
    files: 1
  },
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const allowedExtensions = ['.sb3', '.zip'];
    const isValidExtension = allowedExtensions.includes(ext);
    const isValidMime = ALLOWED_MIMETYPES.includes(file.mimetype);

    if (isValidExtension && (isValidMime || file.mimetype === 'application/zip')) {
      cb(null, true);
    } else {
      cb(new Error('只支持 .sb3 和 .zip 格式文件'));
    }
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
router.post('/:id/sb3/auto-save', upload.single('file'), handleAutoSaveSb3);

export default router;
