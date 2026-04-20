# Audit Report-1 Fix Check

**Source audit:** `.tmp/audit_report-1.md` — Original verdict: Partial Pass  
**Fix scope:** All issues from §5 plus all remaining §8.2 / §8.3 coverage gaps  
**Fix date:** 2026-04-20  
**Post-fix verdict:** **Pass**

---

## Fix Summary

| # | Severity | Title | Status |
|---|---|---|---|
| 1 | High | Session mutation APIs lack strict role boundary enforcement | **Fixed** |
| 2 | Medium | Data masking policy implementation is narrow | **Fixed** |
| 3 | Medium | Security test coverage insufficient for object-level authorization | **Fixed** |
| 4 | Medium | Endpoint coverage doc contains incorrect controller mapping | **Fixed** |
| 5 | Medium | "One-tap continue session" only indirectly represented | **Fixed** |
| 6 | Low | Notification severity enum mismatch (CRITICAL missing) | **Fixed** |
| 7 | Test Gap | Org-scope content assertions missing (§8.2) | **Fixed** |
| 8 | Test Gap | Report data-scope tests for corporate mentor org isolation (§8.2) | **Fixed** |
| 9 | Test Gap | Sensitive data leakage detection in responses (§8.2) | **Fixed** |

---

## Issue 1 — High: Session Mutation Role Boundary Enforcement

**Files changed:**
- `server/src/main/java/com/meridian/sessions/SessionController.java`
- `server/src/main/java/com/meridian/sessions/SessionSyncController.java`

**What was wrong:**  
All session mutation endpoints accepted any authenticated caller regardless of role. `requireSession()` only enforced STUDENT ownership and CORPORATE_MENTOR org scope; FACULTY_MENTOR fell through unchecked. The sync endpoint accepted any authenticated user.

**Fix applied:**  
Added `@PreAuthorize("hasRole('STUDENT')")` to every mutation handler in `SessionController` (create, patch, pause, continue, complete, createSet, patchSet) and to `sync()` in `SessionSyncController`. Read endpoints retain supervisory-role access unchanged.

**Verification:** Non-student roles receive HTTP 403 on any mutation. Students retain full mutation access with ownership validation from `requireSession()`.

---

## Issue 2 — Medium: Data Masking Policy Is Narrow

**Files changed:**
- `server/src/main/java/com/meridian/governance/MaskingPolicy.java`
- `server/src/main/java/com/meridian/reports/runner/ReportRunner.java`

**What was wrong:**  
`MaskingPolicy` had no centralised field-dispatch logic. `maskRows()` hard-coded a `display_name` branch. ENROLLMENTS and CERT_EXPIRING reports omitted `email` from masked output.

**Fix applied:**
1. Added `maskPhone(String)` to `MaskingPolicy`.
2. Added `maskField(String fieldName, String value)` — central dispatcher routing `email`, `display_name`/`full_name`, `phone`/`mobile` to their appropriate masking strategy.
3. Updated `maskRows()` to call `maskingPolicy.maskField(field, value)`.
4. ENROLLMENTS now selects and masks `u.email`.
5. CERT_EXPIRING now selects and masks `u.display_name` and `u.email`.

**Verification:** Adding a new sensitive field to a report requires only listing it in `maskRows()`; strategy is resolved centrally.

---

## Issue 3 — Medium: Security Test Coverage (Object-Level Authorization)

**Files changed:**
- `api_tests/src/test/java/com/meridian/OrgScopeApiTest.java`

**What was wrong:**  
No API tests proved that non-student roles cannot mutate `/sessions` or `/sessions/sync`.

**Fix applied:**  
Added 8 tests (orders 10–17):
- FACULTY_MENTOR, CORPORATE_MENTOR, ADMIN all get 403 on `POST /sessions`
- FACULTY_MENTOR, CORPORATE_MENTOR get 403 on `PATCH /sessions/{id}`
- FACULTY_MENTOR, CORPORATE_MENTOR get 403 on `POST /sessions/sync`
- Student is NOT forbidden from `POST /sessions/sync` (passes auth gate)

---

## Issue 4 — Medium: Endpoint Coverage Doc Incorrect Mapping

**Files changed:**
- `docs-generated/endpoint-coverage.md`

**Fix applied:**  
Corrected `GET /courses/{id}/assessment-items` mapping from `AssessmentItemController.list()` to `CourseController.items()`.

---

## Issue 5 — Medium: "One-Tap Continue Session" Only Indirect

**Files changed:**
- `web/src/app/sessions/pages/sessions-list.component.ts`

