# Delivery Acceptance and Project Architecture Audit (Static-Only)

## 1. Verdict
- Overall conclusion: **Partial Pass**

## 2. Scope and Static Verification Boundary
- Reviewed:
  - Project docs/config: `README.md:5-119`, `.env.example:1-28`, `docker-compose.yml:1-83`, `server/src/main/resources/application.yml:1-112`, `server/pom.xml`
  - Backend security/auth/authorization and core business modules under `server/src/main/java/com/meridian/**` (169 Java files; key files read in full)
  - Database migrations under `server/src/main/resources/db/migration/**` (21 SQL files, full list reviewed)
  - Frontend structure under `web/src/app/**` (39 TypeScript component/service files, structure reviewed)
  - Static tests under `unit_tests/**` (8 Java, 5 TypeScript), `api_tests/**` (6 Java), `e2e_tests/**` (8 Playwright specs + helpers), plus `run_tests.sh:1-312`
  - `server/Dockerfile:1-33`
- Not reviewed:
  - Runtime behavior, browser rendering, performance characteristics, external services
  - Full Angular component templates (HTML/CSS) — structure reviewed, visual output not verifiable statically
  - Nginx configuration files
  - `docs-generated/endpoint-coverage.md` (not read in full)
- Intentionally not executed:
  - Project startup, Docker Compose, Maven build, Angular build, test suites, Playwright (per instruction)
- Manual verification required for:
  - End-to-end runtime correctness of offline sync and background reconciliation (IndexedDB operations)
  - UI rendering fidelity, responsive behavior, and WCAG compliance across target devices
  - Backup/recovery drill success in Docker environment post-fix
  - Rate-limit memory growth at real scale and restart-based bypass validation

## 3. Repository / Requirement Mapping Summary
- Prompt core goal mapped: full-stack Angular + Spring Boot + PostgreSQL on-premise training analytics platform with role-scoped UX/data (four roles), approval-gated account lifecycle, offline-first session capture with IndexedDB sync, assessment analytics/reporting, governance/masking/classification, anomaly detection, and backup/DR.
- Main mapped implementation areas:
  - Auth/lockout/approval/IP/device anomaly: `auth/*`, `users/*`, `approvals/*`, `security/audit/*`, `security/anomaly/*`
  - Session capture/offline/idempotency/LWW: `sessions/*`, `common/idempotency/*`, `web/src/app/sessions/*`, `web/src/app/core/http/*`
  - Analytics at learner/cohort/course level with org scope: `analytics/AnalyticsController.java`, `analytics/AnalyticsService.java`
  - Reporting/exports/schedules/notifications/templates: `reports/*`, `notifications/*`
  - Governance/masking/classification: `governance/MaskingPolicy.java`, `governance/ClassificationPolicy.java`
  - Backup/recycle-bin/DR: `backups/*`, recyclebin references, quarterly drill scheduling

## 4. Section-by-section Review

### 4.1 Hard Gates
#### 4.1.1 Documentation and static verifiability
- Conclusion: **Pass**
- Rationale: `README.md` provides Docker-based quick-start (4 steps), default credentials table, architecture diagram, environment variable reference, encryption/key-rotation instructions, and test commands for all four test layers. `.env.example` enumerates required variables (extended in this review to add three undocumented toggles). `docker-compose.yml` is structurally consistent with the documented architecture. Entry points are derivable from configuration.
- Evidence:
  - `README.md:5-119` — full quick-start, config, and test documentation
  - `docker-compose.yml:1-83` — 4-service architecture consistent with README
  - `application.yml:26-71` — all env-variable bindings present
  - `.env.example` — updated in this review to add `EXPORTS_ADMIN_APPROVAL_REQUIRED`, `RECYCLE_BIN_RETENTION_DAYS`, `REGISTRATION_SLA_BUSINESS_DAYS`
- Manual verification note: Docker-based startup instructions are documented but not executed.

