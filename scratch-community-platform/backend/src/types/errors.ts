export enum ErrorCode {
  SUCCESS = 0,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  METHOD_NOT_ALLOWED = 405,
  CONFLICT = 409,
  UNPROCESSABLE_ENTITY = 422,
  TOO_MANY_REQUESTS = 429,
  INTERNAL_SERVER_ERROR = 500,
  BAD_GATEWAY = 502,
  SERVICE_UNAVAILABLE = 503,

  VALIDATION_ERROR = 1001,
  INVALID_CREDENTIALS = 1002,
  TOKEN_EXPIRED = 1003,
  TOKEN_INVALID = 1004,
  USER_NOT_FOUND = 1005,
  USER_ALREADY_EXISTS = 1006,
  PROJECT_NOT_FOUND = 2001,
  PROJECT_FORBIDDEN = 2002,
  FILE_TOO_LARGE = 3001,
  INVALID_FILE_TYPE = 3002,
  UPLOAD_FAILED = 3003,
  SB3_NOT_FOUND = 3004,
}

export class AppError extends Error {
  public readonly code: ErrorCode;
  public readonly statusCode: number;
  public readonly isOperational: boolean;

  constructor(
    message: string,
    code: ErrorCode = ErrorCode.INTERNAL_SERVER_ERROR,
    statusCode?: number,
    isOperational: boolean = true
  ) {
    super(message);
    this.name = 'AppError';
    this.code = code;
    this.statusCode = statusCode ?? this.mapCodeToStatus(code);
    this.isOperational = isOperational;

    Error.captureStackTrace(this, this.constructor);
  }

  private mapCodeToStatus(code: ErrorCode): number {
    const mapping: Record<ErrorCode, number> = {
      [ErrorCode.SUCCESS]: 200,
      [ErrorCode.BAD_REQUEST]: 400,
      [ErrorCode.UNAUTHORIZED]: 401,
      [ErrorCode.FORBIDDEN]: 403,
      [ErrorCode.NOT_FOUND]: 404,
      [ErrorCode.METHOD_NOT_ALLOWED]: 405,
      [ErrorCode.CONFLICT]: 409,
      [ErrorCode.UNPROCESSABLE_ENTITY]: 422,
      [ErrorCode.TOO_MANY_REQUESTS]: 429,
      [ErrorCode.INTERNAL_SERVER_ERROR]: 500,
      [ErrorCode.BAD_GATEWAY]: 502,
      [ErrorCode.SERVICE_UNAVAILABLE]: 503,
      [ErrorCode.VALIDATION_ERROR]: 400,
      [ErrorCode.INVALID_CREDENTIALS]: 401,
      [ErrorCode.TOKEN_EXPIRED]: 401,
      [ErrorCode.TOKEN_INVALID]: 401,
      [ErrorCode.USER_NOT_FOUND]: 404,
      [ErrorCode.USER_ALREADY_EXISTS]: 409,
      [ErrorCode.PROJECT_NOT_FOUND]: 404,
      [ErrorCode.PROJECT_FORBIDDEN]: 403,
      [ErrorCode.FILE_TOO_LARGE]: 413,
      [ErrorCode.INVALID_FILE_TYPE]: 400,
      [ErrorCode.UPLOAD_FAILED]: 500,
      [ErrorCode.SB3_NOT_FOUND]: 404,
    };
    return mapping[code] ?? 500;
  }
}

export class ValidationError extends AppError {
  constructor(message: string) {
    super(message, ErrorCode.VALIDATION_ERROR, 400);
    this.name = 'ValidationError';
  }
}

export class NotFoundError extends AppError {
  constructor(resource: string, id?: string) {
    const message = id ? `${resource} with id '${id}' not found` : `${resource} not found`;
    super(message, ErrorCode.NOT_FOUND, 404);
    this.name = 'NotFoundError';
  }
}

export class UnauthorizedError extends AppError {
  constructor(message: string = 'Unauthorized access') {
    super(message, ErrorCode.UNAUTHORIZED, 401);
    this.name = 'UnauthorizedError';
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string = 'Access forbidden') {
    super(message, ErrorCode.FORBIDDEN, 403);
    this.name = 'ForbiddenError';
  }
}

export class ConflictError extends AppError {
  constructor(message: string) {
    super(message, ErrorCode.CONFLICT, 409);
    this.name = 'ConflictError';
  }
}
