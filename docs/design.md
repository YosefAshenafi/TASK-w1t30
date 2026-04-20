# Meridian Training Analytics Management System — Design

## 1. Overview & Goals

Meridian is an on-premise training analytics platform for training providers. It combines:

1. **Learner tooling** — offline-first training sessions with in-session and between-set rest timers and per-activity check-offs.
2. **Analytics** — mastery trends, wrong-answer distributions, weak knowledge points, item difficulty/discrimination at learner/cohort/course scopes.
3. **Operations & Reporting** — enrollments, seat utilization, refund/return rate, inventory, and certifications expiring within 30/60/90 days, with local CSV/PDF exports.
4. **Governance** — classification tiers, masking, audit, anomaly alerts, approval workflow, backup/DR — all fully offline-capable.

### Design goals

| Goal | Driven by |
|---|---|
| Fully usable without internet access | "offline-first session capture," IndexedDB, LAN-only backups |
| Deterministic multi-device sync | Idempotency keys + `clientUpdatedAt` last-write-wins |
| Scope isolation by organization | Corporate mentors see only their org |
| Auditable, recoverable, and encrypted | AES-256, BCrypt, audit log, 14-day recycle bin, DR drills |

## 2. Roles

| Role | Scope | Capabilities |
|---|---|---|
| **Student** | Self only | Register, run training sessions, review own analytics, manage own devices |
| **Corporate Mentor** | Own organization | Review org-scoped learners/cohorts/reports; no admin actions |
| **Faculty Mentor** | Assigned courses/cohorts | Author courses and assessment items; review analytics; no admin actions |
| **Administrator** | Global | Approve accounts and requests, manage templates/IP ranges, run exports, recycle bin, backups |

Account approval, unlock, permission changes, restoration, and high-risk exports are **Administrator-only** and pass through the approval workflow where applicable.

## 3. Architecture

```
+----------------------------------------------------------+
|                  Angular SPA (TypeScript)                |
|  Router | Guards | RxJS stores | IndexedDB (Dexie)       |
|  SyncWorker (BackgroundSyncService)                      |
+------------------------+---------------------------------+
                         | HTTPS (local cert), JWT
                         v
+----------------------------------------------------------+
|                Spring Boot REST API (Java)               |
|  Controllers | Services | Security filters | Rate limit  |
|  Sync resolver | Export runner | Anomaly scheduler       |
+------+------------------+-------------------+------------+
       |                  |                   |
       v                  v                   v
+-------------+  +------------------+  +----------------+
| PostgreSQL  |  | Local filesystem |  | Standby server |
|  (OLTP)     |  | (exports/backups)|  | (DR failover)  |
+-------------+  +------------------+  +----------------+
```

### Deployment constraints

- **On-premise.** Locally managed TLS certificate. No external dependencies at runtime.
- **LAN sync only.** The Angular client syncs to the same-device or LAN server.
- **Backups.** Local filesystem paths configured by the Administrator. Standby server on the same network.

## 4. Data Model

### 4.1 PostgreSQL tables (abridged)

