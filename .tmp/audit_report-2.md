# Meridian Delivery Acceptance and Project Architecture Audit (Static-Only)

## 1. Verdict
- Overall conclusion: **Partial Pass**

## 2. Scope and Static Verification Boundary
- Reviewed:
  - Documentation/config/deployment: `README.md:1-207`, `.env.example:1-28`, `docker-compose.yml:1-91`, `nginx/nginx.conf:1-48`, `server/src/main/resources/application.yml:1-111`.
  - Backend architecture/security/business modules under `server/src/main/java/com/meridian/**` (auth, sessions, analytics, reports, approvals, backups, recycle-bin, notifications, governance, audit/anomaly).
  - DB migrations under `server/src/main/resources/db/migration/*`.
  - Angular routing/guards/offline/session/analytics/reports/admin pages under `web/src/app/**`.
  - Static test assets/config: `api_tests/**`, `unit_tests/**`, `e2e_tests/**`, `run_tests.sh:1-348`, `web/angular.json:1-123`.
- Not reviewed:
  - Runtime behavior, container/network behavior, browser rendering execution, DB execution plans, and actual backup/restore execution outcomes.
- Intentionally not executed:
  - Project startup, tests, Docker, external services.
- Manual verification required for:
  - HTTPS termination/certificate wiring in real deployment.
  - End-to-end offline sync behavior under real connectivity transitions.
  - Actual scheduler operations (report schedules, backup schedules, anomaly scans, retention jobs).
  - DR failover procedure execution on standby server.

## 3. Repository / Requirement Mapping Summary
- Prompt core goals mapped:
  - Multi-role training platform (Student/Corporate Mentor/Faculty Mentor/Admin), registration + pending approval, lockout/password policy, offline-first sessions with sync/idempotency/LWW, analytics filters, operations reports + exports + notifications, governance/classification/masking/encryption/audit/anomaly/rate limits, offline backups/retention/recovery drills/recycle bin.
- Main implementation areas mapped:
  - Auth/security: JWT + BCrypt + lockout + IP allow rules (`AuthService`, `SecurityConfig`, `RateLimitFilter`).
  - Offline/session sync: IndexedDB + outbox + sync resolver (`dexie.ts`, `offline.interceptor.ts`, `SessionStore`, `SyncResolver`).
  - Analytics/reporting: dedicated controllers/services/SQL filters (`AnalyticsController/Service`, `ReportController`, `ReportRunner`).
  - Governance and operations: masking/classification, audit/anomaly, backup/recovery/recycle features.

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 4.1.1 Documentation and static verifiability
- Conclusion: **Partial Pass**
- Rationale:
  - Startup/config docs are present and generally coherent (`README.md:26-207`, `.env.example:1-28`).
  - Major static verifiability gap: documented default test commands do not directly execute the provided API/unit test suites because those suites live outside `server/src/test` and are copied in by a custom script.
- Evidence:
  - Startup/test/config docs exist: `README.md:26-207`.
  - Custom copy-in test harness: `run_tests.sh:126-170`, `run_tests.sh:192-217`.
  - Server test tree contains resources only: `server/src/test/resources/application-test.yml:1-23`.
- Manual verification note:
  - Verify what `./mvnw test` actually runs in CI/local and whether review-critical tests are part of normal pipeline.

#### 4.1.2 Material deviation from Prompt
- Conclusion: **Partial Pass**
- Rationale:
  - Core business scenario is implemented (roles, sessions, analytics, reports, admin/ops modules).
  - Prompt explicitly requires HTTPS enabled with local certificate; delivery explicitly documents and configures HTTP-only local stack.
- Evidence:
  - HTTP-only statements: `README.md:41-43`, `README.md:204`, `nginx/nginx.conf:18-23`, `docker-compose.yml:69`.
  - Role and module coverage exists: `web/src/app/app.routes.ts:28-156`, `server/src/main/java/com/meridian/**`.
- Manual verification note:
  - If HTTPS is expected behind external infra not in-repo, deployment artifacts must be supplied for acceptance.

### 4.2 Delivery Completeness

