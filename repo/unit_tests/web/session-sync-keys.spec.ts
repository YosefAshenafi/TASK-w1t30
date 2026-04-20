import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpHandler } from '@angular/common/http';
import { SessionStore } from '../../web/src/app/sessions/session.store';

/**
 * Regression test for the offline-sync idempotency-key strategy.
 *
 * Before the fix, the client always sent `session-<id>` and `set-<id>` as
 * idempotency keys for the same session, so the server's idempotency store
 * would fail subsequent updates (different hash → IDEMPOTENCY_MISMATCH).
 * The fix binds the key to clientUpdatedAt so each distinct mutation carries
 * a distinct key, while retries of the same mutation remain deduped.
 */
describe('SessionStore idempotency key strategy', () => {
  let store: SessionStore;

  beforeEach(() => {
    const httpStub = {
      post: () => ({ subscribe: () => ({}) }),
    };
    TestBed.configureTestingModule({
      providers: [
        SessionStore,
        { provide: HttpClient, useValue: httpStub },
        { provide: HttpHandler, useValue: {} },
      ],
    });
    store = TestBed.inject(SessionStore);
  });

  it('different clientUpdatedAt yields different keys (fresh updates sync)', () => {
    const keyStamp = (store as unknown as {
      keyStamp(v: string | undefined): string;
    }).keyStamp.bind(store);

    const first = keyStamp('2026-04-20T09:00:00Z');
    const second = keyStamp('2026-04-20T09:05:00Z');
    expect(first).not.toBe(second);
  });

  it('same clientUpdatedAt yields the same key (retries stay dedupe-safe)', () => {
    const keyStamp = (store as unknown as {
      keyStamp(v: string | undefined): string;
    }).keyStamp.bind(store);

    const a = keyStamp('2026-04-20T09:00:00Z');
    const b = keyStamp('2026-04-20T09:00:00Z');
    expect(a).toBe(b);
  });

  it('undefined clientUpdatedAt is handled without throwing', () => {
    const keyStamp = (store as unknown as {
      keyStamp(v: string | undefined): string;
    }).keyStamp.bind(store);

    expect(() => keyStamp(undefined)).not.toThrow();
    expect(keyStamp(undefined)).toBe('0');
  });
});
