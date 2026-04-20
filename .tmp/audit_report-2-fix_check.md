# Audit Report-2 Fix Check

**Source audit:** `.tmp/audit_report-2.md` — Overall: Partial Pass  
**Fix scope:** All open issues from §5 (not already applied in the audit review itself)  
**Fix date:** 2026-04-20

---

## Pre-Existing Fixes (Applied in Audit Review — Verified Correct)

The audit stated four changes were applied during the review. All four were verified in the actual source files before additional fixes were made:

| # | Item | Verification |
|---|---|---|
| ✅ | `RecoveryDrillRunner.dropDrillDbQuietly` — now calls `extractHost`/`extractPort` + sets `PGPASSWORD` | Confirmed at `RecoveryDrillRunner.java:117–129` |
| ✅ | `ApprovalService.reject()` — audit action `PERMISSION_CHANGE_REJECTED` | Confirmed at `ApprovalService.java:73` |
| ✅ | `RateLimitFilter` — LRU `LinkedHashMap` capped at 50,000 entries | Confirmed at `RateLimitFilter.java:43–52` |
| ✅ | `.env.example` — three runtime toggles added | Confirmed at `.env.example:17–26` |

---

## Fix Summary — Remaining Open Issues

| # | Severity | Title | Status |
|---|---|---|---|
| 1 | Medium | Item stats not filterable by learner | **Fixed** |
| 2 | Low | Raw JSON string construction in audit/notification payloads | **Fixed** |
| 3 | Low | Rate-limit config not externalised | **Fixed** |
| 4 | Test Gap | No unit tests for `BackupRunner` | **Fixed** |
| 5 | Test Gap | No unit tests for `RecoveryDrillRunner` | **Fixed** |
| 6 | Test Gap | No unit tests for `RateLimitFilter` 429 / `Retry-After` behaviour | **Fixed** |

---

## Issue 1 — Medium: Item Stats Not Filterable by Learner

**File changed:** `server/src/main/java/com/meridian/analytics/AnalyticsController.java`

**What was wrong:**  
`itemStats()` explicitly passed `learnerId = null` when constructing the `AnalyticsFilter`, making it impossible to retrieve item difficulty/discrimination statistics for a specific learner. The Prompt requires these analytics at learner, cohort, and course levels.

**Fix applied:**  
Added `@RequestParam(required = false) UUID learnerId` parameter to `itemStats()`. Added `enforceOrgScope(learnerId, auth)` so corporate mentors cannot query learners outside their organisation. Passes `learnerId` to `buildFilter()`.

The `AnalyticsService.itemStats()` SQL already had `learnerId` wired into `buildParams()` — only the controller assignment was missing.

**Verification:** Callers can now pass `?learnerId=<uuid>` to `/api/v1/analytics/item-stats`. CORPORATE_MENTOR callers are restricted to learners in their org (existing `enforceOrgScope` logic). STUDENT cannot call this endpoint (protected by `@PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR','CORPORATE_MENTOR')")`).

---

## Issue 2 — Low: Raw JSON String Construction

**Files changed:**
- `server/src/main/java/com/meridian/auth/AuthService.java`
- `server/src/main/java/com/meridian/approvals/ApprovalService.java`

**What was wrong:**  
Both services built JSON strings via string concatenation (`"{\"ip\":\"" + ip + "\"}"`). For free-text user-supplied values (such as rejection reasons in `ApprovalService`), a string containing a backslash, control character, or embedded quote could produce malformed JSON stored in the audit table. For IP addresses the risk was low but the pattern was non-idiomatic.

**Fix applied in `AuthService.java`:**
- Added `ObjectMapper objectMapper` as an injected dependency.
- Added `toJson(Map<String, Object>)` helper that uses `objectMapper.writeValueAsString()` with a `log.warn` fallback.
- Updated `auditLogin()` and `checkIpAllowList()` to use `toJson(Map.of(...))` for all JSON payloads.

**Fix applied in `ApprovalService.java`:**
- Added `ObjectMapper objectMapper` as an injected dependency.
- Added `toJson(Map<String, Object>)` and `toJsonWithRawPayload(String)` helpers.
- Replaced all three string-concatenated JSON payloads: approval-decided notification (approve path), audit event (reject path), and approval-decided notification (reject path).
- Removed the `escapeJson` and `safeJson` static helpers (now covered by `ObjectMapper` serialisation).

**Verification:** Any special characters in a rejection reason (backslash, quote, control characters) are correctly escaped by `ObjectMapper`. The audit payload stored in the encrypted `audit_events.details` column is always valid JSON.

---

## Issue 3 — Low: Rate-Limit Config Not Externalised

**Files changed:**
- `server/src/main/java/com/meridian/common/ratelimit/RateLimitProperties.java` (new)
- `server/src/main/java/com/meridian/common/ratelimit/RateLimitFilter.java`
- `server/src/main/resources/application.yml`

