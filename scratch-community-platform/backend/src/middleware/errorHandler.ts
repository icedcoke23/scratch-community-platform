import { Request, Response, NextFunction } from 'express';
import { AppError } from '../types/errors';
import { ApiResponse } from '../types';
import { errorResponse } from '../utils/response';

interface ErrorResponse extends ApiResponse<null> {
  stack?: string;
}

export function errorHandler(
  err: Error,
  req: Request,
  res: Response,
  _next: NextFunction
): void {
  const response: ErrorResponse = {
    code: 500,
    msg: 'Internal server error',
    timestamp: Date.now()
  };

  if (err instanceof AppError) {
    response.code = err.code;
    response.msg = err.message;
    res.status(err.statusCode).json(response);
    return;
  }

  if (err.name === 'SyntaxError' && 'body' in err) {
    response.code = 400;
    response.msg = 'Invalid JSON in request body';
    res.status(400).json(response);
    return;
  }

  if (err.name === 'MulterError') {
    const multerError = err as Error & { code?: string; message: string };
    if (multerError.code === 'LIMIT_FILE_SIZE') {
      response.code = 413;
      response.msg = 'File size exceeds the limit';
    } else {
      response.code = 400;
      response.msg = multerError.message || 'File upload error';
    }
    res.status(response.code).json(response);
    return;
  }

  if (process.env.NODE_ENV === 'development') {
    response.stack = err.stack;
  }

  console.error('[ErrorHandler]', {
    name: err.name,
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method
  });

  res.status(500).json(response);
}

export function notFoundHandler(req: Request, res: Response): void {
  const response: ApiResponse<null> = {
    code: 404,
    msg: `Route ${req.method} ${req.path} not found`,
    timestamp: Date.now()
  };
  res.status(404).json(response);
}

export function asyncHandler<T>(
  fn: (req: Request, res: Response, next: NextFunction) => Promise<T>
) {
  return (req: Request, res: Response, next: NextFunction): void => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}
