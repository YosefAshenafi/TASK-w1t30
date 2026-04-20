import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { db, SessionRecord, SessionSetRecord } from '../core/db/dexie';

const WRITE_THROUGH_DELAY_MS = 5_000;

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly _activeSessions$ = new BehaviorSubject<SessionRecord[]>([]);
  readonly activeSessions$ = this._activeSessions$.asObservable();

  private flushTimers = new Map<string, ReturnType<typeof setTimeout>>();

  constructor(private http: HttpClient) {}

  async loadActiveSessions(studentId: string): Promise<void> {
    const sessions = await db.sessions
      .where('studentId').equals(studentId)
      .and(s => s.status === 'IN_PROGRESS' || s.status === 'PAUSED')
      .toArray();
    this._activeSessions$.next(sessions);
  }

  async upsertSession(session: SessionRecord): Promise<void> {
    await db.sessions.put(session);
    const current = this._activeSessions$.getValue();
    const idx = current.findIndex(s => s.id === session.id);
    if (idx >= 0) {
      const updated = [...current];
      updated[idx] = session;
      this._activeSessions$.next(updated);
    } else {
      this._activeSessions$.next([...current, session]);
    }
    this.scheduleSyncDebounced(session.id);
  }

  async upsertSet(set: SessionSetRecord): Promise<void> {
    await db.sessionSets.put(set);
    this.scheduleSyncDebounced(set.sessionId);
  }

  async getSetsForSession(sessionId: string): Promise<SessionSetRecord[]> {
    return db.sessionSets.where('sessionId').equals(sessionId).sortBy('setIndex');
  }

  private scheduleSyncDebounced(sessionId: string): void {
    const existing = this.flushTimers.get(sessionId);
    if (existing) clearTimeout(existing);
    const timer = setTimeout(() => {
      this.flushSession(sessionId);
      this.flushTimers.delete(sessionId);
    }, WRITE_THROUGH_DELAY_MS);
    this.flushTimers.set(sessionId, timer);
  }

  private async flushSession(sessionId: string): Promise<void> {
    const session = await db.sessions.get(sessionId);
    if (!session) return;
    const sets = await this.getSetsForSession(sessionId);

    const payload = {
      sessions: [{
        id: session.id,
        studentId: session.studentId,
        courseId: session.courseId,
        cohortId: session.cohortId ?? null,
        startedAt: session.startedAt,
        endedAt: session.endedAt ?? null,
        restSecondsDefault: session.restSecondsDefault,
        status: session.status,
        clientUpdatedAt: session.clientUpdatedAt,
        idempotencyKey: session.id,
        sets: sets.map(s => ({
          id: s.id,
          activityId: s.activityId,
          setIndex: s.setIndex,
          restSeconds: s.restSeconds,
          completedAt: s.completedAt ?? null,
          notes: s.notes ?? null,
          clientUpdatedAt: s.clientUpdatedAt,
          idempotencyKey: s.id,
        })),
      }],
    };

    this.http.post('/api/v1/sessions/sync', payload).subscribe({
      error: err => console.warn('Session sync failed, will retry via outbox', err),
    });
  }
}
