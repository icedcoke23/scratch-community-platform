import helmet from 'helmet';
import { Request, Response, NextFunction } from 'express';

const TURBOWARP_ORIGIN = process.env.TURBOWARP_ORIGIN || 'https://turbowarp.org';

export function securityMiddleware(req: Request, res: Response, next: NextFunction): void {
  helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'", "'unsafe-inline'", TURBOWARP_ORIGIN],
        styleSrc: ["'self'", "'unsafe-inline'", TURBOWARP_ORIGIN],
        imgSrc: ["'self'", "data:", "https:", "blob:"],
        fontSrc: ["'self'", "data:", TURBOWARP_ORIGIN],
        connectSrc: ["'self'", TURBOWARP_ORIGIN],
        frameSrc: ["'self'", TURBOWARP_ORIGIN],
        objectSrc: ["'none'"],
        mediaSrc: ["'self'", "data:", "blob:"],
        baseUri: ["'self'"],
        formAction: ["'self'"],
        frameAncestors: ["'none'"],
        upgradeInsecureRequests: process.env.NODE_ENV === 'production' ? [] : null
      }
    },
    crossOriginEmbedderPolicy: false,
    crossOriginResourcePolicy: {
      policy: 'cross-origin'
    },
    dnsPrefetchControl: {
      allow: false
    },
    frameguard: {
      action: 'sameorigin'
    },
    hidePoweredBy: true,
    hsts: {
      maxAge: 31536000,
      includeSubDomains: true,
      preload: true
    },
    ieNoOpen: true,
    noSniff: true,
    originAgentCluster: true,
    permittedCrossDomainPolicies: {
      permittedPolicies: 'none'
    },
    referrerPolicy: {
      policy: 'strict-origin-when-cross-origin'
    },
    xssFilter: true
  })(req, res, next);
}

export function addSecurityHeaders(req: Request, res: Response, next: NextFunction): void {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');

  if (process.env.NODE_ENV === 'production') {
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  }

  next();
}
