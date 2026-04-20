import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';

const MUTATION_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function uuidv4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export const idempotencyInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  if (!MUTATION_METHODS.has(req.method) || req.headers.has('Idempotency-Key')) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'Idempotency-Key': uuidv4() } }));
};