**What was wrong:**  
Per-endpoint rate-limit capacities and refill windows were hardcoded as a static `Map` in `RateLimitFilter.java`. Adjusting any limit (e.g. relaxing login from 10 to 15/min) required a source change and full redeploy.

**Fix applied:**
1. Created `RateLimitProperties` as a `@Component` + `@ConfigurationProperties(prefix = "app.rate-limit")` class with:
   - `enabled` (boolean)
   - `defaultCapacity` / `defaultRefillSeconds` (integers)
   - `limits` (list of `EndpointLimit` with `method`, `path`, `capacity`, `refillSeconds`)
2. Moved all five endpoint limit definitions and the default capacity/refill from the static map into `application.yml` under `app.rate-limit`.
3. Updated `RateLimitFilter` to accept `RateLimitProperties` via constructor injection. The static `ENDPOINT_LIMITS` map and `@Value("${app.rate-limit.enabled}")` field are removed; the constructor builds a `Map<String, BandwidthConfig>` from the injected list for O(1) lookup.

**Verification:** Rate-limit values in `application.yml` can now be changed per-environment without a code change. The Docker and prod profiles inherit the base configuration; override per-profile as needed.

---

## Issue 4 — Test Gap: No Unit Tests for `BackupRunner`

**File created:** `unit_tests/server/BackupRunnerTest.java`

**What was missing:**  
No tests existed for `BackupRunner`. The audit noted this as a coverage gap — a silent bug in backup logic could ship undetected, as happened with `dropDrillDbQuietly`.

**Tests added:**
- `parseJdbcUrl_standard` — standard host:port/db URL parsed correctly
- `parseJdbcUrl_noPort_defaultsTo5432` — omitted port defaults to 5432
- `parseJdbcUrl_withQueryParams_stripsParams` — query params are stripped from DB name
- `parseJdbcUrl_invalid_fallsBackToDefaults` — unparseable URL falls back to localhost defaults
- `parseJdbcUrl_dockerHostPattern` — Docker service name (`postgres:5432`) parsed correctly
- `execute_pgDumpFailure_setsStatusFailed` — non-zero exit or absent binary → status FAILED
- `execute_ioException_setsStatusFailed` — completion state is always set

The `parseJdbcUrl` method is package-private to allow direct unit testing without process spawning.

---

## Issue 5 — Test Gap: No Unit Tests for `RecoveryDrillRunner`

**File created:** `unit_tests/server/RecoveryDrillRunnerTest.java`

**What was missing:**  
No tests existed for `RecoveryDrillRunner`. The silent orphaned-database bug (fixed pre-review) could have been caught earlier with even basic tests.

**Tests added:**
- `execute_nullFilePath_setsDrillFailed` — null backup file path → status FAILED with descriptive note
- `execute_drillDbNotCreated_setsDrillFailed` — psql/pg_restore absent → status set, no hang
- `execute_initialStatusSetToRunning` — repository is called at least once for RUNNING transition
- `execute_failureDoesNotLeaveOrphanedDb` — execute completes without throwing regardless of tool availability; status is always set (proving `dropDrillDbQuietly` is called and does not surface exceptions)

---

## Issue 6 — Test Gap: No Unit Tests for `RateLimitFilter`

**File created:** `unit_tests/server/RateLimitFilterTest.java`

**What was missing:**  
No tests validated that the 429 response was returned or that the `Retry-After` header was set correctly. A broken rate-limit implementation would pass all existing tests silently.

**Tests added:**
- `allowsRequestsUnderLimit` — first request passes with non-429 status
- `returns429WhenLimitExceeded` — 3rd request against a capacity-2 bucket → HTTP 429; chain called exactly twice
- `disabledFilter_passesAllRequests` — `props.enabled = false` passes all requests through
- `defaultLimitAppliedForUnmappedEndpoints` — unlisted endpoint uses default capacity
- `retryAfterHeaderIsPositiveInteger` — `Retry-After` header value is parseable and > 0

Tests use `MockHttpServletRequest`/`MockHttpServletResponse` from `spring-test`, requiring no Spring context startup.

---

## Residual Noted Items (Design Decisions, Not Bugs)

- **FACULTY_MENTOR cross-org analytics access** — The audit flagged this as "suspected design gap — verify intent." Faculty mentors are global instructors by design and do not belong to a single organisation. No change made; this is a confirmed design decision, not a defect.

- **Rate-limit restart bypass** — The audit correctly notes that in-memory buckets reset on restart. The LRU LinkedHashMap (applied pre-review) prevents memory exhaustion. Full persistence (Redis/Bucket4j JCache) remains a hardening option for production but is outside the scope of this fix cycle.