#### 4.2.1 Coverage of explicit core functional requirements
- Conclusion: **Partial Pass**
- Rationale:
  - Implemented: registration pending workflow, password/lockout policy, role routes, session timers/rest windows, continue flow, offline local caching + outbox + sync LWW/idempotency, analytics/report modules, approvals/audit/anomaly/rate-limits/backups/recovery/recycle.
  - Missing/weak against explicit prompt: HTTPS local cert requirement not met; failover-to-standby procedure not documented in repo docs.
- Evidence:
  - Password + lockout: `PasswordPolicy.java:7-27`, `LockoutPolicy.java:10-35`.
  - Pending approval flow: `AuthService.java:81`, `AdminUserController.java:47-59`, `RegistrationSlaScheduler.java:24-74`.
  - Offline/session sync: `offline.interceptor.ts:14-33`, `SessionStore.java` not applicable, `web/src/app/sessions/session.store.ts:63-95`, `SyncResolver.java:42-147`.
  - Continue session flow: `sessions-list.component.ts:56-60`, `SessionController.java:87-97`.
  - HTTPS gap: `README.md:41-43`, `nginx/nginx.conf:18-23`.
  - DR docs gap in README scope: `README.md:1-207` (no standby failover procedure section).
- Manual verification note:
  - Validate if standby failover runbook exists outside repository.

#### 4.2.2 Basic end-to-end deliverable vs fragment/demo
- Conclusion: **Pass**
- Rationale:
  - Repository has full frontend/backend/migrations/config/admin features, not a single-file demo.
- Evidence:
  - Structure and modules: `README.md:140-154`, `server/src/main/java/com/meridian/**`, `web/src/app/**`.

### 4.3 Engineering and Architecture Quality

#### 4.3.1 Structure and module decomposition
- Conclusion: **Pass**
- Rationale:
  - Clear modular decomposition by domain (auth/sessions/reports/analytics/governance/backups).
- Evidence:
  - Package layout and dedicated controllers/services: `server/src/main/java/com/meridian/*`.

#### 4.3.2 Maintainability and extensibility
- Conclusion: **Partial Pass**
- Rationale:
  - Good separation overall, but some controllers/schedulers use in-memory filtering over full tables and synthetic paging wrappers, creating scalability and maintenance risks.
- Evidence:
  - Recycle bin list builds page response over full in-memory list: `RecycleBinController.java:48-60`.
  - Retention jobs load all rows then filter in memory: `BackupScheduler.java:74-89`, `RecycleBinRetentionScheduler.java:45-63`.

### 4.4 Engineering Details and Professionalism

#### 4.4.1 Error handling, logging, validation, API design
- Conclusion: **Partial Pass**
- Rationale:
  - Positive: centralized security responses/errors, structured JSON logging, validation annotations, idempotency conflict handling.
  - Gaps: key test reliability gaps and some weak assertions around security-critical behavior.
- Evidence:
  - Security error envelope and handlers: `SecurityConfig.java:61-73`, `GlobalExceptionHandler.java:1-73`.
  - Structured logging config: `logback-spring.xml:3-28`.
  - Idempotency conflict handling: `SyncResolver.java:45-56`, `92-106`.

#### 4.4.2 Product-grade vs demo-level
- Conclusion: **Partial Pass**
- Rationale:
  - Product-like breadth exists, but acceptance confidence is reduced by non-standard test wiring and limited direct assertions for several high-risk boundaries.
- Evidence:
  - Test-copy harness dependency: `run_tests.sh:126-170`, `192-217`.
  - Sparse assertions in key authorization tests: `OrgScopeApiTest.java:39-43`, `ReportApiTest.java:187-200`.

### 4.5 Prompt Understanding and Requirement Fit

#### 4.5.1 Business goal and constraints fit
- Conclusion: **Partial Pass**
- Rationale:
  - Strong fit on multi-role training/analytics/offline sync/ops workflows.
  - Security/deployment constraint mismatch on HTTPS local cert remains material.
- Evidence:
  - Role-aware navigation and endpoints: `app-shell.component.ts:17-24`, `app.routes.ts:28-156`.
  - Offline-first stack: `dexie.ts:66-87`, `offline.interceptor.ts:14-33`, `SessionSyncController.java:24-29`.
  - HTTPS mismatch: `README.md:41-43`, `nginx/nginx.conf:18-23`.

