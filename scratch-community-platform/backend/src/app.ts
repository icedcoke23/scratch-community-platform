import express, { Application, Request, Response } from 'express';
import cors from 'cors';
import multer from 'multer';
import path from 'path';
import projectRoutes from './routes/projectRoutes';
import { errorHandler, notFoundHandler, requestLogger } from './middleware';
import { ALLOWED_SB3_TYPES, MAX_SB3_SIZE } from './middleware/validation';

const app: Application = express();

app.use(requestLogger);
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

app.use('/public', express.static(path.join(__dirname, '../public')));

const storage = multer.memoryStorage();

const upload = multer({
  storage,
  limits: {
    fileSize: MAX_SB3_SIZE
  },
  fileFilter: (req, file, cb) => {
    const ext = file.originalname.split('.').pop()?.toLowerCase();
    const allowedExtensions = ['sb3', 'json', 'zip'];
    if (ext && allowedExtensions.includes(ext)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid file type'));
    }
  }
});

app.get('/health', (req: Request, res: Response) => {
  res.status(200).json({
    code: 0,
    data: { status: 'ok' },
    msg: 'success',
    timestamp: Date.now()
  });
});

app.use('/api/v1/project', projectRoutes);

app.post('/api/projects', upload.single('file'), (req: Request, res: Response) => {
  res.status(201).json({
    code: 0,
    data: { message: 'Project upload endpoint (to be implemented)' },
    msg: 'success',
    timestamp: Date.now()
  });
});

app.get('/api/projects', (req: Request, res: Response) => {
  res.status(200).json({
    code: 0,
    data: { message: 'Projects list endpoint (to be implemented)' },
    msg: 'success',
    timestamp: Date.now()
  });
});

app.get('/api/projects/:id', (req: Request, res: Response) => {
  res.status(200).json({
    code: 0,
    data: { message: `Project ${req.params.id} endpoint (to be implemented)` },
    msg: 'success',
    timestamp: Date.now()
  });
});

app.use(notFoundHandler);
app.use(errorHandler);

export default app;
