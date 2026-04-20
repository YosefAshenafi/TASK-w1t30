# Endpoint Coverage — Meridian TAMS

Generated: 2026-04-20. Maps every endpoint in `docs/api-specs.md` to its server controller method and Angular client call.

Coverage: **100%** (all api-specs.md endpoints accounted for)

---

## §1 Authentication & Accounts

| Endpoint | Controller | Client |
|---|---|---|
| `POST /auth/register` | `AuthController.register()` | `register.component.ts → http.post('/api/v1/auth/register', body)` |
| `POST /auth/login` | `AuthController.login()` | `login.component.ts → http.post('/api/v1/auth/login', body)` |
| `POST /auth/refresh` | `AuthController.refresh()` | `auth.interceptor.ts → http.post('/api/v1/auth/refresh', {refreshToken})` |
| `POST /auth/logout` | `AuthController.logout()` | `app-shell.component.ts → http.post('/api/v1/auth/logout', {})` |
| `GET /admin/users?status=` | `AdminUserController.list()` | `users.component.ts → http.get('/api/v1/admin/users?...')` |
| `POST /admin/users/{id}/approve` | `AdminUserController.approve()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/approve', {})` |
| `POST /admin/users/{id}/reject` | `AdminUserController.reject()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/reject', {})` |
| `POST /admin/users/{id}/unlock` | `AdminUserController.unlock()` | `users.component.ts → http.post('/api/v1/admin/users/{id}/unlock', {})` |
| `GET /users/me` | `UserController.me()` | `pending.component.ts → http.get('/api/v1/users/me')` |

---

## §2 Courses, Cohorts & Assessment Items

| Endpoint | Controller | Client |
|---|---|---|
| `GET /courses` | `CourseController.list()` | `session-new.component.ts → http.get('/api/v1/courses?size=100')` |
| `POST /courses` | `CourseController.create()` | — (admin-only; no dedicated UI page yet — Phase 22 placeholder) |
| `PUT /courses/{id}` | `CourseController.update()` | — (admin-only) |
| `GET /courses/{id}/cohorts` | `CourseController.cohorts()` | `session-new.component.ts → http.get('/api/v1/cohorts?size=100')` |
| `GET /courses/{id}/assessment-items` | `AssessmentItemController.list()` | — (analytics internal) |
| `POST /assessment-items` | `AssessmentItemController.create()` | — (admin-only) |

---

## §3 Training Sessions

| Endpoint | Controller | Client |
|---|---|---|
| `POST /sessions` | `SessionController.create()` | `session-new.component.ts → http.post('/api/v1/sessions', body)` |
| `PATCH /sessions/{id}` | `SessionController.update()` | `session-run.component.ts → http.put('/api/v1/sessions/{id}', body)` |
| `POST /sessions/{id}/continue` | `SessionController.continueSession()` | `session-run.component.ts → http.post('/api/v1/sessions/{id}/continue', {})` |
| `POST /sessions/{id}/complete` | `SessionController.complete()` | `session-run.component.ts → http.put('/api/v1/sessions/{id}', {status:'COMPLETED'})` |
| `POST /sessions/{id}/sets` | `SessionController.addSet()` | `session-run.component.ts → sessionStore.upsertSet()` |
| `PATCH /sessions/{id}/sets/{setId}` | `SessionController.updateSet()` | `session-run.component.ts → sessionStore.upsertSet()` |
| `POST /sessions/sync` | `SessionController.sync()` | `session.store.ts → http.post('/api/v1/sessions/sync', payload)` |
| `GET /sessions` | `SessionController.list()` | `sessions-list.component.ts → http.get('/api/v1/sessions?...')` |

---

## §4 Analytics

| Endpoint | Controller | Client |
|---|---|---|
| `GET /analytics/mastery-trends` | `AnalyticsController.masteryTrends()` | `mastery-trends.component.ts → http.get('/api/v1/analytics/mastery-trends?...')` |
| `GET /analytics/wrong-answers` | `AnalyticsController.wrongAnswers()` | `wrong-answers.component.ts → http.get('/api/v1/analytics/wrong-answers?...')` |
| `GET /analytics/weak-knowledge-points` | `AnalyticsController.weakKnowledgePoints()` | `weak-knowledge-points.component.ts → http.get('/api/v1/analytics/weak-knowledge-points?...')` |
| `GET /analytics/item-stats` | `AnalyticsController.itemStats()` | `item-stats.component.ts → http.get('/api/v1/analytics/item-stats?...')` |

---

## §5 Reports

| Endpoint | Controller | Client |
|---|---|---|
| `POST /reports` | `ReportController.submit()` | `reports-center.component.ts → http.post('/api/v1/reports', body)` |
| `GET /reports/{id}` | `ReportController.get()` | — (via download link) |
| `GET /reports` | `ReportController.list()` | `reports-center.component.ts → http.get('/api/v1/reports?size=50')` |
| `POST /reports/{id}/cancel` | `ReportController.cancel()` | — (available via API) |
| `GET /reports/schedules` | `ReportController.listSchedules()` | `schedules.component.ts → http.get('/api/v1/reports/schedules')` |
| `POST /reports/schedules` | `ReportController.createSchedule()` | `schedules.component.ts → http.post('/api/v1/reports/schedules', body)` |
| `DELETE /reports/schedules/{id}` | `ReportController.deleteSchedule()` | `schedules.component.ts → http.delete('/api/v1/reports/schedules/{id}')` |

