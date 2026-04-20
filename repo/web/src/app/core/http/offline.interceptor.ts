import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { of, throwError } from 'rxjs';
import { NetworkStatusService } from '../stores/network-status.service';
import { OutboxService } from './outbox.service';

const MUTATION_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);
const SKIP_URLS = ['/auth/', '/health'];

export const offlineInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const network = inject(NetworkStatusService);
  const outbox = inject(OutboxService);

  const shouldQueue = !network.isOnline()
    && MUTATION_METHODS.has(req.method)
    && !SKIP_URLS.some(u => req.url.includes(u));

  if (!shouldQueue) {
    return next(req);
  }

  const idempotencyKey = req.headers.get('Idempotency-Key') ?? crypto.randomUUID();

  outbox.enqueue({
    method: req.method as 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    url: req.url,
    body: req.body,
    idempotencyKey,
    clientUpdatedAt: new Date().toISOString(),
  });

  return of(new HttpResponse({ status: 202, body: { queued: true } }));
};