#### 4.1.2 Material deviation from Prompt
- Conclusion: **Partial Pass**
- Rationale: The implementation is centred on the Meridian training analytics scenario with no scope drift. The one material deviation is learner-level item-stats filtering: the Prompt specifies item difficulty/discrimination at learner, cohort, and course levels but the `itemStats` endpoint excludes `learnerId` filtering.
- Evidence:
  - `AnalyticsController.java:103` — `learnerId` explicitly set to `null` for item stats, blocking learner-level item queries
  - `User.java:38` — role field correctly holds STUDENT/CORPORATE_MENTOR/FACULTY_MENTOR/ADMIN values
  - `ReportRunner.java:37` — `ALLOWED_CERT_WINDOWS = Set.of(30, 60, 90)` satisfies certification-expiration requirement

### 4.2 Delivery Completeness
#### 4.2.1 Core explicit requirements coverage
- Conclusion: **Partial Pass**
- Rationale: Major requirements are implemented (four roles, pending approval, password/lockout, offline sync/idempotency/LWW, analytics, reports, approval workflows, anomaly detection, backups/recycle-bin). One confirmed gap: learner-scoped item-stats analytics. One static-only gap: the 15–300 second configurable rest-timer range was not fully confirmed in the session-run UI component.
- Evidence:
  - Password policy: `server/src/main/java/com/meridian/auth/PasswordPolicy.java:7-16` — 12-char min, 1 digit, 1 symbol
  - Lockout policy: `server/src/main/java/com/meridian/auth/LockoutPolicy.java:10-11` — 5 failures → 15-min lockout
  - Pending registration + 2-day SLA: `server/src/main/java/com/meridian/auth/AuthService.java:72-83`, `RegistrationSlaScheduler.java`
  - Offline IndexedDB/outbox/background sync: `web/src/app/core/http/outbox.service.ts`, `background-sync.service.ts`
  - Sync resolver (LWW/idempotency): `server/src/main/java/com/meridian/sessions/SyncResolver.java:37-90`
  - Analytics filters: `server/src/main/java/com/meridian/analytics/AnalyticsController.java:27-154`, `AnalyticsService.java:16-179`
  - Reports/schedules/exports: `server/src/main/java/com/meridian/reports/ReportController.java:62-242`
  - Certification expiration (30/60/90 days): `server/src/main/java/com/meridian/reports/runner/ReportRunner.java:37`
  - Editable notification templates: `notifications/TemplateController.java`, `notifications/TemplateRenderer.java`
  - Backup/recovery/quarterly drills: `backups/BackupScheduler.java:46-100`, `backups/BackupRunner.java`, `backups/RecoveryDrillRunner.java`
  - Recycle bin: `RecycleBinRetentionScheduler` (referenced)
  - Data masking: `governance/MaskingPolicy.java:15-85`
  - Item stats at learner level: **missing** — `AnalyticsController.java:103` passes `learnerId = null`

#### 4.2.2 End-to-end 0→1 deliverable vs partial demo
- Conclusion: **Pass**
- Rationale: Complete multi-container Docker application with clear entry points, Flyway-managed schema, real `pg_dump`/`pg_restore` for backups, no mock/hardcoded logic replacing real business flows, and four test layers with JaCoCo coverage enforcement.
- Evidence:
  - `docker-compose.yml` — 4-service architecture (postgres, server, web, nginx)
  - `server/Dockerfile:19-32` — production `runtime` target (JRE only, no Maven)
  - `run_tests.sh:18` — `COV_THRESHOLD=90` enforced across server unit and integration suites
  - Migrations: `V1`–`V18` + `V100`–`V201` covering full schema including encryption columns

### 4.3 Engineering and Architecture Quality
#### 4.3.1 Structure and module decomposition
- Conclusion: **Pass**
- Rationale: 169 Java source files across 15+ cohesive packages with clear bounded responsibilities. No excessive single-file accumulation. Angular frontend mirrors backend decomposition with dedicated service/component pairs per functional area.
- Evidence:
  - Backend packages: `auth`, `common.security`, `common.ratelimit`, `common.idempotency`, `sessions`, `analytics`, `backups`, `reports`, `notifications`, `approvals`, `courses`, `users`, `organizations`, `security.audit`, `security.anomaly`, `governance`
  - `AuditEventPublisher.java` — standalone class for single-responsibility audit publishing (`REQUIRES_NEW` transaction)
  - `SyncResolver.java` — conflict resolution logic fully separated from the sync controller
  - `web/src/app/` — 39 component/service files across auth, sessions, analytics, admin, reports, notifications areas

