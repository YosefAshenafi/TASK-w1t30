# Endpoint Coverage — Meridian TAMS

Generated: 2026-04-20 (updated after audit remediation). Derived from controller `@RequestMapping` annotations.

**Note:** Coverage is based on controller mappings as of this generation. No external `api-specs.md` file is used as a source.

---

## §1 Authentication & Accounts

| Endpoint | Controller | Client |
|---|---|---|
| `POST /auth/register` | `AuthController.register()` | `register.component.ts → { username, displayName, requestedRole, organizationCode, password }` |
| `POST /auth/login` | `AuthController.login()` | `login.component.ts → http.post('/api/v1/auth/login', body)` |
| `POST /auth/refresh` | `AuthController.refresh()` | `auth.interceptor.ts → http.post('/api/v1/auth/refresh', {refreshToken})` |
| `POST /auth/logout` | `AuthController.logout()` | `app-shell.component.ts → http.post('/api/v1/auth/logout', {})` |
| `GET /admin/users?status=` | `AdminUserController.list()` | `users.component.ts → http.get('/api/v1/admin/users?...')` |
| `POST /admin/users/{id}/approve` | `AdminUserController.approve()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/approve', {})` |
| `POST /admin/users/{id}/reject` | `AdminUserController.reject()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/reject', { reason })` |
| `POST /admin/users/{id}/unlock` | `AdminUserController.unlock()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/unlock', {})` |
| `PATCH /admin/users/{id}/status` | `AdminUserController.requestStatusChange()` | `users.component.ts → http.patch('/api/v1/admin/users/{id}/status', { status })` |
| `GET /users/me` | `UserController.me()` | `pending.component.ts → http.get('/api/v1/users/me')` |

---

## §2 Courses, Cohorts & Assessment Items

| Endpoint | Controller | Client |
|---|---|---|
| `GET /courses` | `CourseController.list()` | `session-new.component.ts → http.get('/api/v1/courses?size=100')` |
| `POST /courses` | `CourseController.create()` | — (admin-only) |
| `PUT /courses/{id}` | `CourseController.update()` | — (admin-only) |
| `DELETE /courses/{id}` | `CourseController.softDelete()` | — (admin-only) |
| `GET /courses/{id}/cohorts` | `CourseController.cohorts()` | `session-new.component.ts → http.get('/api/v1/courses/${courseId}/cohorts')` |
| `GET /courses/{id}/assessment-items` | `CourseController.items()` | — (analytics internal) |
| `POST /assessment-items` | `AssessmentItemController.create()` | — (admin-only) |

---

## §3 Training Sessions

| Endpoint | Controller | Client |
|---|---|---|
| `POST /sessions` | `SessionController.create()` | `session-new.component.ts → http.post('/api/v1/sessions', body)` |
| `PATCH /sessions/{id}` | `SessionController.patch()` | `session-run.component.ts` |
| `POST /sessions/{id}/pause` | `SessionController.pause()` | `session-run.component.ts` |
| `POST /sessions/{id}/continue` | `SessionController.continueSession()` | `session-run.component.ts` |
| `POST /sessions/{id}/complete` | `SessionController.complete()` | `session-run.component.ts` |
| `POST /sessions/{id}/sets` | `SessionController.createSet()` | `session-run.component.ts` |
| `PATCH /sessions/{id}/sets/{setId}` | `SessionController.patchSet()` | `session-run.component.ts` |
| `GET /sessions/{id}` | `SessionController.getById()` | `sessions-list.component.ts` |
| `GET /sessions` | `SessionController.list()` | `sessions-list.component.ts → http.get('/api/v1/sessions?...')` |
| `POST /sessions/sync` | `SessionSyncController.sync()` | `session.store.ts → { sessions: [...], sets: [...] }` |
| `POST /sessions/attempt-drafts` | `AttemptDraftController.upsert()` | `session-run.component.ts → { id, sessionId, itemId, chosenAnswer, clientUpdatedAt }` |
| `GET /sessions/{sessionId}/attempt-drafts` | `AttemptDraftController.list()` | `session-run.component.ts` |
| `DELETE /sessions/{sessionId}/attempt-drafts` | `AttemptDraftController.clearForSession()` | `session-run.component.ts` |
| `POST /sessions/{sessionId}/submit-attempts` | `AttemptDraftController.submitAttempts()` | `session-run.component.ts → Idempotency-Key header required` |

---

## §4 Analytics

| Endpoint | Controller | Client |
|---|---|---|
| `GET /analytics/mastery-trends` | `AnalyticsController.masteryTrends()` | `mastery-trends.component.ts` |
| `GET /analytics/wrong-answers` | `AnalyticsController.wrongAnswers()` | `wrong-answers.component.ts` |
| `GET /analytics/weak-knowledge-points` | `AnalyticsController.weakKnowledgePoints()` | `weak-knowledge-points.component.ts` |
| `GET /analytics/item-stats` | `AnalyticsController.itemStats()` | `item-stats.component.ts` |

---

## §5 Reports

