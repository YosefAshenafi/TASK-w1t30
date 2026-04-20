import { Injectable } from '@angular/core';
import { db, AttemptDraftRecord } from '../core/db/dexie';
import { OutboxService } from '../core/http/outbox.service';

@Injectable({ providedIn: 'root' })
export class AssessmentDraftService {
  constructor(private outbox: OutboxService) {}

  async saveDraft(sessionId: string, itemId: string, chosenAnswer: string | null): Promise<AttemptDraftRecord> {
    const id = `${sessionId}:${itemId}`;
    const now = new Date().toISOString();
    const record: AttemptDraftRecord = {
      id,
      sessionId,
      itemId,
      chosenAnswer,
      clientUpdatedAt: now,
    };
    await db.attemptDrafts.put(record);
    await this.queueSync(record);
    return record;
  }

  async getDraft(sessionId: string, itemId: string): Promise<AttemptDraftRecord | undefined> {
    return db.attemptDrafts.get(`${sessionId}:${itemId}`);
  }

  async listForSession(sessionId: string): Promise<AttemptDraftRecord[]> {
    return db.attemptDrafts.where('sessionId').equals(sessionId).toArray();
  }

  async removeDraft(sessionId: string, itemId: string): Promise<void> {
    await db.attemptDrafts.delete(`${sessionId}:${itemId}`);
  }

  async clearForSession(sessionId: string): Promise<void> {
    await db.attemptDrafts.where('sessionId').equals(sessionId).delete();
  }

  private async queueSync(record: AttemptDraftRecord): Promise<void> {
    await this.outbox.enqueue({
      method: 'POST',
      url: '/api/v1/sessions/attempt-drafts',
      body: {
        id: record.id,
        sessionId: record.sessionId,
        itemId: record.itemId,
        chosenAnswer: record.chosenAnswer,
        clientUpdatedAt: record.clientUpdatedAt,
      },
      idempotencyKey: `draft-${record.id}-${record.clientUpdatedAt}`,
      clientUpdatedAt: record.clientUpdatedAt,
    });
  }
}