#### 4.3.2 Maintainability/extensibility
- Conclusion: **Pass**
- Rationale: Clean Spring Boot patterns throughout: `@PreAuthorize` for declarative security, Spring Data JPA repositories, record DTOs, `@Value`/`@ConfigurationProperties`-externalised configuration, Flyway schema management. `AnalyticsFilter` record makes adding new filter dimensions straightforward. Rate-limit values are now externalised to `application.yml` via `RateLimitProperties` (refactored in this review).
- Evidence:
  - `AnalyticsFilter.java:6-21` — clean record with all filter dimensions
  - `RateLimitProperties.java:1-34` — `@ConfigurationProperties` binding for rate-limit config (externalised in this review)
  - `application.yml:46-70` — rate-limit limits now in config, not hardcoded
  - `application.yml:72-112` — 4 named Spring profiles supporting different environments

### 4.4 Engineering Details and Professionalism
#### 4.4.1 Error handling, logging, validation, API design
- Conclusion: **Pass**
- Rationale: Consistent `ResponseStatusException` usage throughout. Standard HTTP semantics: 201 creation, 204 logout, 409 conflicts, 401/403 auth failures, 429 rate-limiting with `Retry-After` header. `@Valid` on all request bodies. Jakarta Bean Validation on DTOs. Structured `ErrorEnvelope` JSON error responses. Slf4j logging at appropriate levels throughout. Audit events in separate transactions for durability.
- Evidence:
  - `AuthController.java:19-38` — correct HTTP status codes per operation
  - `RateLimitFilter.java:69-77` — 429 with `Retry-After` header
  - `RegisterRequest.java:9-25` — `@NotBlank`, `@Size`, `@Email`, `@Pattern` validation
  - `AuditEventPublisher.java` — `@Transactional(propagation=REQUIRES_NEW)` for audit durability
  - Sub-optimal pattern: `AuthService.java:177`, `ApprovalService.java:75` — JSON payloads built by string concatenation instead of `ObjectMapper` (low risk for IP addresses; corrected audit action name in this review)

#### 4.4.2 Product/service realism vs demo
- Conclusion: **Pass**
- Rationale: Full multi-container Docker deployment, Flyway-managed schema, JaCoCo-enforced 90% coverage threshold, four test layers, slim production Dockerfile, multiple Spring profiles, approval workflows, anomaly detection with notifications, scheduled backup/retention, and quarterly recovery drills. Shape matches a real on-premise enterprise product.
- Evidence:
  - `run_tests.sh:18` — `COV_THRESHOLD=90`
  - `server/Dockerfile:23-32` — JRE-only slim runtime target
  - `application.yml:98-112` — explicit `prod` profile requiring all DB credentials from environment
  - `approvals/ApprovalService.java:36-92` — full approval lifecycle with notifications and audit
  - `security/anomaly/AnomalyDetector.java:28-66` — real-time export-burst and new-device detection

### 4.5 Prompt Understanding and Requirement Fit
#### 4.5.1 Business goal/scenario/constraints fit
- Conclusion: **Partial Pass**
- Rationale: Business domain fit is strong. The main literal deviation is the item-stats endpoint excluding learner-level filtering. One ambiguity: the Prompt lists "at learner, cohort, and course levels" for analytics — it is unclear whether this scope applies specifically to item difficulty/discrimination or only to mastery trends. The current design (item stats as item-level only) is defensible but does not satisfy the Prompt as written.
- Evidence:
  - `AnalyticsController.java:88-106` — `itemStats` excludes `learnerId` explicitly (line 103)
  - `ReportController.java:228-236` — `resolveReportOrgId` forces corporate mentor org scope (correctly implemented)
  - `AnalyticsController.java:108-128` — student scope enforced; corporate mentor org scope enforced for analytics
  - `AnalyticsController.java:116-128` — `FACULTY_MENTOR` receives no org filter (cross-org analytics access); Prompt does not explicitly restrict faculty mentors to one org, but intent should be verified

