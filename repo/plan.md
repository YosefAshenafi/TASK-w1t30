# Meridian Training Analytics Management System — Implementation Plan

Derived from `metadata.json`, `docs/questions.md`, `docs/api-specs.md`, `docs/design.md`.
Project type: on-premise full-stack web app (Angular SPA + Spring Boot REST API + PostgreSQL + IndexedDB offline cache).
Repo root for generated code: `/repo`.

---

## 0. Assumptions & Open Questions

The four input files are internally consistent. A few gaps are worth flagging; the plan picks a concrete default for each and continues.

1. **Refresh token idle vs. absolute TTL.** `questions.md` Q2 says 8h idle / 12h absolute; `api-specs.md §1.2` says only "12h absolute." → Implement both: `refresh_tokens.idle_expires_at` (8h, slides on use) and `refresh_tokens.absolute_expires_at` (12h, hard cap).
2. **Pact contract tests.** `design.md §11` lists Pact. It is treated as optional — included under `api_tests/` as JSON schema + request/response fixtures rather than a full Pact broker, to keep the on-premise constraint intact.
3. **Locations and Instructors.** Referenced by `courses.location_id` and `courses.instructor_id` but no dedicated endpoint exists. → Introduce `locations` and `instructors` reference tables with admin-only CRUD (seeded; not exposed as user-facing pages — used only as filter dropdown sources via `GET /courses?...` facets).
4. **Knowledge Points and Activities.** Referenced by `assessment_items.knowledge_point_id` and `session_activity_sets.activity_id` with no CRUD spec. → Manage via `POST/GET /courses/{id}/knowledge-points` and `POST/GET /courses/{id}/activities`, scoped under the existing Courses module (Faculty Mentor + Admin only).
5. **Enrollments surface.** `report_runs` + corporate-mentor scope require enrollment data but no UI route is listed. → Add no new UI route; enrollments are visible through the Reports pages and the Admin user detail drawer only (matches `design.md §5`).
6. **`deviceFingerprint` on register.** Not required by spec. Only collected at login.
7. **Standby failover automation.** `design.md §13` v1.3 — out of scope for this build. A runbook markdown file under `/repo/server/docs/dr-runbook.md` is sufficient.

---

## Phase ordering at a glance

| # | Phase | Depends on |
|---|---|---|
| 1 | Containerization & dev workflow | — |
| 2 | Spring Boot scaffolding | 1 |
| 3 | Angular scaffolding | 1 |
| 4 | PostgreSQL schema + Flyway | 2 |
| 5 | Auth, lockout, JWT, fingerprint, IP allow-list | 4 |
| 6 | User admin (approvals, unlock, profile) | 5 |
| 7 | Courses / cohorts / knowledge-points / assessment items | 5 |
| 8 | Training sessions + offline sync resolver | 7 |
| 9 | Analytics endpoints | 7, 8 |
| 10 | Reports, schedules, runner | 6, 7, 9 |
| 11 | Approval workflow | 6, 10 |
| 12 | Notifications + templates | 11 |
| 13 | Audit + anomalies + governance/masking | 5, 10 |
| 14 | Recycle bin + backups + DR drills | 6, 13 |
| 15 | Angular core (guards, interceptors, Dexie, outbox) | 3, 5 |
| 16 | Angular auth/register/pending screens | 15 |
| 17 | Angular shared UI system + home | 16 |
| 18 | Angular sessions UI (offline timers + resume) | 15, 17 |
| 19 | Angular analytics UI | 17 |
| 20 | Angular reports UI + schedule editor | 17 |
| 21 | Angular notifications UI | 17 |
| 22 | Angular admin UIs (all `/admin/*` routes) | 17 |
| 23 | Cross-cutting integration polish | all |
| 24 | Consolidated test suites | all |

Each phase ends with an **"Initialize Claude Code here and continue to next phase"** checkpoint. Exit criteria are lightweight smoke checks, not test suites.

---

## Phase 1 — Containerization & Local Dev Workflow

**Objective.** Produce a reproducible local environment: PostgreSQL, Spring Boot, Angular, and a TLS reverse proxy, all runnable with one command. On-premise deployability requires everything to boot without external services.

