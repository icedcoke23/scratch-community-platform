import { Request, Response, NextFunction } from 'express';
import { ValidationError } from '../types/errors';
import { ApiResponse } from '../types';

export interface ValidationRule {
  field: string;
  required?: boolean;
  type?: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'email' | 'url';
  minLength?: number;
  maxLength?: number;
  min?: number;
  max?: number;
  pattern?: RegExp;
  enum?: (string | number)[];
  custom?: (value: unknown) => boolean | string;
}

export function validate(rules: ValidationRule[]) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const errors: string[] = [];
    const body = req.body;
    const params = req.params;
    const query = req.query;

    for (const rule of rules) {
      let value: unknown;

      if (rule.field.startsWith('params.')) {
        value = params[rule.field.replace('params.', '')];
      } else if (rule.field.startsWith('query.')) {
        value = query[rule.field.replace('query.', '')];
      } else {
        value = body[rule.field];
      }

      if (rule.required && (value === undefined || value === null || value === '')) {
        errors.push(`Field '${rule.field}' is required`);
        continue;
      }

      if (value === undefined || value === null || value === '') {
        continue;
      }

      if (rule.type) {
        const typeError = validateType(rule.field, value, rule.type);
        if (typeError) {
          errors.push(typeError);
          continue;
        }
      }

      if (rule.type === 'string' && typeof value === 'string') {
        if (rule.minLength !== undefined && value.length < rule.minLength) {
          errors.push(`Field '${rule.field}' must be at least ${rule.minLength} characters`);
        }
        if (rule.maxLength !== undefined && value.length > rule.maxLength) {
          errors.push(`Field '${rule.field}' must not exceed ${rule.maxLength} characters`);
        }
      }

      if (rule.type === 'number' && typeof value === 'number') {
        if (rule.min !== undefined && value < rule.min) {
          errors.push(`Field '${rule.field}' must be at least ${rule.min}`);
        }
        if (rule.max !== undefined && value > rule.max) {
          errors.push(`Field '${rule.field}' must not exceed ${rule.max}`);
        }
      }

      if (rule.pattern && typeof value === 'string' && !rule.pattern.test(value)) {
        errors.push(`Field '${rule.field}' has invalid format`);
      }

      if (rule.enum && !rule.enum.includes(value as string | number)) {
        errors.push(`Field '${rule.field}' must be one of: ${rule.enum.join(', ')}`);
      }

      if (rule.custom) {
        const customResult = rule.custom(value);
        if (customResult !== true && typeof customResult === 'string') {
          errors.push(customResult);
        } else if (customResult !== true) {
          errors.push(`Field '${rule.field}' failed custom validation`);
        }
      }
    }

    if (errors.length > 0) {
      const response: ApiResponse<null> = {
        code: 400,
        msg: errors.join('; '),
        timestamp: Date.now()
      };
      res.status(400).json(response);
      return;
    }

    next();
  };
}

function validateType(field: string, value: unknown, type: ValidationRule['type']): string | null {
  switch (type) {
    case 'string':
      if (typeof value !== 'string') {
        return `Field '${field}' must be a string`;
      }
      break;
    case 'number':
      if (typeof value !== 'number' || isNaN(value as number)) {
        return `Field '${field}' must be a number`;
      }
      break;
    case 'boolean':
      if (typeof value !== 'boolean') {
        return `Field '${field}' must be a boolean`;
      }
      break;
    case 'array':
      if (!Array.isArray(value)) {
        return `Field '${field}' must be an array`;
      }
      break;
    case 'object':
      if (typeof value !== 'object' || value === null || Array.isArray(value)) {
        return `Field '${field}' must be an object`;
      }
      break;
    case 'email':
      if (typeof value !== 'string' || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return `Field '${field}' must be a valid email address`;
      }
      break;
    case 'url':
      if (typeof value !== 'string') {
        return `Field '${field}' must be a string to be a valid URL`;
      }
      try {
        new URL(value);
      } catch {
        return `Field '${field}' must be a valid URL`;
      }
      break;
  }
  return null;
}

export const projectValidation = {
  create: validate([
    { field: 'title', required: true, type: 'string', minLength: 1, maxLength: 100 },
    { field: 'description', type: 'string', maxLength: 2000 },
    { field: 'tags', type: 'array' }
  ]),

  update: validate([
    { field: 'title', type: 'string', minLength: 1, maxLength: 100 },
    { field: 'description', type: 'string', maxLength: 2000 },
    { field: 'tags', type: 'array' },
    { field: 'blockCount', type: 'number', min: 0 },
    { field: 'complexityScore', type: 'number', min: 0, max: 100 }
  ]),

  getById: validate([
    { field: 'params.id', required: true, type: 'string', pattern: /^[a-zA-Z0-9-_]+$/ }
  ])
};

export const userValidation = {
  register: validate([
    { field: 'username', required: true, type: 'string', minLength: 3, maxLength: 20, pattern: /^[a-zA-Z0-9_]+$/ },
    { field: 'password', required: true, type: 'string', minLength: 6, maxLength: 50 },
    { field: 'nickname', required: true, type: 'string', minLength: 1, maxLength: 30 },
    { field: 'email', type: 'email' }
  ]),

  login: validate([
    { field: 'username', required: true, type: 'string' },
    { field: 'password', required: true, type: 'string' }
  ])
};

export function validateFileType(allowedTypes: string[]) {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.file) {
      next();
      return;
    }

    const file = req.file;
    const ext = file.originalname.split('.').pop()?.toLowerCase();
    const mimeType = file.mimetype;

    const isValidExtension = ext && allowedTypes.some(type => type.startsWith('.') ? type.slice(1) === ext : type.includes(ext));
    const isValidMime = allowedTypes.some(type => mimeType.includes(type) || type === ext);

    if (!isValidExtension && !isValidMime) {
      const response: ApiResponse<null> = {
        code: 400,
        msg: `Invalid file type. Allowed types: ${allowedTypes.join(', ')}`,
        timestamp: Date.now()
      };
      res.status(400).json(response);
      return;
    }

    next();
  };
}

export function validateFileSize(maxSizeBytes: number) {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.file) {
      next();
      return;
    }

    if (req.file.size > maxSizeBytes) {
      const maxSizeMB = (maxSizeBytes / (1024 * 1024)).toFixed(2);
      const response: ApiResponse<null> = {
        code: 413,
        msg: `File size exceeds the limit of ${maxSizeMB}MB`,
        timestamp: Date.now()
      };
      res.status(413).json(response);
      return;
    }

    next();
  };
}

export const ALLOWED_SB3_TYPES = ['.sb3', 'application/zip', 'application/x-scratch3', 'application/vnd.scratch.sb3'];
export const MAX_SB3_SIZE = 10 * 1024 * 1024;