### 4.6 Aesthetics (frontend)
#### 4.6.1 Visual/interaction quality
- Conclusion: **Cannot Confirm Statistically**
- Rationale: Static review confirms Angular component hierarchy exists for all functional areas, routing guards are tested, and dedicated components exist per role/function. Actual rendered quality, hover states, responsive layout, and WCAG compliance can only be confirmed at runtime.
- Evidence:
  - `web/src/app/` — 39 component/service files across sessions, analytics, admin, reports, notifications
  - `e2e_tests/tests/08-accessibility.spec.ts` — WCAG check present (requires running services)
  - `unit_tests/web/auth.guard.spec.ts` — route guard protection confirmed statically
- Manual verification note: Visual hierarchy, responsive behavior, and interaction feedback require browser execution.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker / High
1. **Severity: High**
- Title: In-memory rate-limit bucket store resets on restart — brute-force window bypass
- Conclusion: **Fail (partially mitigated in this review)**
- Evidence:
  - `RateLimitFilter.java:35-41` — bucket store is a JVM-heap `Map<String, Bucket>` (now bounded LRU, but still not persistent)
  - `RateLimitProperties.java:17` — `enabled` flag is in-process only; no shared state
- Impact:
  - After any server restart (including a crash triggered during a brute-force attempt), all per-user bucket counters reset to full capacity. An attacker can immediately retry login (10 req/min) with fresh quota, bypassing the brute-force lockout window. For an on-premise deployment the restart vector is real (power cycle, deployment, crash).
- Minimum actionable fix:
  - Migrate bucket state to a persistent store (Redis, PostgreSQL) or Bucket4j's JCache integration. For a single-instance on-premise deployment, a Caffeine-backed store with TTL provides eviction without distributed infrastructure. As an interim measure, this review replaced the unbounded `ConcurrentHashMap` with a bounded LRU `LinkedHashMap` (50,000 entries) to prevent memory exhaustion from unique-IP flooding.
- Minimal verification path:
  - Add integration test restarting the filter context and confirming that accumulated failures do not reset; or document the restart-tolerance risk in the security runbook.

### Medium
2. **Severity: Medium**
- Title: Item stats endpoint excludes learner-level filtering — Prompt deviation
- Conclusion: **Fail (gap)**
- Evidence:
  - `AnalyticsController.java:103` — `learnerId` passed as `null` when constructing the item-stats filter, even when caller provides `learnerId` parameter
- Impact:
  - The Prompt requires "item difficulty/discrimination at learner, cohort, and course levels." Mentors cannot identify which specific items are most problematic for an individual student. The `AnalyticsService.itemStats()` SQL already supports `learnerId` via `buildParams`; only the controller assignment is missing.
- Minimum actionable fix:
  - Add `@RequestParam(required = false) UUID learnerId` to `itemStats()` and pass it to `buildFilter`. Add the `learnerId` scope enforcement logic (mirror `enforceLearnerScope` / `enforceOrgScope` already used by other analytics endpoints).
- Minimal verification path:
  - Add API test asserting that a student-scoped `learnerId` parameter filters item stats to only that student's attempts.

3. **Severity: Medium**
- Title: RecoveryDrillRunner.dropDrillDbQuietly missing host/port — orphaned databases on Docker
- Conclusion: **Fixed in this review**
- Evidence:
  - `RecoveryDrillRunner.java:117-129` (post-fix) — now uses `extractHost`/`extractPort` + `PGPASSWORD`
  - Pre-fix code called `psql -U meridian -c DROP DATABASE ...` with no `-h`/`-p`, causing silent connection failure on Docker host `postgres:5432`
- Impact:
  - When a drill fails mid-run (IOException or InterruptedException), the emergency cleanup path silently failed, leaving `meridian_drill_<timestamp>` databases permanently on the PostgreSQL server. Multiple failed drills would accumulate orphaned databases.
