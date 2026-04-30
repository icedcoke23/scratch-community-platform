import { Request, Response, NextFunction } from 'express';
import { ApiResponse } from '../types';

export interface PaginatedResponse<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export function successResponse<T>(data: T, msg: string = 'success'): ApiResponse<T> {
  return {
    code: 0,
    data,
    msg,
    timestamp: Date.now()
  };
}

export function errorResponse<T = null>(msg: string, code: number = 400): ApiResponse<T> {
  return {
    code,
    data: null as T,
    msg,
    timestamp: Date.now()
  };
}

export function sendSuccess<T>(res: Response, data: T, msg: string = 'success', statusCode: number = 200): void {
  res.status(statusCode).json(successResponse(data, msg));
}

export function sendError(res: Response, msg: string, code: number, statusCode?: number): void {
  const status = statusCode ?? getStatusFromCode(code);
  res.status(status).json(errorResponse(msg, code));
}

function getStatusFromCode(code: number): number {
  if (code >= 400 && code < 600) {
    return code;
  }
  if (code >= 1000 && code < 2000) {
    return 400;
  }
  if (code >= 2000 && code < 3000) {
    return 400;
  }
  if (code >= 3000 && code < 4000) {
    return 400;
  }
  return 500;
}

export function paginatedResponse<T>(
  records: T[],
  total: number,
  page: number,
  pageSize: number
): ApiResponse<PaginatedResponse<T>> {
  return successResponse({
    records,
    total,
    page,
    pageSize,
    totalPages: Math.ceil(total / pageSize)
  });
}

export function listResponse<T>(
  records: T[],
  total: number,
  size: number,
  current: number
): ApiResponse<{ records: T[]; total: number; size: number; current: number; pages: number }> {
  return successResponse({
    records,
    total,
    size,
    current,
    pages: Math.ceil(total / size)
  });
}