**Fix applied:**
1. Added a dedicated **Continue** button (brand-colored pill) visible only for `IN_PROGRESS` or `PAUSED` sessions.
2. Added `sortedSessions()` that floats IN_PROGRESS → PAUSED → other, ensuring active sessions are immediately visible.

---

## Issue 6 — Low: CRITICAL Severity Missing from Notification Map

**Files changed:**
- `web/src/app/notifications/pages/inbox.component.ts`

**Fix applied:**  
Added `CRITICAL: 'bg-red-700'` to `severityDot()`. CRITICAL notifications now render a dark-red indicator dot.

---

## Issue 7 — Test Gap: Org-Scope Content Assertions

**File created:** `api_tests/src/test/java/com/meridian/OrgIsolationContentApiTest.java`

**What was missing:**  
From §8.2: "API test asserting filtered content ownership by org for analytics/session list." Tests only checked HTTP status codes; no assertion proved the payload did not contain cross-org data.

**Tests added (content-level assertions):**
- `corpMentorA_sessionList_doesNotContainOrgBSessions` — response content asserted to exclude org-B student IDs
- `corpMentorB_sessionList_doesNotContainOrgASessions` — symmetric check
- `corpMentorA_cannotQueryAnalyticsForOrgBLearner` — mastery-trends 403 for cross-org learner
- `corpMentorA_canQueryAnalyticsForOwnOrgLearner` — not-403 for own-org learner
- `admin_sessionListNotRestrictedByOrg` — admin sees all sessions (no false restriction)

---

## Issue 8 — Test Gap: Report Data-Scope Tests for Corporate Mentor

**File created:** `api_tests/src/test/java/com/meridian/OrgIsolationContentApiTest.java` (same file)

**What was missing:**  
From §8.2: "Add report data-scope tests for corp mentor org isolation."

**Tests added:**
- `admin_canQueryItemStatsByLearnerId` — validates the audit-2 fix: learnerId now accepted in item-stats
- `corpMentorA_itemStats_forbiddenForOrgBLearner` — 403 when querying cross-org learner in item-stats
- `corpMentorA_reportList_returnsOkAndIsArray` — corp mentor can list their own reports
- `corpMentorA_cannotDownloadOrgBReport` — attempting to access an org-B report ID returns 403 or 404, never 200

---

## Issue 9 — Test Gap: Sensitive Data Leakage Detection

**File created:** `api_tests/src/test/java/com/meridian/SensitiveDataApiTest.java`

**What was missing:**  
From §8.2: "No automated guard for accidental sensitive data leakage." No tests asserted that response DTOs were free of `passwordBcrypt`, raw tokens, or other sensitive internal fields.

**Tests added:**
- `register_responseDoesNotExposePasswordHash` — registration response has no `passwordBcrypt`/`password`/`passwordHash` fields
- `usersMe_doesNotExposePasswordBcrypt` — GET /users/me has no password hash
- `usersMe_doesNotExposeRefreshToken` — GET /users/me has no token fields
- `sessionList_studentOnlySeesOwnStudentId` — content assertion: every session in the student's list has `studentId` equal to the calling user
- `sessionList_doesNotExposeOtherUserPasswordFields` — no password fields in session array
- `adminUserList_doesNotExposePasswordBcrypt` — admin user list strips password hash
- `notificationList_doesNotExposeRawTokens` — notification payload has no token fields

---

## Post-Fix Verdict Mapping

| Audit Section | Original | Post-Fix |
|---|---|---|
| §4.1.2 Material deviation from Prompt | Partial Pass | **Pass** |
| §4.2.1 Core explicit requirements coverage | Partial Pass | **Pass** |
| §4.3.2 Maintainability/extensibility | Partial Pass | **Pass** |
| §4.4.1 Error handling / API design | Partial Pass | **Pass** |
| §4.5.1 Business goal / scenario fit | Partial Pass | **Pass** |
| §6 Security — Object-level authorization | **Fail** | **Pass** |
| §6 Security — Route-level authorization | Partial Pass | **Pass** |
| §6 Security — Function-level authorization | Partial Pass | **Pass** |
| §6 Security — Tenant / user isolation | Partial Pass | **Pass** |
| §8.3 Object-level authorization coverage | Insufficient | **Basically Covered** |
| §8.3 Tenant / data isolation coverage | Insufficient | **Basically Covered** |
| §8.4 Final coverage judgment | Partial Pass | **Pass** |
| **Overall** | **Partial Pass** | **Pass** |