### 4.6 Aesthetics (frontend/full-stack)

#### 4.6.1 Visual/interaction quality
- Conclusion: **Cannot Confirm Statistically**
- Rationale:
  - Static templates show consistent component system, spacing, states, and banners, but render quality and interaction polish cannot be fully validated without runtime/browser execution.
- Evidence:
  - Shared UI and page templates: `web/src/app/shared/ui/*.ts`, `web/src/app/**/pages/*.component.ts`.
- Manual verification note:
  - Run manual UX pass on desktop/mobile and confirm visual coherence/accessibility interactions.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker / High first

1. **Severity: Blocker**
- Title: HTTPS + local certificate requirement not delivered
- Conclusion: **Fail**
- Evidence:
  - `README.md:41-43` (explicit plain HTTP only)
  - `README.md:204` (HTTPS deferred to production)
  - `nginx/nginx.conf:18-23` (listen 80)
  - `docker-compose.yml:69` (maps host to container port 80 only)
- Impact:
  - Direct mismatch with explicit prompt security requirement; acceptance gate risk.
- Minimum actionable fix:
  - Add HTTPS listener and local certificate management path in repo (or documented managed cert workflow) and make local deployment support HTTPS.

2. **Severity: High**
- Title: Review-critical tests are not in normal test source layout; default documented commands are misleading
- Conclusion: **Fail**
- Evidence:
  - README says run server tests with `./mvnw test`: `README.md:121-123`
  - Actual test execution depends on copying tests into source tree: `run_tests.sh:126-170`, `run_tests.sh:192-217`
  - `server/src/test` contains resources but no Java test classes (observed file tree; evidence anchor: `server/src/test/resources/application-test.yml:1-23`)
- Impact:
  - Static verifiability and acceptance confidence are reduced; core tests may be skipped in standard workflows.
- Minimum actionable fix:
  - Move API/unit tests into `server/src/test/java` and `web/src/**.spec.ts` (or equivalent native directories) and align README commands with real test execution.

3. **Severity: High**
- Title: Security/tenant isolation test assertions are frequently shallow and may miss real data-leak regressions
- Conclusion: **Partial Fail**
- Evidence:
  - Session isolation test only checks array existence/commented intent: `OrgScopeApiTest.java:39-43`
  - Report list tests only assert array/status, not ownership/org constraints: `ReportApiTest.java:187-200`
  - E2E anomaly test checks anomalies endpoint array only, not presence of target event: `05-new-device-anomaly.spec.ts:31-37`
- Impact:
  - Severe authz/isolation defects could pass tests undetected.
- Minimum actionable fix:
  - Add positive/negative content assertions for owner/org IDs, explicit forbidden/not-found behavior on cross-tenant object IDs, and concrete anomaly/export event checks.

### Medium / Low

4. **Severity: Medium**
- Title: Recycle-bin list endpoint does not perform true pagination
- Conclusion: **Fail**
- Evidence:
  - Fetches full filtered list and returns it as page window without slicing: `RecycleBinController.java:48-60`
- Impact:
  - Performance degradation and inconsistent API paging semantics as data grows.
- Minimum actionable fix:
  - Implement repository-level paginated queries for soft-deleted entities.

5. **Severity: Medium**
- Title: Retention jobs use full-table in-memory scans
- Conclusion: **Partial Fail**
- Evidence:
  - Backup retention scans all runs then filters: `BackupScheduler.java:74-76`
  - Recycle purge scans all users/courses then filters: `RecycleBinRetentionScheduler.java:45-57`
- Impact:
  - Scalability risk for large on-prem datasets.
- Minimum actionable fix:
  - Replace `findAll().stream().filter(...)` with cutoff-indexed repository queries.

6. **Severity: Medium**
- Title: Frontend unit coverage exclusions omit most product-critical modules
- Conclusion: **Partial Fail**
- Evidence:
  - Broad exclusions include auth/admin/analytics/reports/sessions and core guards/interceptors: `web/angular.json:84-108`
- Impact:
  - Reported frontend coverage can look healthy while critical paths remain untested.
- Minimum actionable fix:
  - Reduce exclusions to generated/irrelevant code; include critical guards/interceptors and feature modules.

