import { Request, Response, NextFunction } from 'express';
import { ApiResponse } from '../types';

export function notFoundHandler(req: Request, res: Response, next: NextFunction): void {
  const response: ApiResponse<null> = {
    code: 404,
    msg: `Route ${req.originalUrl} not found`,
    timestamp: Date.now()
  };
  res.status(404).json(response);
}

export function methodNotAllowedHandler(allowedMethods: string[]) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const response: ApiResponse<null> = {
      code: 405,
      msg: `Method ${req.method} is not allowed for ${req.originalUrl}. Allowed methods: ${allowedMethods.join(', ')}`,
      timestamp: Date.now()
    };
    res.status(405).json(response);
  };
}