- Minimum actionable fix: Applied — `dropDrillDbQuietly` now uses the same `extractHost`/`extractPort` pattern as the rest of the class, with `PGPASSWORD` set from environment.
- Minimal verification path:
  - Simulate a drill failure (mock `pg_restore` exit non-zero) and confirm no orphaned databases remain after the cleanup path executes.

4. **Severity: Medium**
- Title: Audit action name inconsistency for rejection events
- Conclusion: **Fixed in this review**
- Evidence:
  - `ApprovalService.java:73` (pre-fix) — rejection audit used action `"PERMISSION_CHANGE"` (same as the generic change action); approval used `"PERMISSION_CHANGE_APPROVED"` (line 56)
- Impact:
  - Audit log queries filtering on `action = 'PERMISSION_CHANGE'` would ambiguously match both generic changes and rejection decisions, making compliance reports unreliable.
- Minimum actionable fix: Applied — rejection audit action renamed to `"PERMISSION_CHANGE_REJECTED"`.
- Minimal verification path:
  - Add `AuthAuditTest` case asserting rejection events appear as `PERMISSION_CHANGE_REJECTED` and approval events as `PERMISSION_CHANGE_APPROVED`.

### Low
5. **Severity: Low**
- Title: Missing `.env.example` entries for three runtime toggle variables
- Conclusion: **Fixed in this review**
- Evidence:
  - `application.yml:37,41,45` — references `EXPORTS_ADMIN_APPROVAL_REQUIRED`, `RECYCLE_BIN_RETENTION_DAYS`, `REGISTRATION_SLA_BUSINESS_DAYS`; all absent from `.env.example` before this review
- Impact:
  - Operators cannot discover these toggles from the configuration template, leaving defaults unknown and potentially misconfigured for regulated environments.
- Minimum actionable fix: Applied — all three variables added to `.env.example` with defaults and descriptions.

6. **Severity: Low**
- Title: Raw JSON string construction in audit and notification payloads
- Conclusion: **Partial Pass**
- Evidence:
  - `AuthService.java:177` — `"{\"ip\":\"" + clientIp + "\"}"`
  - `ApprovalService.java:75` — string-concatenated rejection reason in JSON payload
- Impact:
  - Not escaped for all special characters. For IP addresses the risk is negligible. For free-text rejection reasons, a backslash or control character could produce malformed JSON stored in the encrypted `audit_events.details` column.
- Minimum actionable fix:
  - Use `objectMapper.writeValueAsString(Map.of(...))` for all JSON payload construction. `ObjectMapper` is already injected in `ReportController` and `ReportRunner`; apply the same pattern in `AuthService` and `ApprovalService`.

7. **Severity: Low**
- Title: FACULTY_MENTOR cross-org analytics access — design intent unclear
- Conclusion: **Suspected gap — verify intent**
- Evidence:
  - `AnalyticsController.java:116-128` — `extractOrgFilter` returns `null` org filter for `FACULTY_MENTOR`, granting cross-org analytics access to all learner data when a `learnerId` from any org is passed
- Impact:
  - A faculty mentor can query analytics for students in any organization. The Prompt does not explicitly restrict faculty mentors to one organization but does not grant cross-org access either. If faculty mentors should be org-scoped, this is a data isolation gap.
- Minimum actionable fix:
  - Confirm design intent. If org-scoped, add enforcement in `extractOrgFilter` for `FACULTY_MENTOR` mirroring the `CORPORATE_MENTOR` pattern.

## 6. Security Review Summary
- Authentication entry points: **Pass**
  - Evidence: `AuthController.java:18-38`, `JwtAuthenticationFilter.java:35-52`, `SecurityConfig.java:51-57`
  - Reasoning: JWT stateless auth (15-min access token, 8h/12h refresh TTLs), refresh token rotation with family revocation on reuse (`AuthService.java:138-141`), BCrypt cost 12 (`SecurityConfig.java:78`), 5-failure lockout 15 min (`LockoutPolicy.java:10-11`), device fingerprint anomaly tracking.