7. **Severity: Low**
- Title: README frontend version claim mismatches actual dependencies
- Conclusion: **Fail**
- Evidence:
  - README says Angular 18: `README.md:13`
  - Package pins Angular 17.3: `web/package.json:16-23`, `34-36`
- Impact:
  - Documentation trust and onboarding friction.
- Minimum actionable fix:
  - Align README tech stack declaration with `package.json`.

## 6. Security Review Summary

- Authentication entry points: **Pass**
  - Evidence: `/auth/register/login/refresh/logout` implemented with password policy/lockout and JWT issuance: `AuthController.java:18-34`, `AuthService.java:54-169`, `PasswordPolicy.java:7-27`, `LockoutPolicy.java:10-35`.
- Route-level authorization: **Partial Pass**
  - Evidence: security chain + method-level guards: `SecurityConfig.java:52-60`, multiple `@PreAuthorize` annotations.
  - Note: confidence reduced by shallow authz test assertions (`OrgScopeApiTest.java:39-43`).
- Object-level authorization: **Partial Pass**
  - Evidence: ownership checks on sessions/reports/drafts/notifications (`SessionController.java:207-227`, `ReportController.java:221-225`, `AttemptDraftController.java:171-177`, `NotificationController.java:60-63`).
  - Note: tests do not consistently assert object-level negative cases with concrete foreign IDs.
- Function-level authorization: **Pass**
  - Evidence: role-specific endpoint guards for admin/report/session sync paths (`AdminUserController.java:23`, `ReportController.java:63`, `SessionSyncController.java:24`).
- Tenant / user data isolation: **Partial Pass**
  - Evidence: org checks in analytics/sessions/report creation for corporate mentors (`AnalyticsController.java:118-147`, `SessionController.java:173-186`, `ReportController.java:228-235`, `ReportRunner.java:114-115`).
  - Note: coverage quality is limited by weak content assertions in tests.
- Admin/internal/debug endpoint protection: **Pass**
  - Evidence: admin controllers protected via class-level `@PreAuthorize("hasRole('ADMIN')")` (`AdminUserController.java:23`, `ApprovalController.java:18`, `AuditController.java:21`, `AnomalyController.java:21`, `BackupController.java:26`, `RecycleBinController.java:26`).

## 7. Tests and Logging Review

- Unit tests: **Partial Pass**
  - Existence: backend and frontend unit tests exist in `unit_tests/**`.
  - Concern: they are not in native source test locations and are copied in by script (`run_tests.sh:126-140`, `192-231`).
- API/integration tests: **Partial Pass**
  - Existence: `api_tests/src/test/java/com/meridian/*.java`.
  - Concern: many tests verify status/shape only, not sensitive boundary content.
- Logging categories/observability: **Pass**
  - Evidence: structured JSON logging, request ID propagation, audit events for critical actions (`logback-spring.xml:3-28`, `RequestIdFilter.java:25-35`, `AuditEvent.java:52-65`, `AuthService.java:178-185`, `ReportRunner.java:57-84`).
- Sensitive-data leakage risk in logs/responses: **Partial Pass**
  - Positive: encrypted at-rest fields, response leakage guard tests exist (`AesAttributeConverter.java:23-77`, `SensitiveDataApiTest.java:39-125`).
  - Remaining risk: debug logging in local profile and limited depth of response-leak assertions.

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit/API/E2E tests exist:
  - Unit: `unit_tests/server/*`, `unit_tests/web/*`
  - API: `api_tests/src/test/java/com/meridian/*`
  - E2E: `e2e_tests/tests/*`
- Frameworks:
  - Backend: JUnit + Spring Boot MockMvc (`AuthApiTest.java:24-29`)
  - Frontend unit: Karma/Jasmine (`web/package.json:11-13`, `web/angular.json:74-80`)
  - E2E: Playwright (`e2e_tests/package.json:6`, `playwright.config.ts:3-22`)
- Test entry points:
  - Custom orchestration script: `run_tests.sh:121-279`
- Documentation for test commands:
  - Present in README (`README.md:116-139`), but does not clearly explain copy-in dependency for api/unit suites.

