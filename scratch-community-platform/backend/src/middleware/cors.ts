import { Request, Response, NextFunction } from 'express';
import cors from 'cors';

const allowedOrigins = process.env.ALLOWED_ORIGINS?.split(',') || [
  'http://localhost:3000',
  'http://localhost:8080'
];

const corsOptions: cors.CorsOptions = {
  origin: (origin, callback) => {
    if (!origin) {
      callback(null, true);
      return;
    }

    if (allowedOrigins.includes(origin)) {
      callback(null, true);
      return;
    }

    console.warn(`[CORS] Blocked request from origin: ${origin}`);
    callback(new Error('Not allowed by CORS'));
  },
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: [
    'Content-Type',
    'Authorization',
    'X-Request-ID',
    'X-Requested-With',
    'Accept',
    'Origin'
  ],
  exposedHeaders: ['X-Request-ID', 'X-RateLimit-Limit', 'X-RateLimit-Remaining', 'X-RateLimit-Reset'],
  credentials: true,
  maxAge: 86400,
  optionsSuccessStatus: 204
};

export function corsMiddleware(req: Request, res: Response, next: NextFunction): void {
  cors(corsOptions)(req, res, (err) => {
    if (err) {
      console.warn(`[CORS] CORS error: ${err.message}`);
      res.status(403).json({
        code: 403,
        msg: 'CORS policy violation',
        timestamp: Date.now()
      });
      return;
    }
    next();
  });
}

export { allowedOrigins };
