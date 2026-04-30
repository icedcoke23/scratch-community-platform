import rateLimit from 'express-rate-limit';
import { Request, Response } from 'express';

export const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: {
    code: 429,
    msg: '请求过于频繁，请稍后再试',
    timestamp: Date.now()
  },
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request) => {
    return req.headers['x-forwarded-for'] as string ||
      req.ip ||
      'unknown';
  },
  handler: (req: Request, res: Response) => {
    res.status(429).json({
      code: 429,
      msg: '请求过于频繁，请稍后再试',
      timestamp: Date.now()
    });
  },
  skip: (req: Request) => {
    return req.path === '/health';
  }
});

export const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: {
    code: 429,
    msg: '登录尝试过于频繁，请稍后再试',
    timestamp: Date.now()
  },
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request) => {
    const forwarded = req.headers['x-forwarded-for'] as string;
    return forwarded ? forwarded.split(',')[0].trim() : req.ip || 'unknown';
  }
});

export const uploadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: {
    code: 429,
    msg: '上传过于频繁，请稍后再试',
    timestamp: Date.now()
  },
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req: Request) => {
    return req.headers['x-forwarded-for'] as string ||
      req.ip ||
      'unknown';
  }
});

export const searchLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  message: {
    code: 429,
    msg: '搜索请求过于频繁，请稍后再试',
    timestamp: Date.now()
  },
  standardHeaders: true,
  legacyHeaders: false
});
