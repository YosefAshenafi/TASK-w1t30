import Dexie, { Table } from 'dexie';

export interface SessionRecord {
  id: string;
  studentId: string;
  courseId: string;
  cohortId: string | null;
  startedAt: string;
  endedAt: string | null;
  restSecondsDefault: number;
  status: 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'ABANDONED';
  clientUpdatedAt: string;
}

export interface SessionSetRecord {
  id: string;
  sessionId: string;
  activityId: string;
  setIndex: number;
  restSeconds: number;
  completedAt: string | null;
  notes: string | null;
  clientUpdatedAt: string;
}

export interface AttemptDraftRecord {
  id: string;
  sessionId: string;
  itemId: string;
  chosenAnswer: string | null;
  clientUpdatedAt: string;
}

export interface OutboxRecord {
  id?: number;
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  url: string;
  body: unknown;
  idempotencyKey: string;
  clientUpdatedAt: string;
  attempts: number;
  nextRetryAt: number;
  createdAt: string;
}

export interface ProfileCacheRecord {
  userId: string;
  profile: unknown;
  cachedAt: string;
}

export interface TemplatesCacheRecord {
  key: string;
  subject: string;
  bodyMarkdown: string;
  variables: string[];
  cachedAt: string;
}

export interface SyncStateRecord {
  singleton: 'state';
  lastSuccessfulSyncAt: string | null;
  pendingCount: number;
}

export class MeridianDatabase extends Dexie {
  sessions!: Table<SessionRecord, string>;
  sessionSets!: Table<SessionSetRecord, string>;
  attemptDrafts!: Table<AttemptDraftRecord, string>;
  outbox!: Table<OutboxRecord, number>;
  profileCache!: Table<ProfileCacheRecord, string>;
  templatesCache!: Table<TemplatesCacheRecord, string>;
  syncState!: Table<SyncStateRecord, string>;

  constructor() {
    super('meridian');
    this.version(1).stores({
      sessions: 'id, status, studentId, courseId, clientUpdatedAt',
      sessionSets: 'id, sessionId, activityId',
      attemptDrafts: 'id, sessionId, itemId',
      outbox: '++id, method, url, nextRetryAt, createdAt',
      profileCache: 'userId',
      templatesCache: 'key',
      syncState: 'singleton',
    });
  }
}

export const db = new MeridianDatabase();
