import { Request, Response, NextFunction } from 'express';

interface RequestLog {
  method: string;
  path: string;
  query: Record<string, unknown>;
  ip: string;
  userAgent: string;
  startTime: number;
  duration?: number;
  statusCode?: number;
}

export function requestLogger(req: Request, res: Response, next: NextFunction): void {
  const startTime = Date.now();
  const log: RequestLog = {
    method: req.method,
    path: req.path,
    query: req.query as Record<string, unknown>,
    ip: req.ip || req.socket.remoteAddress || 'unknown',
    userAgent: req.get('user-agent') || 'unknown',
    startTime
  };

  res.on('finish', () => {
    log.duration = Date.now() - startTime;
    log.statusCode = res.statusCode;

    const logLevel = res.statusCode >= 500 ? 'error' : res.statusCode >= 400 ? 'warn' : 'info';
    const logMessage = `[${req.method}] ${req.path} - ${res.statusCode} - ${log.duration}ms`;

    if (logLevel === 'error') {
      console.error(logMessage, log);
    } else if (logLevel === 'warn') {
      console.warn(logMessage, log);
    } else {
      console.log(logMessage);
    }
  });

  next();
}

export function corsMiddleware(req: Request, res: Response, next: NextFunction): void {
  res.header('Access-Control-Allow-Origin', process.env.CORS_ORIGIN || '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  res.header('Access-Control-Allow-Credentials', 'true');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }

  next();
}