- Route-level authorization: **Pass**
  - Evidence: `SecurityConfig.java:34-73` — all routes except `/api/v1/auth/**`, `/api/v1/health`, `/actuator/health` require authentication; `@EnableMethodSecurity` enables method-level `@PreAuthorize`
  - All admin controllers annotated `@PreAuthorize("hasRole('ADMIN')")` at class level: `BackupController.java:26`, `ApprovalController.java:19`, `AuditController.java`, `AnomalyController.java`, `AllowedIpRangeController.java`
- Object-level authorization: **Pass**
  - Evidence: `ReportController.java:221-226` — download requires owner or ADMIN; `ApprovalController.java:37-40` — reviewer ID from JWT, not request body; `IdempotencyService.java:49-51` — binds cached response to originating `userId`
  - Reasoning: Object ownership consistently checked at service/controller level for core resources.
- Function-level authorization: **Pass**
  - Evidence: `BackupController.java:26`, `OrgScopeApiTest.java:36-127` — tests all forbidden/allowed role combinations; session create/sync restricted to `STUDENT`; item stats excluded from `STUDENT` role (`AnalyticsController.java:88`)
- Tenant / user isolation: **Pass**
  - Evidence: `JwtService.java:38-43` — org ID embedded in JWT; `AnalyticsService.java:27-28` — `ou.organization_id = CAST(:orgId AS uuid)` EXISTS subquery; `ReportController.java:228-236` — corporate mentor org forced from JWT not request body
  - Minor: `FACULTY_MENTOR` has no org constraint on analytics (Low severity, design intent unclear — see Issues section)
- Admin / internal / debug protection: **Pass**
  - Evidence: All `/api/v1/admin/**` routes protected at controller class level; Actuator exposes `health` and `info` only (`application.yml:17-24`); Dockerfile `runtime` target excludes Maven and devtools

## 7. Tests and Logging Review
- Unit tests: **Pass**
  - Evidence: `unit_tests/server/` — 8 Java tests (PasswordPolicy, LockoutPolicy, JwtService, IdempotencyService, SyncResolver, AesAttributeConverter, AuthServiceAudit, TemplateRenderer); `unit_tests/web/` — 5 TypeScript tests (auth guard, auth store, outbox service, pending route, session sync keys)
  - Coverage enforced: `run_tests.sh:18` — `COV_THRESHOLD=90` via JaCoCo
- API / integration tests: **Pass**
  - Evidence: `api_tests/src/test/java/com/meridian/AuthApiTest.java:1-311` — 16 ordered integration tests covering full auth lifecycle; `OrgScopeApiTest.java:1-181` — 17 tests covering all role-boundary forbidden/allowed combinations; `SyncApiTest`, `ReportApiTest`, `AuthAuditTest`, `ClassificationApiTest`; uses `@SpringBootTest` with Testcontainers PostgreSQL
  - Gap: No tests for `BackupRunner` or `RecoveryDrillRunner` execution logic; rate-limit 429 behaviour not tested
- Logging categories / observability: **Pass**
  - Evidence: `AuthService.java:82` — `log.info("Registered user={} role={}", ...)` (no PII); `BackupRunner.java:82` — `log.error("Backup failed", e)`; `IdempotencyService.java:59` — `log.warn("Failed to deserialize ...")` (no payload in message)
  - Structured audit events written to `audit_events` table in separate transactions (`AuditEventPublisher.java`) separate from application logs
- Sensitive-data leakage risk in logs / responses: **Pass (with low-risk note)**
  - Evidence: Passwords not logged; JWTs not logged; refresh tokens stored only as `sha256(raw)` (`AuthService.java:198-208`); `User.java:30-33` — email encrypted via `@Convert(converter=AesAttributeConverter.class)`
  - Low-risk note: `AuthService.java:177` constructs audit detail JSON with client IP as plaintext; mitigated by AES-256-GCM encryption of the `audit_events.details` column

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests exist: yes — `unit_tests/server/` (8 Java), `unit_tests/web/` (5 TypeScript)
- API / integration tests exist: yes — `api_tests/src/test/java/com/meridian/` (6 Java)
- E2E tests exist: yes — `e2e_tests/tests/` (8 Playwright specs)
- Frameworks:
  - JUnit 5 + Spring MockMvc + Testcontainers (`api_tests`, `unit_tests/server`)
  - Angular Karma (`unit_tests/web`)
  - Playwright with V8 JS coverage (`e2e_tests`)
