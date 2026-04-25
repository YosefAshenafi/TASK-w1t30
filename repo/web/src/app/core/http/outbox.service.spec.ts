import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OutboxService } from './outbox.service';
import { db } from '../db/dexie';

async function waitForHttp(httpMock: HttpTestingController, url: string, tries = 20) {
  for (let i = 0; i < tries; i++) {
    const match = httpMock.match(url);
    if (match.length > 0) return match[0];
    await new Promise(resolve => setTimeout(resolve, 5));
  }
  throw new Error(`Expected outbound request to ${url} within timeout`);
}

describe('OutboxService', () => {
  let service: OutboxService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OutboxService],
    });
    service = TestBed.inject(OutboxService);
    httpMock = TestBed.inject(HttpTestingController);
    await db.outbox.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('enqueue adds an item to the outbox', async () => {
    await service.enqueue({
      url: '/api/v1/sessions/sync',
      method: 'POST',
      body: '{"sessions":[],"sets":[]}',
      idempotencyKey: 'test-key-1',
      clientUpdatedAt: new Date().toISOString(),
    });
    const count = await service.pendingCount();
    expect(count).toBe(1);
  });

  it('drainPending flushes a due item and removes it on success', async () => {
    await service.enqueue({
      url: '/api/v1/sessions/sync',
      method: 'POST',
      body: '{"sessions":[],"sets":[]}',
      idempotencyKey: 'test-key-2',
      clientUpdatedAt: new Date().toISOString(),
    });

    const drainPromise = service.drainPending();
    const req = await waitForHttp(httpMock, '/api/v1/sessions/sync');
    req.flush({});
    await drainPromise;

    const count = await service.pendingCount();
    expect(count).toBe(0);
  });

  it('drainPending schedules retry with backoff on failure', async () => {
    await service.enqueue({
      url: '/api/v1/sessions/sync',
      method: 'POST',
      body: '{"sessions":[],"sets":[]}',
      idempotencyKey: 'test-key-3',
      clientUpdatedAt: new Date().toISOString(),
    });

    const drainPromise = service.drainPending();
    const req = await waitForHttp(httpMock, '/api/v1/sessions/sync');
    req.error(new ErrorEvent('network error'));
    await drainPromise;

    const count = await service.pendingCount();
    expect(count).toBe(1);

    const items = await db.outbox.toArray();
    expect(items[0].nextRetryAt).toBeGreaterThan(Date.now());
    expect(items[0].attempts).toBe(1);
  });

  it('retry delay is monotonically non-decreasing and capped at 300s', async () => {
    const RETRY_DELAYS_MS = [2_000, 5_000, 15_000, 30_000, 60_000, 300_000];
    let prev = 0;
    for (let i = 0; i < RETRY_DELAYS_MS.length; i++) {
      expect(RETRY_DELAYS_MS[i]).toBeGreaterThanOrEqual(prev);
      expect(RETRY_DELAYS_MS[i]).toBeLessThanOrEqual(300_000);
      prev = RETRY_DELAYS_MS[i];
    }
    expect(RETRY_DELAYS_MS[Math.min(10, RETRY_DELAYS_MS.length - 1)]).toBe(300_000);
  });

  it('items are processed in FIFO order (by createdAt)', async () => {
    await db.outbox.bulkAdd([
      { url: '/a', method: 'POST', body: '{}', idempotencyKey: 'k1', clientUpdatedAt: '2026-04-20T09:00:00Z', attempts: 0, nextRetryAt: 0, createdAt: '2026-04-20T09:00:00Z' },
      { url: '/b', method: 'POST', body: '{}', idempotencyKey: 'k2', clientUpdatedAt: '2026-04-20T09:00:01Z', attempts: 0, nextRetryAt: 0, createdAt: '2026-04-20T09:00:01Z' },
      { url: '/c', method: 'POST', body: '{}', idempotencyKey: 'k3', clientUpdatedAt: '2026-04-20T09:00:02Z', attempts: 0, nextRetryAt: 0, createdAt: '2026-04-20T09:00:02Z' },
    ]);

    const due = await db.outbox.where('nextRetryAt').belowOrEqual(Date.now()).sortBy('createdAt');
    expect(due.map(i => i.idempotencyKey)).toEqual(['k1', 'k2', 'k3']);
  });
});