### 8.2 Coverage Mapping Table
| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Password policy + pending registration | `AuthApiTest.java:43-57`, `79-92`, `96-109` | Checks `201 + PENDING`, weak password `400`, pending login `403` | basically covered | No direct assertion for each password rule detail | Add explicit invalid cases for missing digit/symbol and message codes |
| 5-failure lockout 15 min | `AuthAuditTest.java:83-106`, `AuthApiTest.java:229-242` | Lockout status checked indirectly (`401/403`) | insufficient | No deterministic lockout timer boundary assertion | Add DB-backed assertion for `locked_until` and unlock-after-15-min behavior via controllable clock |
| Session sync idempotency + LWW + conflicts | `SyncApiTest.java:37-167` | CREATED/NOOP/UPDATED/OLDER_TIMESTAMP/IDEMPOTENCY_MISMATCH assertions | sufficient | No cross-user object-level negative test in this suite | Add student A vs student B sync attempt expecting ACCESS_DENIED conflict |
| Unauthenticated 401 gates | `OrgScopeApiTest.java:47-57`, `AuthAuditTest.java:118-121` | Direct `401` assertions | basically covered | Endpoint coverage not exhaustive | Add parameterized 401 checks for key protected routes |
| Unauthorized 403 role gates | `OrgScopeApiTest.java:61-166`, `ReportApiTest.java:170-182` | Multiple role-denial checks | basically covered | Some checks only status, no payload confidentiality assertion | Add response content assertions for denial cases |
| Object-level authorization (reports/sessions) | `ReportApiTest.java:73-89` | Cross-user report access `403` | insufficient | Session object-level foreign-ID access not explicitly asserted | Add explicit GET/PATCH/SET foreign session ID tests expecting `403/404` |
| Tenant/org isolation | `OrgIsolationContentApiTest.java:49-68`, `74-90` | Some content-level checks and 403 checks | insufficient | Heavy use of `@WithMockUser` may not model org claim in JWT principal (`AuthPrincipal.java:23`) | Add JWT-authenticated integration tests with real org claims and strict payload assertions |
| Report export workflow + approvals | `ReportApiTest.java:94-107`, `147-166`, `205-247` | Checks queued/approval/not-ready/download status | insufficient | Does not assert org-scoped data rows and ownership filters | Add seeded multi-org export data tests asserting no foreign-org rows |
| Sensitive response leakage | `SensitiveDataApiTest.java:39-125` | Asserts no `passwordBcrypt/password/token` fields | basically covered | Does not validate masking policy outputs for restricted viewers | Add assertions for masked username/email/display fields by role/org |
| Admin/internal endpoint protection | `OrgScopeApiTest.java:54-89`, `AuthAuditTest.java:110-114` | 401/403 checks on admin routes | basically covered | No deep checks for all admin modules | Add smoke matrix for all `/api/v1/admin/*` routes with student/corp/faculty/admin actors |

### 8.3 Security Coverage Audit
- Authentication: **Basically covered**
  - Evidence: auth lifecycle tests in `AuthApiTest`/`AuthAuditTest`.
- Route authorization: **Basically covered**
  - Evidence: role-gate tests in `OrgScopeApiTest`.
- Object-level authorization: **Insufficient**
  - Evidence: limited direct object-level assertions (mainly reports) and sparse session foreign-ID checks.
- Tenant/data isolation: **Insufficient**
  - Evidence: some org tests exist, but `@WithMockUser` principal modeling can miss JWT org-claim reality (`AuthPrincipal.java:15-24`).
- Admin/internal protection: **Basically covered**
  - Evidence: explicit 401/403 tests for several admin endpoints; should be broadened.

### 8.4 Final Coverage Judgment
- **Final Coverage Judgment: Partial Pass**
- Boundary explanation:
  - Covered: core auth happy paths, some 401/403 gates, sync idempotency/LWW conflict matrix, baseline sensitive-field response checks.
  - Not sufficiently covered: strong object-level isolation proofs, org/tenant isolation with real JWT claim semantics, and deep export/report data-scope assertions.
  - Result: tests could still pass while severe isolation defects remain.

## 9. Final Notes
- Static audit found a substantial implementation with broad domain coverage.
- Acceptance is currently constrained by one explicit prompt mismatch (HTTPS/local cert) and by test verifiability/coverage-quality weaknesses in security-critical paths.