| Table | Key columns | Indexes |
|---|---|---|
| `organizations` | `id PK`, `code UNIQUE`, `name` | `idx_org_code` |
| `users` | `id PK`, `username UNIQUE`, `password_bcrypt`, `role`, `status`, `organization_id FK`, `failed_login_count`, `locked_until`, `deleted_at` | `idx_users_org`, `idx_users_status`, `idx_users_deleted` |
| `user_device_fingerprints` | `user_id FK`, `fingerprint_hash`, `trusted`, `first_seen_at`, `last_seen_at` | PK `(user_id, fingerprint_hash)` |
| `allowed_ip_ranges` | `id PK`, `cidr`, `role_scope NULLABLE`, `note` | `idx_ipranges_role` |
| `refresh_tokens` | `id PK`, `user_id FK`, `token_hash`, `family_id`, `expires_at`, `revoked_at` | `idx_refresh_user_family` |
| `courses` | `id PK`, `code`, `title`, `version`, `location_id`, `instructor_id`, `classification`, `deleted_at` | `idx_courses_version`, `idx_courses_loc_instr` |
| `cohorts` | `id PK`, `course_id FK`, `total_seats`, `starts_at`, `ends_at` | `idx_cohorts_course_dates` |
| `enrollments` | `id PK`, `student_id FK`, `cohort_id FK`, `organization_id`, `enrolled_at`, `refunded_at NULLABLE`, `deleted_at` | `idx_enroll_org`, `idx_enroll_cohort` |
| `assessment_items` | `id PK`, `course_id FK`, `knowledge_point_id FK`, `type`, `difficulty`, `discrimination`, `deleted_at` | `idx_items_course_kp` |
| `assessment_attempts` | `id PK`, `student_id FK`, `item_id FK`, `chosen_answer`, `is_correct`, `attempted_at` | `idx_attempts_student_item`, `idx_attempts_time` |
| `training_sessions` | `id PK` (client UUID v7), `student_id FK`, `course_id FK`, `cohort_id`, `rest_seconds_default`, `status`, `started_at`, `ended_at`, `client_updated_at`, `deleted_at` | `idx_sessions_student_time` |
| `session_activity_sets` | `id PK`, `session_id FK`, `activity_id`, `set_index`, `rest_seconds`, `completed_at`, `client_updated_at` | `idx_sets_session_activity` |
| `operational_transactions` | `id PK`, `type` (`PURCHASE`,`REFUND`,`INVENTORY_ADJUST`), `amount`, `organization_id`, `occurred_at` | `idx_optx_org_time_type` |
| `certifications` | `id PK`, `student_id FK`, `course_id FK`, `issued_at`, `expires_at` | `idx_certs_expires` |
| `idempotency_keys` | `key PK`, `user_id`, `request_hash`, `response_json`, `created_at` | TTL 72h, `idx_idem_created` |
| `approval_requests` | `id PK`, `type`, `payload JSONB`, `status`, `requested_by`, `reviewed_by`, `expires_at`, `decided_at` | `idx_appr_status_expires` |
| `notification_templates` | `id PK`, `key UNIQUE`, `subject`, `body_md`, `variables JSONB` | `idx_ntpl_key` |
| `in_app_notifications` | `id PK`, `recipient_id FK`, `template_key`, `rendered JSONB`, `severity`, `read_at` | `idx_notif_recipient_unread` |
| `audit_events` | `id PK`, `actor_id`, `action`, `target_type`, `target_id`, `source_ip`, `fingerprint`, `classification`, `created_at`, `metadata JSONB` | `idx_audit_action_time`, `idx_audit_actor_time` |
| `anomaly_events` | `id PK`, `kind`, `user_id NULLABLE`, `source_ip NULLABLE`, `details JSONB`, `resolved_at NULLABLE`, `created_at` | `idx_anom_unresolved` |
| `report_runs` | `id PK`, `kind`, `status`, `row_count`, `output_path`, `requested_by`, `approval_request_id`, `classification`, `created_at`, `completed_at` | `idx_reports_status_time` |
| `report_schedules` | `id PK`, `kind`, `cron`, `local_path`, `owner_id`, `organization_id` | `idx_sched_owner` |
| `backup_runs` | `id PK`, `mode` (`FULL`,`INCREMENTAL`), `started_at`, `finished_at`, `size_bytes`, `path`, `status` | `idx_backups_time` |
| `recovery_drills` | `id PK`, `performed_at`, `result`, `notes` | — |

Every table supporting recycle bin has `deleted_at TIMESTAMPTZ` and `deleted_by UUID`. All `SELECT` paths filter `deleted_at IS NULL` unless under `/admin/recycle-bin`.

### 4.2 IndexedDB stores (client, via Dexie)

| Store | Key | Purpose |
|---|---|---|
| `sessions` | `id` | `TrainingSession` drafts and in-progress |
| `sessionSets` | `id` | `SessionActivitySet` drafts |
| `attemptDrafts` | `id` | Offline assessment answers |
| `outbox` | `id` | Queued mutations `{ method, url, body, idempotencyKey, clientUpdatedAt, attempts }` |
| `profileCache` | `userId` | Last profile + role for offline UI |
| `templatesCache` | `key` | Notification template bodies for offline render |
| `syncState` | `singleton` | `lastSuccessfulSyncAt`, `pendingCount` |

### 4.3 Encryption & masking

- At rest: AES-256 via PostgreSQL `pgcrypto` for columns marked sensitive (contact info, employee IDs).
- In flight: HTTPS only.
- Passwords: BCrypt salt+hash, cost ≥ 12.
- Masking: a server-side view layer emits masked strings based on classification and viewer permission. Default masks: employee ID → `****1234`, email → `j***@example.com`, phone → `***-***-1234`.

## 5. Screens & Routes (Angular)