- Test entry points documented:
  - `README.md:92-109`, `run_tests.sh:5-7`
- Test commands in docs: yes — `README.md:97-108`
- Coverage enforcement: JaCoCo 90% threshold for server (`run_tests.sh:18`); V8 statement coverage for Angular E2E

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Registration → PENDING status | `AuthApiTest.java:43`, `e2e/01-register-approve-login.spec.ts:12` | `status=PENDING`, user found in pending list | basically covered | No direct assertion for 2-day SLA escalation behavior | Add integration test for escalation notification after SLA boundary (time-mocked) |
| Password complexity | `unit_tests/server/PasswordPolicyTest.java:11-63` | Valid/invalid parameterized cases + exception messages | sufficient | None major | Keep |
| 5-failure lockout + 15-min lockout | `unit_tests/server/LockoutPolicyTest.java`, `AuthApiTest.java:227-241` | Lock state transitions, API 403 ACCOUNT_LOCKED | basically covered | No deterministic time-forward unlock test | Add API test with fixed clock proving unlock after 15-min window |
| Pending account login 403 | `AuthApiTest.java:95-109` | `ACCOUNT_PENDING` code | sufficient | — | — |
| Valid login + token issuance | `AuthApiTest.java:111-133` | `accessToken`, `refreshToken`, `user.role` | sufficient | — | — |
| Wrong password 401 | `AuthApiTest.java:135-150` | `INVALID_CREDENTIALS` | sufficient | — | — |
| Suspended/deleted account 401 | `AuthApiTest.java:210-225` | `INVALID_CREDENTIALS` | sufficient | — | — |
| Locked account 403 | `AuthApiTest.java:227-241` | `ACCOUNT_LOCKED` | sufficient | — | — |
| Corporate mentor requires org code | `AuthApiTest.java:182-197` | `status=400` | sufficient | — | — |
| IP allowlist enforced 403 | `AuthApiTest.java:279-294` | `IP_NOT_ALLOWED` | sufficient | — | — |
| Refresh token rotation | `AuthApiTest.java:152-162` | new tokens issued | sufficient | — | — |
| Refresh token reuse 401 | `AuthApiTest.java:296-306` | `REFRESH_TOKEN_REUSE` | sufficient | — | — |
| Duplicate username 409 | `AuthApiTest.java:59-75` | `USERNAME_TAKEN` | sufficient | — | — |
| Offline sync (IndexedDB → server) | `SyncApiTest`, `e2e/02-offline-session.spec.ts` | session created via sync, status truthy | basically covered | Runtime sync timing cannot be confirmed statically | Runtime E2E required; add API test for empty sync payload |
| Idempotency / duplicate sync prevention | `unit_tests/server/IdempotencyServiceTest.java` | same response returned on repeat | basically covered | Integration test for sync endpoint duplicate-key path not confirmed | Add `SyncApiTest` case posting same idempotency key twice |
| Org isolation — cross-org analytics 403 | `OrgScopeApiTest.java:55-66`, `e2e/06-org-scope-isolation.spec.ts:55-80` | 403 cross-org, 200 same-org | sufficient | — | — |
| Non-student roles forbidden from session mutation | `OrgScopeApiTest.java:98-166` | 403 for FACULTY_MENTOR/CORPORATE_MENTOR/ADMIN on POST/PATCH/sync | sufficient | — | — |
| Unauthenticated → 401 | `OrgScopeApiTest.java:44-57` | 401 on sessions and admin endpoints | sufficient | — | — |
| Student forbidden from admin endpoints | `OrgScopeApiTest.java:59-89` | 403 on all admin endpoint families | sufficient | — | — |
| AES-256-GCM encrypt/decrypt | `unit_tests/server/AesAttributeConverterTest.java` | round-trip equality | sufficient | — | — |
| Audit events on login | `AuthAuditTest.java` | audit row exists | basically covered | Cannot confirm statically without reading full test assertions | Manual verify |
| Certification expiration report | `ReportApiTest.java` | report type coverage | cannot confirm statistically | Full test file not read | Verify CERT_EXPIRATION type in ReportApiTest |
| Item stats learner-level | No test found | — | missing | Gap matches Section 4.1.2 finding | Add test after controller fix |
| Backup run execution | No test found | — | missing | `BackupRunner.execute()` logic not tested | Add unit test mocking `ProcessBuilder` for pg_dump success/failure paths |
| Recovery drill execution | No test found | — | missing | `RecoveryDrillRunner` not tested | Add unit test for successful/failed drill paths including `dropDrillDbQuietly` |
| Rate-limit 429 + Retry-After | No test found | — | missing | Rate-limit filter behavior not tested | Add `RateLimitFilter` unit test asserting 429 and `Retry-After` header |
| Sensitive log exposure | No dedicated test | — | insufficient | No automated guard for accidental PII in logs/responses | Add assertions against response DTOs for sensitive field absence |

