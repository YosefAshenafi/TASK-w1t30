import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { db, OutboxRecord } from '../db/dexie';

const RETRY_DELAYS_MS = [2_000, 5_000, 15_000, 30_000, 60_000, 300_000];

@Injectable({ providedIn: 'root' })
export class OutboxService {
  constructor(private http: HttpClient) {}

  async enqueue(item: Omit<OutboxRecord, 'id' | 'attempts' | 'nextRetryAt' | 'createdAt'>): Promise<void> {
    await db.outbox.add({
      ...item,
      attempts: 0,
      nextRetryAt: Date.now(),
      createdAt: new Date().toISOString(),
    });
  }

  async drainPending(): Promise<void> {
    const now = Date.now();
    const due = await db.outbox.where('nextRetryAt').belowOrEqual(now).sortBy('createdAt');

    for (const record of due) {
      await this.flush(record);
    }
  }

  pendingCount(): Promise<number> {
    return db.outbox.count();
  }

  private async flush(record: OutboxRecord): Promise<void> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Idempotency-Key': record.idempotencyKey,
    });

    try {
      await this.http.request(record.method, record.url, {
        body: record.body,
        headers,
      }).toPromise();

      // Success — remove from outbox
      if (record.id !== undefined) {
        await db.outbox.delete(record.id);
      }
    } catch {
      const attempt = record.attempts + 1;
      const delayMs = RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)];
      if (record.id !== undefined) {
        await db.outbox.update(record.id, {
          attempts: attempt,
          nextRetryAt: Date.now() + delayMs,
        });
      }
    }
  }
}