| Route | Role(s) | Purpose |
|---|---|---|
| `/login`, `/register` | All | Auth entry |
| `/pending` | Pending users | Status screen while awaiting approval |
| `/home` | All | Role-scoped landing |
| `/sessions` | Student | List + resume-after-interruption banner |
| `/sessions/new` | Student | Start a session |
| `/sessions/:id/run` | Student | In-session + rest timer + check-offs |
| `/analytics/mastery` | Mentor, Admin | Mastery trends |
| `/analytics/items` | Mentor, Admin | Item difficulty & discrimination |
| `/analytics/weak-points` | Mentor, Admin | Weak knowledge points |
| `/analytics/wrong-answers` | Mentor, Admin | Wrong-answer distribution |
| `/reports` | Mentor, Admin | Reporting center |
| `/reports/schedules` | Mentor, Admin | Scheduled exports |
| `/notifications` | All | In-app notifications |
| `/admin/users` | Admin | Approve/reject/lock/unlock |
| `/admin/approvals` | Admin | Approval queue |
| `/admin/audit` | Admin | Audit viewer |
| `/admin/anomalies` | Admin | Anomaly triage |
| `/admin/templates` | Admin | Notification template editor |
| `/admin/ip-ranges` | Admin | Allowed CIDRs |
| `/admin/recycle-bin` | Admin | Soft-deleted entities |
| `/admin/backups` | Admin | Backup policy + drills |

All routes are guarded by `AuthGuard` and `RoleGuard`; deep links to out-of-scope data redirect to `/home` and emit an audit event.

## 6. Key Flows

### 6.1 Register → approval

1. Client calls `POST /auth/register`; response is `PENDING`.
2. Administrator sees the user in `/admin/users?status=PENDING`; SLA is **2 business days**.
3. `POST /admin/users/{id}/approve` flips status to `ACTIVE`; in-app notification `approval.decided` is sent.

### 6.2 Login with lockout & anomaly

1. `POST /auth/login` with `deviceFingerprint`.
2. Server increments `failed_login_count` on bad credentials; at 5 within 15 minutes it sets `locked_until = now + 15m`.
3. On success: reset counter, compare fingerprint against `user_device_fingerprints`; if new → raise `anomaly.newDevice` and set `newDeviceAlertRaised`.
4. If `source_ip` not in any applicable `allowed_ip_ranges`: deny with `IP_NOT_ALLOWED` and raise `anomaly.ipOutOfRange`.

### 6.3 Offline training session

1. Student taps **Start**; UI writes `TrainingSession` into IndexedDB `sessions` and enqueues `POST /sessions` in `outbox`.
2. In-session timer runs; between-set rest timer starts at 60s (adjustable 15-300s).
3. Each set check-off writes into `sessionSets` and enqueues a mutation with a generated `Idempotency-Key`.
4. Connectivity lost → UI shows **“Saved locally”** toast.
5. On LAN reconnect, `BackgroundSyncService`:
   - Dequeues in FIFO order.
   - Calls `POST /sessions/sync` with up to 100 items per batch.
   - Applies `SyncResult.applied`; for `conflicts`, client stores server version and shows a reconciliation badge.
6. If the session was interrupted, `/sessions` list shows a **Continue Session** banner mapped to `POST /sessions/{id}/continue`.

### 6.4 Scheduled export

1. Mentor configures `POST /reports/schedules` with cron + local path.
2. Spring Boot scheduler triggers `ReportRunner`.
3. If `rowCount > 10_000` or `classification = 'RESTRICTED'`, the run enters `NEEDS_APPROVAL` and posts an `approval.requested` notification to Administrators.
4. On approval, the runner writes CSV/PDF to the configured local path and emits `export.ready`.
5. Every attempt writes an `EXPORT_ATTEMPT` `audit_events` row; success writes `EXPORT_SUCCESS`.

### 6.5 Anomaly: export burst

- A 1-minute scheduled job queries `audit_events` for the last 10 minutes grouped by `actor_id` and by `source_ip`. Counts `>20` raise `anomaly.exportBurst`.
- Administrators see the anomaly in `/admin/anomalies` and can optionally rate-limit or lock the actor.

### 6.6 Backup & DR

- Nightly incremental `pg_dump -Fc --incremental`-equivalent and weekly full backup to the administrator-defined local path.
- Retention: 30 days default, purge job runs nightly.
- Quarterly recovery drills recorded in `recovery_drills` (Administrator triggers).
- Failover: documented runbook to promote the standby server on the same LAN.

## 7. Security

| Control | Implementation |
|---|---|
| Password policy | ≥12 chars, ≥1 number, ≥1 symbol; enforced client + server |
| Lockout | 5 failures / 15 min → 15-min lock |
| Passwords at rest | BCrypt cost ≥ 12 |
| Sensitive columns | AES-256 via `pgcrypto` |
| Transport | HTTPS only; HSTS header; locally managed cert |
| Headers | CSP, `X-Frame-Options: DENY`, `nosniff`, referrer policy |
| Tokens | JWT access 15m, refresh 8h idle / 12h absolute, rotating, revocable on password change and anomaly resolution |
| IP allow-list | Per-role CIDRs enforced on login and on `/admin/*` |
| Rate limits | Per-user and per-IP sliding windows |
| Approvals | Required for permission elevation and high-risk exports |
| Masking | Default-masked Confidential/Restricted; unmask only with explicit permission |
| Audit | Logins, exports, permission changes, deletions (hard and soft) |
| Recycle bin | 14-day soft-delete; Admin-only restore |