| Endpoint | Controller | Client |
|---|---|---|
| `POST /reports` | `ReportController.create()` | `reports-center.component.ts → { kind, format, classification? }` |
| `GET /reports/{id}` | `ReportController.get()` | — (owner or admin only) |
| `GET /reports/{id}/download` | `ReportController.download()` | `reports-center.component.ts → downloadUrl(run.id)` |
| `GET /reports` | `ReportController.list()` | `reports-center.component.ts → response: { kind, outputPath, createdAt }` |
| `POST /reports/{id}/cancel` | `ReportController.cancel()` | — (owner or admin only) |
| `GET /reports/schedules` | `ReportController.listSchedules()` | `schedules.component.ts` |
| `POST /reports/schedules` | `ReportController.createSchedule()` | `schedules.component.ts` |
| `PUT /reports/schedules/{id}` | `ReportController.updateSchedule()` | `schedules.component.ts → toggleEnabled()` |
| `DELETE /reports/schedules/{id}` | `ReportController.deleteSchedule()` | `schedules.component.ts` |

---

## §6 Approval Workflow

| Endpoint | Controller | Client |
|---|---|---|
| `GET /admin/approvals?status=PENDING` | `ApprovalController.list()` | `approvals.component.ts → http.get('/api/v1/admin/approvals?status=PENDING')` |
| `POST /admin/approvals/{id}/approve` | `ApprovalController.approve()` | `approvals.component.ts → http.post('/api/v1/admin/approvals/{id}/approve', {})` |
| `POST /admin/approvals/{id}/reject` | `ApprovalController.reject()` | `approvals.component.ts → http.post('/api/v1/admin/approvals/{id}/reject', { reason })` |

---

## §7 Notifications

| Endpoint | Controller | Client |
|---|---|---|
| `GET /notifications` | `NotificationController.list()` | `inbox.component.ts → http.get('/api/v1/notifications?size=50')` |
| `GET /notifications?unread=true` | `NotificationController.list()` | `inbox.component.ts → http.get('/api/v1/notifications?unread=true')` |
| `POST /notifications/{id}/read` | `NotificationController.markRead()` | `inbox.component.ts → http.post('/api/v1/notifications/{id}/read', {})` |
| `POST /notifications/read-all` | `NotificationController.markAllRead()` | `inbox.component.ts → http.post('/api/v1/notifications/read-all', {})` |
| `GET /notifications/unread-count` | `NotificationController.unreadCount()` | `home.component.ts → http.get('/api/v1/notifications/unread-count')` |
| `GET /admin/notification-templates` | `TemplateController.list()` | `templates.component.ts` |
| `PUT /admin/notification-templates/{key}` | `TemplateController.update()` | `templates.component.ts` |

---

## §8 Audit, Anomalies, Governance

| Endpoint | Controller | Client |
|---|---|---|
| `GET /admin/audit` | `AuditController.list()` | `audit.component.ts → http.get('/api/v1/admin/audit?...')` |
| `GET /admin/anomalies` | `AnomalyController.list()` | `anomalies.component.ts → http.get('/api/v1/admin/anomalies?resolved=false')` |
| `POST /admin/anomalies/{id}/resolve` | `AnomalyController.resolve()` | `anomalies.component.ts` |
| `GET /admin/allowed-ip-ranges` | `AllowedIpRangeController.list()` | `ip-ranges.component.ts` |
| `POST /admin/allowed-ip-ranges` | `AllowedIpRangeController.create()` | `ip-ranges.component.ts` |
| `DELETE /admin/allowed-ip-ranges/{id}` | `AllowedIpRangeController.delete()` | `ip-ranges.component.ts` |

---

## §9 Recycle Bin & Backups

| Endpoint | Controller | Client |
|---|---|---|
| `GET /admin/recycle-bin` | `RecycleBinController.list()` | `recycle-bin.component.ts` |
| `POST /admin/recycle-bin/{type}/{id}/restore` | `RecycleBinController.restore()` | `recycle-bin.component.ts` |
| `DELETE /admin/recycle-bin/{type}/{id}` | `RecycleBinController.hardDelete()` | `recycle-bin.component.ts` |
| `GET /admin/backups` | `BackupController.list()` | `backups.component.ts` |
| `POST /admin/backups/run` | `BackupController.triggerBackup()` | `backups.component.ts` |
| `GET /admin/backups/policy` | `BackupController.getPolicy()` | — (persisted to DB via backup_policy table) |
| `PUT /admin/backups/policy` | `BackupController.updatePolicy()` | — (persisted to DB) |
| `POST /admin/backups/recovery-drill` | `BackupController.scheduleDrill()` | `backups.component.ts` |
| `GET /admin/backups/recovery-drills` | `BackupController.listDrills()` | `backups.component.ts` |

---

## Security Headers

All API responses include:

| Header | Value |
|---|---|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |

---

## Rate Limits

| Scope | Limit |
|---|---|
| Login (`POST /auth/login`) | 10 req/min per IP |
| Register (`POST /auth/register`) | 5 req/min per IP |
| Report endpoints | 30 req/min per user |
| Sync endpoint (`POST /sessions/sync`) | 60 req/min per user |
| Default | 120 req/min per user |

---

## Encryption

Sensitive columns (audit event details) are encrypted at rest using AES-256-GCM via `AesAttributeConverter`. See `README.md §Encryption` for key rotation guidance.

---

## Pagination

All paginated list responses use `PageResponse<T>` with fields: `content`, `page`, `size`, `total` (not `totalElements`).