### 8.3 Security Coverage Audit
- Authentication: **Sufficient**
  - Evidence: `AuthApiTest.java` — 16 ordered cases cover all flows including lockout, suspend, refresh rotation, reuse, IP filtering, and expired-lock auto-clear.
- Route authorization (unauthenticated 401): **Sufficient**
  - Evidence: `OrgScopeApiTest.java:44-57` — explicit 401 checks on `/sessions` and `/admin/users`.
- Function-level authorization (role 403): **Sufficient**
  - Evidence: `OrgScopeApiTest.java:59-166` — covers student/faculty/corp-mentor/admin all forbidden combinations on session and admin endpoints.
- Object-level authorization (owner check): **Basically covered**
  - Evidence: `OrgScopeApiTest` covers session ownership boundary; report download owner check not explicitly tested.
  - Residual risk: report/recycle-bin cross-owner access not directly tested.
- Tenant / data isolation: **Sufficient**
  - Evidence: `OrgScopeApiTest` + `e2e/06-org-scope-isolation` confirm cross-org 403 for both mentor org pairs in both directions.
- Admin / internal endpoint protection: **Sufficient**
  - Evidence: `OrgScopeApiTest.java:59-89` — students get 403 on all admin endpoint families.
- Backup / recovery execution: **Missing**
  - No tests for `BackupRunner` or `RecoveryDrillRunner`; severe defects in those paths could ship undetected.
- Rate-limit enforcement: **Missing**
  - No test confirms 429 and `Retry-After` behavior; restart-bypass has no test coverage.

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Covered major risks:
  - Password policy, lockout basics, authn status handling, token lifecycle (rotation/reuse/expiry), sync/idempotency/LWW core behavior, org-scope isolation (analytics and session role boundaries), admin-endpoint protection.
- Uncovered/undercovered major risks:
  - Backup and recovery drill execution (no test coverage).
  - Rate-limit 429 behavior and restart-bypass risk (no test coverage).
  - Report download and recycle-bin object-level authorization (not directly tested).
  - Sensitive log/response leakage detection (no automated guard).
- Boundary statement:
  - Current tests could pass while a severe defect in backup execution or rate-limit enforcement remains undetected.

## 9. Final Notes
- This audit is static-only and evidence-bound; no runtime success claims are made.
- The most material unresolved risk is rate-limit bucket state reset on restart (High severity, partially mitigated by bounding the map). Persisting bucket state to a durable store would close this gap.
- Code changes applied in this review: `RecoveryDrillRunner.java` (dropDrillDbQuietly host/port fix), `ApprovalService.java` (audit action name for rejection), `RateLimitFilter.java` + `RateLimitProperties.java` (bounded LRU map + externalised config), `.env.example` (three undocumented toggles added).