**Tasks.**
- `/repo/docker-compose.yml` with services: `postgres`, `server`, `web`, `nginx` (terminates HTTPS with a locally generated cert).
- `/repo/server/Dockerfile` — multi-stage Java 21 + Maven Wrapper build → slim JRE runtime.
- `/repo/web/Dockerfile` — Node 20 build stage → nginx static-serve stage.
- `/repo/nginx/nginx.conf` — HTTPS only, HSTS, CSP, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`; proxies `/api/*` to `server`.
- `/repo/scripts/gen-local-cert.sh` — generates self-signed cert into `./nginx/certs/`.
- `/repo/.env.example` with `DB_*`, `JWT_SIGNING_KEY`, `AES_KEY_BASE64`, `BACKUP_PATH`, `EXPORT_PATH`.
- `/repo/README.md` quickstart: `docker compose up --build`.
- `/repo/Makefile` targets: `make up`, `make down`, `make logs`, `make psql`, `make migrate`.

**Exit criteria.**
- `docker compose up` brings all four containers to `healthy`.
- `curl -k https://localhost/api/v1/health` returns `200 {"status":"UP"}` (stub endpoint is fine).
- `https://localhost/` serves the Angular default page.
- `psql` (via `make psql`) connects to the `meridian` database.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 2 — Spring Boot Scaffolding

**Objective.** Create the backend skeleton with package structure matching `design.md §9`, shared DTO infrastructure, error envelope, global exception handler, security filter chain placeholder, rate limiting scaffold, and a `/health` endpoint.

**Tasks.**
- `/repo/server/pom.xml` — Spring Boot 3.3, Java 21, starters: web, validation, security, data-jpa, actuator; Flyway; `jjwt`; Bucket4j; Testcontainers; MapStruct; Lombok; BCrypt via Spring Security.
- Package tree under `com.meridian`: `auth`, `users`, `courses`, `sessions`, `analytics`, `reports`, `approvals`, `notifications`, `governance`, `audit`, `anomalies`, `recyclebin`, `backups`, `common`.
- `common/web/ErrorEnvelope.java`, `GlobalExceptionHandler.java`, `PageResponse.java`.
- `common/security/SecurityConfig.java` — baseline filter chain (permit `/api/v1/auth/**` + `/health`, deny everything else).
- `common/ratelimit/RateLimitFilter.java` — per-user/IP Bucket4j, config-driven; default 120/min, overrides declared via `@RateLimit` annotation.
- `common/idempotency/IdempotencyInterceptor.java` — stub that reads `Idempotency-Key` and attaches to request attributes.
- `application.yml` profiles: `local`, `docker`, `prod`; `JWT_SIGNING_KEY` read from env.
- `HealthController` returns `{"status":"UP","version":"..."}`.
- Logback JSON encoder with `X-Request-Id` MDC.

**Exit criteria.**
- `./mvnw spring-boot:run` inside container logs `Started MeridianApplication`.
- `GET /api/v1/health` returns `200` with `X-Request-Id` echoed back.
- `GET /api/v1/foo` returns `404` in the `ErrorEnvelope` shape.
- Flyway reports no migrations found but does not error.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 3 — Angular Scaffolding

**Objective.** Create the Angular 17+ SPA skeleton with routing, standalone components, Tailwind + shadcn-style primitives via `ng-primitives`/`spartan/ui`, RxJS service base classes, Dexie setup, and the route table from `design.md §5`.

**Tasks.**
- `/repo/web/package.json` — Angular 17 standalone APIs, `@angular/router`, `dexie`, `rxjs`, `tailwindcss`, `@ng-icons/lucide`, `date-fns`, `zod` (for DTO validation), `@spartan-ng/ui` primitives (shadcn-for-Angular); Playwright + Jasmine + Karma dev deps.
- `src/styles/tokens.css` — design tokens from `design.md §10` (breakpoints, spacing, large-touch-target sizing, reduced-motion variables).
- `src/app/app.routes.ts` — every route from `design.md §5` registered with stub components + `AuthGuard` + `RoleGuard` placeholders.
- Folder structure under `src/app/`: `core/`, `auth/`, `sessions/`, `analytics/`, `reports/`, `notifications/`, `admin/`, `shared/`.
- `core/db/dexie.ts` — Dexie schema for `sessions`, `sessionSets`, `attemptDrafts`, `outbox`, `profileCache`, `templatesCache`, `syncState`.
- `core/http/api.service.ts` — base HTTP client with `Authorization`, `Idempotency-Key` (UUID v7), and `X-Request-Id` headers.
- `core/stores/auth.store.ts`, `core/stores/network-status.service.ts` (stubs — polls `/health` every 15s).
- ESLint + Prettier + editorconfig; import path aliases `@core`, `@shared`, etc.

**Exit criteria.**
- `ng serve` (inside container) compiles without error.
- `/` redirects to `/login`; `/login` renders a stub component.
- Visiting `/analytics/mastery` as an unauthenticated user redirects to `/login`.
- IndexedDB database `meridian` is created on first app load (verified via DevTools).

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 4 — Database Schema & Flyway Migrations

**Objective.** Create all PostgreSQL tables listed in `design.md §4.1`, including indexes, plus `locations`, `instructors`, `knowledge_points`, `activities` reference tables (see Assumption 3 & 4). Seed minimal data for local dev.

**Tasks.**
- `server/src/main/resources/db/migration/V1__extensions.sql` — enable `pgcrypto`, `uuid-ossp`.
- `V2__organizations_users_roles.sql` — `organizations`, `users`, `user_device_fingerprints`, `allowed_ip_ranges`, `refresh_tokens`.
- `V3__courses_cohorts_items.sql` — `locations`, `instructors`, `courses`, `cohorts`, `knowledge_points`, `activities`, `assessment_items`, `assessment_attempts`.
- `V4__enrollments_and_ops.sql` — `enrollments`, `operational_transactions`, `certifications`.
- `V5__training_sessions.sql` — `training_sessions`, `session_activity_sets`.
- `V6__idempotency_and_approvals.sql` — `idempotency_keys`, `approval_requests`.
- `V7__notifications.sql` — `notification_templates`, `in_app_notifications`.
- `V8__audit_anomalies.sql` — `audit_events`, `anomaly_events`.
- `V9__reports.sql` — `report_runs`, `report_schedules`.
- `V10__backups.sql` — `backup_runs`, `recovery_drills`.
- Every recycle-bin-eligible table has `deleted_at TIMESTAMPTZ` and `deleted_by UUID`.
- `V100__seed_reference.sql` — seed `notification_templates` with the 10 keys from `questions.md` Q10; seed one Admin user (dev only, gated by `spring.profiles.active=local`) with BCrypt-hashed password; seed one `organization` and one `location` / `instructor`.
- `data.sql` (local profile only) — a handful of courses, cohorts, activities, and knowledge points for UI development.

**Exit criteria.**
- `./mvnw flyway:info` lists V1..V10 + V100, all `Pending` then `Success` after `flyway:migrate`.
- `\dt` in psql shows all 23 tables from `design.md §4.1` plus 4 reference tables.
- `SELECT count(*) FROM notification_templates` returns 10.
- Admin seed user present only in the `local` profile.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 5 — Auth: Register, Login, JWT, Lockout, Fingerprint, IP Allow-List

**Objective.** Implement `/auth/*` endpoints per `api-specs.md §1`, enforce the password policy, lockout rules, device-fingerprint capture, and IP allow-list; issue rotating refresh tokens.

**Tasks.**
- `auth/dto/` — `RegisterRequest`, `RegisterResponse`, `LoginRequest`, `LoginResponse`, `RefreshRequest`, `UserProfileDto`.
- `auth/PasswordPolicy.java` — ≥12 chars, ≥1 digit, ≥1 symbol; reusable for both client and server contracts (document JSON schema in `docs-generated/`).
- `auth/AuthService.java` — register (status `PENDING`), login (BCrypt verify, failed-count tracking, `locked_until`), refresh (rotate, revoke family on reuse), logout (revoke).
- `auth/JwtService.java` — issue + parse 15-minute access tokens signed with `JWT_SIGNING_KEY`.
- `auth/DeviceFingerprintService.java` — lookup/insert into `user_device_fingerprints`; flag new device → insert `anomaly_events` row + enqueue `in_app_notifications` (to user and all admins) with template `anomaly.newDevice`.
- `auth/IpAllowListFilter.java` — deny login when `sourceIp` not in any applicable CIDR and role-scope applies; raise `anomaly.ipOutOfRange`.
- `auth/JwtAuthenticationFilter.java` — populate `SecurityContext`; wire into `SecurityConfig`.
- Rate-limit overrides: `POST /auth/login` 10/min per IP; `POST /auth/register` 5/min per IP.
- `auth/LockoutPolicy.java` — 5 failures / 15 min ⇒ 15-min lock; decremented/reset on success or elapsed window.
- Refresh-token table populated with rotation family + reuse-detection. Absolute 12h cap; idle 8h slide.

**Exit criteria.**
- `curl POST /api/v1/auth/register` returns `201 { userId, status: PENDING }`.
- Register with weak password returns `400 VALIDATION_FAILED` with per-field detail.
- After seed-flipping the user to `ACTIVE`, `POST /auth/login` returns `200` with `accessToken`, `refreshToken`, `expiresIn: 900`.
- 6 bad logins in a row return `403 ACCOUNT_LOCKED` on the 6th.
- `POST /auth/refresh` with a previously used refresh token returns `401` and revokes the entire family.
- New device → `anomaly_events` row written; `newDeviceAlertRaised: true` in response.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 6 — User Admin: Approval Queue, Unlock, Profile

**Objective.** Implement `/admin/users/*` and `GET /users/me` per `api-specs.md §1.4–§1.5`; route tied to Administrator role.

**Tasks.**
- `users/AdminUserController.java` — `GET /admin/users?status=`, `POST /admin/users/{id}/approve`, `POST /admin/users/{id}/reject`, `POST /admin/users/{id}/unlock`.
- `users/UserController.java` — `GET /users/me` returns `UserProfile` (masked per governance defaults for non-admin fields).
- Business rules: `approve` flips `PENDING→ACTIVE` + writes `audit_events` + enqueues `approval.decided` notification; `reject` requires a `reason`; `unlock` clears `failed_login_count` + `locked_until`.
- `@PreAuthorize("hasRole('ADMIN')")` on all `/admin/*`.

**Exit criteria.**
- Admin JWT can list pending users; non-admin gets `403`.
- Approve flips status and produces an `audit_events` row with `action='PERMISSION_CHANGE'`.
- `GET /users/me` returns the authenticated user's profile.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 7 — Courses, Cohorts, Knowledge Points, Activities, Assessment Items

**Objective.** Implement the `/courses/*` and `/assessment-items` endpoints per `api-specs.md §2`, plus the knowledge-points and activities sub-resources needed for sessions and analytics.

**Tasks.**
- `courses/CourseController.java` — `GET/POST/PUT /courses`, `GET /courses/{id}/cohorts`, `GET /courses/{id}/assessment-items`.
- `courses/KnowledgePointController.java` — `GET/POST /courses/{id}/knowledge-points`.
- `courses/ActivityController.java` — `GET/POST /courses/{id}/activities`.
- `courses/AssessmentItemController.java` — `POST /assessment-items`, `PUT /assessment-items/{id}`.
- Classification enforcement via `governance/ClassificationPolicy.java` stub (full masking comes in Phase 13 — for now return unmasked for Faculty/Admin, hide from Student).
- Pagination `?page=0&size=50`, `max size 200`, matches `api-specs.md §0`.
- Role permissions: Faculty Mentor + Admin can `POST/PUT`; all authenticated users can `GET` (scope-filtered).

**Exit criteria.**
- As seeded Admin, `GET /api/v1/courses` returns the seed courses in a `Page<Course>`.
- `POST /courses` with an invalid `version` returns `400 VALIDATION_FAILED`.
- `GET /courses/{id}/assessment-items?page=0&size=5` respects pagination.
- Student-role JWT cannot `POST /assessment-items`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 8 — Training Sessions with Offline Sync Resolver

**Objective.** Implement `/sessions/*` including the bulk `POST /sessions/sync` with idempotency + `clientUpdatedAt` last-write-wins per `api-specs.md §3` and `questions.md` Q5–Q6.

**Tasks.**
- `sessions/SessionController.java` — `POST /sessions`, `PATCH /sessions/{id}`, `POST /sessions/{id}/continue`, `POST /sessions/{id}/complete`, `POST /sessions/{id}/sets`, `PATCH /sessions/{id}/sets/{setId}`, `GET /sessions`.
- `sessions/SessionSyncController.java` — `POST /sessions/sync` accepts batches up to 100.
- `common/idempotency/IdempotencyService.java` — write-through cache on `idempotency_keys`; behavior per `api-specs.md §3` conflict rules (NOOP, `409 IDEMPOTENCY_MISMATCH`, last-write-wins).
- `sessions/SyncResolver.java` — for each item, compare `clientUpdatedAt`; older ⇒ `conflicts[]` entry `OLDER_CLIENT_TIMESTAMP`.
- Rate limit: `POST /sessions/sync` 60/min per user.
- Validation: `restSecondsDefault` 15–300; `setIndex` monotonic per activity; `status` enum.

**Exit criteria.**
- Submitting the same `POST /sessions` payload twice with the same `Idempotency-Key` returns the cached `201` body on the second call.
- Same key with a different body returns `409 IDEMPOTENCY_MISMATCH`.
- `POST /sessions/sync` with 3 items (1 new, 1 older-than-server, 1 unchanged) returns `applied[2]` + `conflicts[1]`.
- `POST /sessions/{id}/continue` on a `PAUSED` session flips it to `IN_PROGRESS`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 9 — Analytics Endpoints

**Objective.** Implement `/analytics/*` per `api-specs.md §4` with role-scoped filtering.

**Tasks.**
- `analytics/AnalyticsController.java` — `GET /analytics/mastery-trends`, `wrong-answers`, `weak-knowledge-points`, `item-stats`.
- `AnalyticsFilter` DTO; `@Validated` on query params (`from`, `to`, `locationId`, `instructorId`, `courseId`, `courseVersion`, `cohortId`, `learnerId`).
- Scope enforcement in a `ScopeInterceptor`:
  - `STUDENT` → forced `learnerId = self`.
  - `CORPORATE_MENTOR` → forced `organizationId = self.orgId` (join through `enrollments`).
  - `FACULTY_MENTOR` → forced to courses/cohorts they are assigned to.
  - `ADMIN` → no forced filter.
- SQL/JPA implementations with views or materialized queries for `mastery_trends` (daily aggregation) and `item_stats` (difficulty/discrimination from `assessment_attempts`).

**Exit criteria.**
- As Admin, `GET /analytics/mastery-trends?courseId=...&from=...&to=...` returns a `MasteryTrendSeries` with non-empty points against seed data.
- As Student, any request with a different `learnerId` is transparently rewritten to the student's own id.
- As Corporate Mentor, weak-knowledge-points response only includes learners from their `organization_id`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 10 — Reports, Schedules, Runner

**Objective.** Implement the Operations & Reporting Center per `api-specs.md §5` with on-disk CSV/PDF output, scheduler, and the approval threshold rule.

**Tasks.**
- `reports/ReportController.java` — `POST /reports`, `GET /reports`, `GET /reports/{id}`, `POST /reports/{id}/cancel`, `GET/POST/DELETE /reports/schedules`.
- `reports/ReportKind.java` + five kind-specific query implementations:
  - `ENROLLMENTS` — rows per enrollment filtered by `organization_id` for mentors.
  - `SEAT_UTILIZATION` — per-cohort per-day ratio with numerator/denominator columns.
  - `REFUND_RETURN_RATE` — windowed ratio with numerator/denominator.
  - `INVENTORY_LEVELS` — aggregated `operational_transactions.type='INVENTORY_ADJUST'`.
  - `CERT_EXPIRING` — `certifications` where `expires_at <= now + {30|60|90} days`.
- `reports/runner/ReportRunner.java` — writes to `${EXPORT_PATH}/${runId}.{csv|pdf}` using OpenCSV + OpenPDF (or Apache PDFBox).
- `reports/runner/ReportScheduler.java` — Spring `@Scheduled`; materializes `report_schedules` via cron4j-style cron strings.
- Approval rule: if estimated `rowCount > 10000` OR `classification = 'RESTRICTED'`, create an `approval_requests` row (`type='EXPORT'`) and leave the run `NEEDS_APPROVAL`; don't execute until approved.
- Every attempt writes `audit_events` `EXPORT_ATTEMPT`; success writes `EXPORT_SUCCESS`.
- Rate limit: `POST /reports`, `POST /reports/schedules` 30/min per user.

**Exit criteria.**
- `POST /reports {kind: ENROLLMENTS, format: CSV}` returns `202` with a `ReportRun{status: QUEUED|SUCCEEDED}`.
- After a few seconds, the CSV appears at `${EXPORT_PATH}/<runId>.csv`.
- A report that synthetically exceeds 10k rows enters `NEEDS_APPROVAL`; until approved, no file is written.
- `POST /reports/schedules` creates a row and the scheduler fires on the configured cron (verified with a 1-minute cron in local dev).

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 11 — Approval Workflow

**Objective.** Implement `/approvals/*` per `api-specs.md §6` and tie it into Phases 6 (permission change) and 10 (export).

**Tasks.**
- `approvals/ApprovalController.java` — `GET /approvals?status=`, `POST /approvals/{id}/approve`, `POST /approvals/{id}/reject`.
- `approvals/ApprovalService.java` — expire after 7 days (`@Scheduled` nightly sweep); emit `approval.requested` to all Admins on creation; `approval.decided` to requester on decision.
- Integration hooks: role elevation in `/admin/users/{id}/approve` (when changing to a higher-privileged role, require approval first); export runner blocks on `approval_request_id` being `APPROVED`.

**Exit criteria.**
- Approving a pending `EXPORT` request causes the paused report run to execute within a minute.
- Rejecting a request marks the report run `FAILED` and writes an `audit_events` row.
- A 7-day-old `PENDING` approval flips to `EXPIRED` when the sweep runs.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 12 — Notifications & Template Editor

**Objective.** Implement `/notifications/*` and `/admin/notification-templates/*` per `api-specs.md §7`, render templates (subject + markdown body) at notification time.

**Tasks.**
- `notifications/NotificationController.java` — `GET /notifications?unread=true`, `POST /notifications/{id}/read`.
- `notifications/TemplateController.java` — `GET /admin/notification-templates`, `PUT /admin/notification-templates/{key}`.
- `notifications/TemplateRenderer.java` — simple `${var}` substitution; markdown → HTML via `flexmark`; validates that every `${var}` is declared in the template's `variables` array.
- `NotificationService.dispatch(key, recipientId, vars)` — used by auth anomaly, approval, export pipelines.
- Seeded templates pre-filled with human-readable copy (see `questions.md` Q10 keys).

**Exit criteria.**
- Editing a template persists and immediately affects subsequently dispatched notifications.
- Triggering an admin-targeted event (e.g., a new pending user) produces an `in_app_notifications` row for every Admin.
- `GET /notifications?unread=true` returns the rendered subject/body.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 13 — Audit, Anomalies, Governance (Masking + Classification)

**Objective.** Complete end-to-end audit logging, the three anomaly detectors, the IP allow-list admin endpoints, and the masking layer per `design.md §7`.

**Tasks.**
- `audit/AuditService.java` + `AuditController.java` — central sink; covers `LOGIN`, `LOGOUT`, `EXPORT_ATTEMPT`, `EXPORT_SUCCESS`, `PERMISSION_CHANGE`, `DATA_DELETE`, `DATA_RESTORE`; `GET /admin/audit?action=&from=&to=&actorId=`.
- `anomalies/AnomalyDetector.java` — `@Scheduled(fixedRate = 60_000)`:
  - `anomaly.newDevice` (fires synchronously on login — already wired in Phase 5).
  - `anomaly.ipOutOfRange` (login-time — Phase 5).
  - `anomaly.exportBurst` — query last 10 min of `audit_events WHERE action='EXPORT_ATTEMPT'`, group by user and source IP; if any bucket `>20`, insert `anomaly_events`.
- `anomalies/AnomalyController.java` — `GET /admin/anomalies?resolved=false`, `POST /admin/anomalies/{id}/resolve`.
- `governance/MaskingInterceptor.java` — Jackson `BeanPropertyWriter` post-processor driven by `@Classified(Classification)` + viewer role; default masks match `design.md §4.3` (`****1234`, `j***@example.com`, `***-***-1234`).
- `governance/Pgcrypto*.java` — AES-256 helper using `PGP_SYM_ENCRYPT` for columns tagged `@Encrypted`; key from env.
- `governance/AllowedIpRangeController.java` — `GET/POST/DELETE /admin/allowed-ip-ranges`.

**Exit criteria.**
- 21 `EXPORT_ATTEMPT` events within 10 minutes ⇒ exactly one `anomaly_events` row with `kind='exportBurst'`.
- `GET /admin/audit` filters by `action` and `actorId`.
- `GET /courses/{id}` as a Student returns masked instructor contact fields; as Admin returns unmasked.
- Sensitive columns round-trip through `pgcrypto` (verified by reading directly in psql and confirming non-plaintext on disk).

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 14 — Recycle Bin, Backups, Recovery Drills

**Objective.** Implement `api-specs.md §9` — soft-delete flows, admin restore, nightly hard-delete sweep, nightly/weekly `pg_dump` backups, recovery drills.

**Tasks.**
- `recyclebin/RecycleBinController.java` — `GET /admin/recycle-bin`, `POST /admin/recycle-bin/{type}/{id}/restore`, `DELETE /admin/recycle-bin/{type}/{id}`.
- Entity `@Where(clause="deleted_at IS NULL")` on all soft-delete entities to keep normal queries clean; admin queries override with a repository method.
- Nightly sweep: `recycle-bin` rows older than 14 days → hard delete; audit row per deletion.
- `backups/BackupController.java` — `GET /admin/backups`, `POST /admin/backups/run?mode=FULL|INCREMENTAL`, `GET/PUT /admin/backups/policy`, `POST /admin/backups/recovery-drill`, `GET /admin/backups/recovery-drills`.
- `backups/BackupRunner.java` — shells out to `pg_dump -Fc` (full) and `pg_basebackup`/WAL incremental; writes to `${BACKUP_PATH}`; retention 30 days by default.
- `backups/RecoveryDrillRunner.java` — restores the latest full backup into a throwaway database and records row-count deltas into `recovery_drills`.
- `/repo/server/docs/dr-runbook.md` — documented standby failover procedure.

**Exit criteria.**
- `DELETE /courses/{id}` sets `deleted_at` and the course no longer appears in `GET /courses`.
- `POST /admin/recycle-bin/courses/{id}/restore` brings it back.
- `POST /admin/backups/run?mode=FULL` produces a file at `${BACKUP_PATH}/<timestamp>.dump` and a `backup_runs` row with `status='SUCCEEDED'`.
- A recovery drill inserts a row in `recovery_drills` with non-null `result`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 15 — Angular Core: Guards, Interceptors, Dexie, Outbox, Network Status

**Objective.** Build the client-side infrastructure that every feature module depends on: HTTP interceptors, guards, IndexedDB schema (already stubbed in Phase 3, finalized here), outbox/sync worker, network-status polling.

**Tasks.**
- `core/http/auth.interceptor.ts` — attach `Authorization` from `AuthStore`, auto-refresh on `401 once`.
- `core/http/idempotency.interceptor.ts` — generate UUID v7 for every non-idempotent method.
- `core/http/offline.interceptor.ts` — when `NetworkStatusService.online$ = false`, enqueue into Dexie `outbox`, return an optimistic stub.
- `core/http/error.interceptor.ts` — map `ErrorEnvelope` to typed errors.
- `core/guards/auth.guard.ts`, `role.guard.ts`, `org-scope.guard.ts` (Corporate Mentor routes).
- `core/db/dexie.ts` — finalize stores + versioning (migrations).
- `core/sync/outbox.service.ts` — FIFO dequeue, batches of 100 to `POST /sessions/sync`, retry with backoff `[2, 5, 15, 30, 60, 300]` seconds.
- `core/sync/background-sync.service.ts` — subscribes to `online$` transitions and drives the outbox.
- `core/stores/auth.store.ts` — login, logout, refresh, pending-approval state; persists `profileCache`.
- `core/stores/network-status.service.ts` — polls `GET /api/v1/health` every 15s, maintains `online$` BehaviorSubject.
- `shared/ui/` — app shell (top bar with pending-sync badge, left drawer, Saved-Locally banner, toast host).

**Exit criteria.**
- Disabling the backend → the app stays responsive, a persistent "Saved locally" banner appears, and the outbox count increments when buttons are pressed.
- Re-enabling the backend drains the outbox and the banner goes away within 15 seconds.
- A 401 response triggers exactly one refresh attempt then retries the original request.
- Deep link to `/admin/users` as a Student role redirects to `/home` and emits an `audit_events` entry (server-side stub endpoint accepts the client report).

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 16 — Angular Auth Screens (`/login`, `/register`, `/pending`)

**Objective.** Implement the three auth-entry pages per `design.md §5` with polished UI and responsive layouts.

**Tasks.**
- `auth/pages/login.component.ts` — username/password, device fingerprint computation (`SHA-256` of `userAgent + acceptLanguage + screenResolution + timezone + platform`), lockout/error display, 429 Retry-After handling.
- `auth/pages/register.component.ts` — role selector, organization code input when `CORPORATE_MENTOR`, password strength meter matching the policy.
- `auth/pages/pending.component.ts` — shows "Your account is pending approval (SLA 2 business days)"; polls `GET /users/me` every 60s.
- Forms built with `ReactiveForms` + zod-validated submit.
- Accessibility: labels, keyboard navigation, ARIA error messages, reduced-motion respected.

**Exit criteria.**
- Registering a new user shows success and routes to `/pending`.
- Weak password prevents submit with inline messages.
- Successful login on a `PENDING` user routes to `/pending`; on `ACTIVE`, routes to `/home`.
- 5 bad logins in a row surface the `ACCOUNT_LOCKED` error with the unlock time.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 17 — Shared UI System & Role-Scoped Home

**Objective.** Stand up the shared UI primitives and the `/home` landing that tiles widgets by role per `design.md §2`.

**Tasks.**
- `shared/ui/button, input, select, dialog, drawer, tabs, toast, banner, skeleton, table, empty-state, masked-field, date-range-picker, cron-picker` — shadcn-style, Tailwind tokens.
- `shared/layout/app-shell.component.ts` — top bar (unread notifications badge, pending-sync count, profile menu), left drawer with role-filtered nav.
- `home/home.component.ts` — role-specific widget tiles:
  - Student: Continue Session, Recent Sessions, My Mastery.
  - Corporate Mentor: Org-scope KPIs, Pending Reports.
  - Faculty Mentor: My Courses, Cohort Mastery, Pending Assessments.
  - Admin: Pending Approvals, Open Anomalies, Backup Status, Recent Audit Events.

**Exit criteria.**
- `/home` renders distinctly for each of the four roles (visually verified in the browser).
- All breakpoints (`sm/md/lg/xl`) render without overlap or clipping.
- Dark mode + reduced-motion toggle render correctly.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 18 — Sessions UI (`/sessions`, `/sessions/new`, `/sessions/:id/run`)

**Objective.** Implement the Student-facing offline-first training flow with timers, check-offs, and resume-after-interruption per `design.md §6.3`.

**Tasks.**
- `sessions/pages/sessions-list.component.ts` — list via `GET /sessions?studentId=`; surfaces a prominent **Continue Session** banner when the top row is `IN_PROGRESS` or `PAUSED`; tap calls `POST /sessions/{id}/continue`.
- `sessions/pages/session-new.component.ts` — course + cohort picker, `restSecondsDefault` slider (15–300, default 60), **Start** writes to Dexie `sessions` and enqueues the POST.
- `sessions/pages/session-run.component.ts`:
  - Large in-session elapsed timer.
  - Between-set rest timer with large **Skip** / **+15s** buttons (≥ 48 px tap targets).
  - Activity list with per-set check-off rows; each check-off writes to Dexie and enqueues PATCH.
  - **Saved locally** banner visible whenever `online$ = false`.
  - Pause/Complete/Abandon actions.
- `SessionStore` writes through to IndexedDB every 5 seconds and on every check-off.

**Exit criteria.**
- Starting a session while the backend is up creates server-side + Dexie rows.
- Dropping the backend mid-session: UI keeps running, check-offs land in Dexie, "Saved locally" persists.
- Restoring the backend drains the outbox; the session's server-side state matches Dexie within a few seconds.
- Closing and reopening the browser while `IN_PROGRESS` shows **Continue Session** on `/sessions`.
- Rest timer respects `prefers-reduced-motion`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 19 — Analytics UI

**Objective.** Implement `/analytics/mastery`, `/analytics/items`, `/analytics/weak-points`, `/analytics/wrong-answers` per `api-specs.md §4` and `design.md §5`.

**Tasks.**
- Shared left-drawer filter panel bound to URL query string (`from`, `to`, `locationId`, `instructorId`, `courseId`, `courseVersion`, `cohortId`, `learnerId` — last one admin-only). URL is the source of truth; refresh restores state.
- `analytics/pages/mastery-trends.component.ts` — line chart (Apache ECharts or Angular-Chartjs) of `masteryPct` over time.
- `analytics/pages/item-stats.component.ts` — scatter plot of `difficulty` vs. `discrimination`, table below.
- `analytics/pages/weak-knowledge-points.component.ts` — sorted list with mastery bars.
- `analytics/pages/wrong-answers.component.ts` — stacked bars by wrong-choice distribution.
- Empty state, error state, skeleton loaders.
- CSV export button uses `POST /reports` with `kind` derived from current view.

**Exit criteria.**
- As Admin, all four pages load, show data from seed, and respond to filter changes within 1s.
- Share-link a URL with filters set; opening it in a fresh tab restores the same view.
- As Corporate Mentor, cohort dropdown lists only org-scoped cohorts.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 20 — Reports UI

**Objective.** Implement `/reports` and `/reports/schedules` per `design.md §5` with run history, schedule editor, and export triggers.

**Tasks.**
- `reports/pages/reports-center.component.ts` — kind selector (`ENROLLMENTS | SEAT_UTILIZATION | REFUND_RETURN_RATE | INVENTORY_LEVELS | CERT_EXPIRING`), window picker, format radio (`CSV | PDF | JSON`), optional `certExpiringDays` when `CERT_EXPIRING`.
- **Run Now** ⇒ `POST /reports`; shows toast + tracks status (`QUEUED | RUNNING | NEEDS_APPROVAL | SUCCEEDED | FAILED`).
- **Run History** table bound to `GET /reports`; row click opens a detail drawer with classification badge, output path (if local), approval link.
- `reports/pages/schedules.component.ts` — list/create/delete scheduled exports; cron input with a friendly translator ("every day at 2am").
- Approval state surfaces a link to `/admin/approvals/{id}` when Admin; otherwise shows "awaiting approval".

**Exit criteria.**
- Run Now on `ENROLLMENTS` yields a `SUCCEEDED` row whose output path is reachable on-disk (verified via `make exec server ls $EXPORT_PATH`).
- Creating a schedule and deleting it reflects in the list immediately.
- A `RESTRICTED` report reaches `NEEDS_APPROVAL` and is unblocked after the Admin approves.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 21 — Notifications UI

**Objective.** Implement `/notifications` per `design.md §5`.

**Tasks.**
- `notifications/pages/inbox.component.ts` — tabs "Unread" / "All"; group by severity; mark-as-read via `POST /notifications/{id}/read`.
- Polling every 30 seconds plus an unread-count badge in the top bar.
- Deep links per template key (`approval.requested` → `/admin/approvals/{id}`, `export.ready` → `/reports`, `anomaly.*` → `/admin/anomalies`).
- Markdown rendered safely (DOMPurify or Angular `[innerHTML]` gated on server-sanitized HTML).

**Exit criteria.**
- New notification on the server appears in the inbox within ~30s.
- Clicking a notification with a `/admin/...` deep-link navigates correctly and updates `readAt`.
- Unread count in the top bar matches `GET /notifications?unread=true`.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 22 — Admin UIs (all `/admin/*` routes)

**Objective.** Implement the remaining Administrator-only pages: users, approvals, audit, anomalies, templates, IP ranges, recycle bin, backups.

**Tasks.**
- `/admin/users` — approval queue (status filter), approve/reject/unlock actions, optional reason modal.
- `/admin/approvals` — unified approval queue (permission changes + exports); approve/reject with reason field.
- `/admin/audit` — filterable audit-event viewer (action, actor, time range); CSV export button.
- `/admin/anomalies` — unresolved anomalies list; resolve action; grouped by kind.
- `/admin/templates` — list of `notification_templates`; markdown editor with live preview; variable chip list.
- `/admin/ip-ranges` — CIDR list with add/remove, optional `role_scope`.
- `/admin/recycle-bin` — tabs per entity type (`users | enrollments | courses | assessment_items | session_logs | reports`); restore + hard-delete actions.
- `/admin/backups` — policy editor (`localPath`, `retentionDays`, cron strings, recycle-bin toggle), run history, drills history, **Run Full Backup / Run Recovery Drill** buttons.

**Exit criteria.**
- Each admin page loads and its primary action succeeds end-to-end (approving a user, resolving an anomaly, editing a template, saving a CIDR, restoring from recycle bin, running a backup).
- Non-admin users land on `/home` when trying to visit any `/admin/*` URL, with a server audit row written.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 23 — Cross-Cutting Integration & Polish

**Objective.** Walk the full product end-to-end and close integration gaps.

**Tasks.**
- End-to-end smoke: register → admin approve → login (new device alert) → start session → go offline → check-off sets → come online → sync drains → view analytics → run a report → approve export → receive `export.ready`.
- Contract audit: grep every endpoint in `api-specs.md`; confirm one controller method + one typed client call exists for each. Produce `/repo/docs-generated/endpoint-coverage.md` listing endpoint → controller → client call.
- Rate-limit verification: scripted loop confirms `429 RATE_LIMITED` at the documented thresholds (login 10/min/IP, register 5/min/IP, reports 30/min/user, sync 60/min/user, default 120/min/user).
- Security headers: `curl -I` the SPA and the API; every header in `api-specs.md §0` present.
- Classification masking: Student sees masked instructor/contact fields; Admin sees unmasked; the difference is visible in the browser.
- Seed demo dataset improvement — enough data for every analytics page to look alive.
- Error states, empty states, loading skeletons audited on every page.

**Exit criteria.**
- The end-to-end script runs clean, all steps green.
- `endpoint-coverage.md` shows 100% coverage of `api-specs.md` endpoints.
- Every response carries the full security-header set.
- Visual QA across `sm/md/lg/xl` — no layout breaks.

**Checkpoint.** *Initialize Claude Code here and continue to next phase.*

---

## Phase 24 — Consolidated Test Suites

**Objective.** Land the full test battery in a single late phase so earlier iteration stays fast. Three top-level folders at the repo root as directed.

**`/repo/unit_tests/`**
- `server/` — JUnit 5 + Mockito for: `AuthService` (password policy, lockout math, refresh rotation, reuse detection), `JwtService` (roundtrip + tamper), `IdempotencyService` (hit/miss/mismatch), `SyncResolver` (LWW + tiebreak by key), `MaskingInterceptor` (every default mask), `AnomalyDetector` (export-burst window arithmetic), `PasswordPolicy` edge cases, `ScopeInterceptor` (org scope SQL rewrite).
- `web/` — Jasmine + Karma for: `AuthStore`, `OutboxService` (FIFO + backoff), `NetworkStatusService`, `SessionStore` (5-second throttle + check-off through-write), all guards, `TemplateRenderer` parity (client-side preview only).

**`/repo/api_tests/`**
- Testcontainers PostgreSQL + Spring Boot test slice. Cover **every** endpoint in `api-specs.md`:
  - §1 Auth: register validation, login success/failure/lockout/new-device/IP-out-of-range, refresh rotation, logout revoke, admin approvals.
  - §2 Courses: CRUD + pagination bounds + role gating.
  - §3 Sessions: CRUD + `POST /sessions/sync` conflict matrix (NOOP, IDEMPOTENCY_MISMATCH, LWW).
  - §4 Analytics: four endpoints × scope matrix (Student/Corporate/Faculty/Admin).
  - §5 Reports: kind matrix, `NEEDS_APPROVAL` threshold, schedule CRUD.
  - §6 Approvals: approve/reject/expire paths.
  - §7 Notifications: fetch + mark-read + template put.
  - §8 Audit/Anomalies/IP ranges: audit filters, export-burst detection, CIDR CRUD.
  - §9 Recycle bin & backups: soft-delete/restore/hard-delete, `POST /admin/backups/run`, drill record.
  - §10 Rate limits: threshold verification with `Retry-After`.
  - JSON-schema assertions against every DTO in `api-specs.md` (acts as the contract layer in lieu of a Pact broker — see Assumption 2).

**`/repo/e2e_tests/`**
- Playwright, headless Chromium against the full Docker compose stack.
- Flows:
  1. Register → pending → admin approves → login → home.
  2. Student starts a session, network throttled to `offline`, completes 3 sets, reconnects, verifies server state matches.
  3. Continue-Session after simulated browser reload.
  4. Admin approves a large export; `export.ready` notification arrives.
  5. New-device anomaly path: login from a second simulated device → notification arrives for admin.
  6. Corporate Mentor scope isolation: logging in as a mentor for Org-A cannot see Org-B learners in analytics filter dropdowns.
  7. Recycle bin restore round-trip.
- Reduced-motion + keyboard-only navigation assertions on all auth pages and the session-run page.

**Exit criteria.**
- `unit_tests/` pass in `server` and `web` suites.
- `api_tests/` pass against a Testcontainers-backed Postgres.
- `e2e_tests/` green on the compose stack.
- `/repo/docs-generated/endpoint-coverage.md` still shows 100% coverage — and the `api_tests/` list maps 1:1.

**Checkpoint.** *Initialize Claude Code here — project complete.*

---

## Appendix A — Requirements → Phase Map (for completeness audit)

| Requirement (source) | Phase |
|---|---|
| Self-register with 2-business-day admin approval SLA | 5, 6, 16 |
| Password ≥12 chars, digit, symbol | 5, 16 |
| 5-failure / 15-min lock | 5, 16 |
| JWT access 15m, rotating refresh 8h idle / 12h absolute | 5, 15 |
| IP allow-list CIDR, per-role and global | 5, 13, 22 |
| Device fingerprint + new-device anomaly | 5, 13, 22 |
| In-session + between-set rest timer (15–300s, default 60s) | 18 |
| Per-activity check-off | 7, 8, 18 |
| Offline IndexedDB session capture | 15, 18 |
| "Saved locally" offline banner | 15, 18 |
| One-tap Continue Session | 8, 18 |
| Idempotency key + LWW sync | 8, 15, 18 |
| Analytics: mastery / wrong answers / weak KPs / item stats | 9, 19 |
| Scope isolation per organization | 9, 19 |
| Filters: date range, location, instructor, course version | 9, 19 |
| Reports: enrollments, seat utilization, refund rate, inventory, cert expiring 30/60/90 | 10, 20 |
| Scheduled CSV/PDF to local path | 10, 20 |
| Editable notification templates | 12, 22 |
| Approval workflow for permission changes and exports | 11, 22 |
| Audit log (logins, exports, permission changes, deletions) | 13, 22 |
| Anomaly alerts: new device, IP out-of-range, export burst (>20/10min) | 5, 13, 22 |
| Token-based access + rate limits | 2, 5, 10, 23 |
| Classification tiers + default masking | 13, 23 |
| AES-256 at rest, BCrypt passwords, HTTPS local cert, security headers | 1, 4, 5, 13, 23 |
| Nightly incremental + weekly full PostgreSQL backup | 14 |
| 30-day retention | 14 |
| Quarterly recovery drills with recorded results | 14 |
| Optional 14-day recycle bin | 14, 22 |
| Documented standby failover | 14 |

Every item in `metadata.json`, every page in `design.md §5`, and every endpoint in `api-specs.md §§1–10` is accounted for above.