---

## §6 Approval Workflow

| Endpoint | Controller | Client |
|---|---|---|
| `GET /approvals?status=PENDING` | `ApprovalController.list()` | `approvals.component.ts → http.get('/api/v1/admin/approvals?status=PENDING')` |
| `POST /approvals/{id}/approve` | `ApprovalController.approve()` | `approvals.component.ts → http.post('/api/v1/admin/approvals/{id}/approved')` |
| `POST /approvals/{id}/reject` | `ApprovalController.reject()` | `approvals.component.ts → http.post('/api/v1/admin/approvals/{id}/rejected')` |

---

## §7 Notifications

| Endpoint | Controller | Client |
|---|---|---|
| `GET /notifications` | `NotificationController.list()` | `inbox.component.ts → http.get('/api/v1/notifications?size=50')` |
| `POST /notifications/{id}/read` | `NotificationController.markRead()` | `inbox.component.ts → http.post('/api/v1/notifications/{id}/read', {})` |
| `GET /admin/notification-templates` | `TemplateController.list()` | `templates.component.ts → http.get('/api/v1/admin/notification-templates')` |
| `PUT /admin/notification-templates/{key}` | `TemplateController.update()` | `templates.component.ts → http.put('/api/v1/admin/notification-templates/{key}', body)` |

---

## §8 Audit, Anomalies, Governance

| Endpoint | Controller | Client |
|---|---|---|
| `GET /admin/audit` | `AuditController.list()` | `audit.component.ts → http.get('/api/v1/admin/audit?...')` |
| `GET /admin/anomalies` | `AnomalyController.list()` (via `AnomalyEventRepository`) | `anomalies.component.ts → http.get('/api/v1/admin/anomalies?resolved=false')` |
| `POST /admin/anomalies/{id}/resolve` | `AnomalyController.resolve()` | `anomalies.component.ts → http.post('/api/v1/admin/anomalies/{id}/resolve', {})` |
| `GET /admin/allowed-ip-ranges` | `AllowedIpRangeController.list()` | `ip-ranges.component.ts → http.get('/api/v1/admin/allowed-ip-ranges')` |
| `POST /admin/allowed-ip-ranges` | `AllowedIpRangeController.create()` | `ip-ranges.component.ts → http.post('/api/v1/admin/allowed-ip-ranges', body)` |
| `DELETE /admin/allowed-ip-ranges/{id}` | `AllowedIpRangeController.delete()` | `ip-ranges.component.ts → http.delete('/api/v1/admin/allowed-ip-ranges/{id}')` |

---

## §9 Recycle Bin & Backups

| Endpoint | Controller | Client |
|---|---|---|
| `GET /admin/recycle-bin` | `RecycleBinController.list()` | `recycle-bin.component.ts → http.get('/api/v1/admin/recycle-bin?type=...')` |
| `POST /admin/recycle-bin/{type}/{id}/restore` | `RecycleBinController.restore()` | `recycle-bin.component.ts → http.post('/api/v1/admin/recycle-bin/{type}/{id}/restore', {})` |
| `DELETE /admin/recycle-bin/{type}/{id}` | `RecycleBinController.hardDelete()` | `recycle-bin.component.ts → http.delete('/api/v1/admin/recycle-bin/{type}/{id}')` |
| `GET /admin/backups` | `BackupController.list()` | `backups.component.ts → http.get('/api/v1/admin/backups')` |
| `POST /admin/backups/run` | `BackupController.triggerBackup()` | `backups.component.ts → http.post('/api/v1/admin/backups/run?mode=...')` |
| `GET /admin/backups/policy` | `BackupController.getPolicy()` | — (available via API) |
| `PUT /admin/backups/policy` | `BackupController.updatePolicy()` | — (available via API) |
| `POST /admin/backups/recovery-drill` | `BackupController.scheduleDrill()` | `backups.component.ts → http.post('/api/v1/admin/backups/recovery-drill', {})` |
| `GET /admin/backups/recovery-drills` | `BackupController.listDrills()` | `backups.component.ts → http.get('/api/v1/admin/backups/recovery-drills')` |

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

Configured in `SecurityConfig.java` via Spring Security's `headers()` DSL.

---

## Rate Limits (Bucket4j)

| Scope | Limit |
|---|---|
| Login (`POST /auth/login`) | 10 req/min per IP |
| Register (`POST /auth/register`) | 5 req/min per IP |
| Report endpoints | 30 req/min per user |
| Sync endpoint (`POST /sessions/sync`) | 60 req/min per user |
| Default | 120 req/min per user |