## 8. State Management (Angular)

- RxJS subjects backed by Angular services, one per domain (Auth, Sessions, Analytics, Reports, Notifications, Admin).
- `SessionStore` is the source of truth for live timers; it writes through to IndexedDB every 5 seconds and on every check-off.
- `OutboxService` owns the background sync loop. Retry with exponential backoff: 2, 5, 15, 30, 60 seconds, capped at 5 minutes.
- Guards (`AuthGuard`, `RoleGuard`, `OrgScopeGuard`) read from `AuthStore` and block out-of-scope deep links.
- `NetworkStatusService` polls `GET /health` on the LAN every 15 seconds; transitions drive the "Saved locally" banner.

## 9. Project Structure

```
/
├── metadata.json
├── docs/
│   ├── questions.md
│   ├── api-specs.md
│   └── design.md
├── web/                      # Angular SPA
│   ├── src/app/
│   │   ├── core/             # interceptors, guards, services
│   │   ├── auth/
│   │   ├── sessions/
│   │   ├── analytics/
│   │   ├── reports/
│   │   ├── notifications/
│   │   ├── admin/
│   │   └── shared/
│   ├── src/app/core/sync/    # OutboxService, BackgroundSyncService
│   └── src/app/core/db/      # Dexie schema + stores
└── server/                   # Spring Boot
    ├── src/main/java/.../meridian/
    │   ├── auth/             # JWT, lockout, fingerprint
    │   ├── users/            # approval workflow
    │   ├── courses/
    │   ├── sessions/         # sync resolver
    │   ├── analytics/
    │   ├── reports/          # runner + scheduler
    │   ├── approvals/
    │   ├── notifications/
    │   ├── governance/       # masking, classification, encryption
    │   ├── audit/
    │   ├── anomalies/
    │   ├── recyclebin/
    │   └── backups/
    └── src/main/resources/
        ├── db/migration/     # Flyway
        └── application.yml
```

## 10. Responsive / UX

- Breakpoints: `sm 0-599`, `md 600-959`, `lg 960-1279`, `xl 1280+`. Tablet-first (training floor).
- Timers render with large tap targets (≥ 48 px). Rest timer honors reduced-motion preferences.
- "Saved locally" surfaces as a persistent banner while offline; the pending-sync count shows in the app bar.
- "Continue Session" is a single-tap button on the sessions list; no multi-step resume.
- All interactive analytics filters (date range, location, instructor, course version) live in one left drawer synced with the URL query string so filters are shareable and restorable.

## 11. Testing

| Layer | Approach |
|---|---|
| Angular unit | Jasmine + Karma for services, reducers, guards |
| Angular component | Component harnesses + ng-mocks |
| Angular e2e | Playwright with network throttling for offline flows and resume-after-interruption |
| Spring unit | JUnit 5 + Mockito |
| Spring integration | Testcontainers PostgreSQL; Flyway migrations applied; runs full auth + sync + approval flows |
| Contract | Pact between SPA and API for every DTO in `api-specs.md` |
| Security | Dependency scan, BCrypt cost check, CSP header test, lockout test |
| DR | Scripted quarterly drill executed via `POST /admin/backups/recovery-drill` + recorded result |

## 12. Observability

- Structured JSON logs with correlation id per request (`X-Request-Id`), propagated to audit metadata.
- In-process metrics surface via `/admin/anomalies` counters and a read-only operational dashboard — no external telemetry to respect on-premise constraint.
- Every destructive endpoint writes both an `audit_events` row and an `in_app_notifications` row to Administrators.

## 13. Migration Path

1. **v1 (MVP)**: auth, courses, sessions (online + offline sync), analytics read endpoints, admin approvals, audit, basic reports, backups.
2. **v1.1**: Anomaly alerts, approval workflow, rate limits, allowed IP ranges, notification templates.
3. **v1.2**: Recycle bin (14-day), recovery drills, scheduled exports, export approval threshold.
4. **v1.3**: Standby failover runbook automation, masked-field unmask permissions, per-role allowed IP ranges.

Schema migrations are Flyway-versioned; every schema change is accompanied by an idempotent data migration and, for destructive changes, a recycle-bin safety window.
