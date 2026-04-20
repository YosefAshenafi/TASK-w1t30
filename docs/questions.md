# Open Questions & Assumptions

The description below leaves the following points ambiguous. Each entry records the **Question**, the **Assumption** we will proceed with, and the concrete **Solution** (schema / numeric rule / UX rule) that `api-specs.md` and `design.md` must follow.

---

## Q1. What identifies an "organization" for corporate-mentor scope isolation?

- **Question**: The description requires corporate mentors to see only "their organization's learners and purchases," but the organization entity is not specified.
- **Assumption**: Each user is linked to exactly one `organization_id`. Learners are linked to the organization that purchased their enrollment. Corporate mentors are linked to one organization at registration and cannot be reassigned without Administrator approval.
- **Solution**:
  - Table `organizations(id UUID PK, name TEXT, code TEXT UNIQUE, created_at, updated_at)`.
  - `users.organization_id` (nullable for Students self-enrolled; required for Corporate Mentors).
  - `enrollments.organization_id` denormalized from the purchasing org for fast scope filtering.
  - All mentor-scoped queries add `WHERE organization_id = :viewerOrgId`.

## Q2. What are the exact token lifetimes and refresh rules?

- **Question**: "Token-based access" is stated, but lifetimes, refresh, and revocation are not.
- **Assumption**: JWT access tokens with sliding refresh. Access token 15 minutes, refresh token 8 hours (idle) / 12 hours (absolute). Refresh tokens are rotating and stored server-side for revocation.
- **Solution**:
  - `POST /auth/login` returns `{ accessToken, refreshToken, expiresIn: 900 }`.
  - `POST /auth/refresh` rotates the refresh token; reuse of a consumed token revokes the family and forces re-login.
  - Logout, password change, role change, and anomaly alerts trigger refresh-token revocation.

## Q3. How is the "new device fingerprint" computed and when does it alert?

- **Question**: Anomaly alerts trigger on "logins from a new device fingerprint," but the fingerprint composition is unspecified.
- **Assumption**: Fingerprint = SHA-256 of `{userAgent, acceptLanguage, screenResolution, timezone, platform}` captured client-side, stored per user.
- **Solution**:
  - Table `user_device_fingerprints(user_id, fingerprint_hash, first_seen_at, last_seen_at, trusted BOOLEAN)`.
  - On login, if the hash is unseen for this user, emit in-app notification `anomaly.newDevice` to the user and to Administrators.
  - Allow user to mark a fingerprint as trusted; untrusted fingerprints continue to alert on every login.

## Q4. What is the canonical definition of "seat utilization" and "refund/return rate"?

- **Question**: The reporting center names these metrics but does not define them.
- **Assumption**:
  - **Seat utilization** = `active_enrollments / total_seats_in_cohort`, evaluated per cohort per day.
  - **Refund/return rate** = `refunded_enrollments_in_window / total_enrollments_in_window`, per reporting window.
- **Solution**:
  - Report DTO exposes both numerator and denominator so downstream CSV/PDF can render the ratio.
  - Windowing supported: `day | week | month | quarter | custom(from,to)`.

## Q5. How granular is the activity check-off model during a training session?

- **Question**: "Completion check-offs per activity" is stated but the activity/set structure is not.
- **Assumption**: A `session` has ordered `activities`, each activity has 1..N `sets`, each set has a configurable `rest_seconds` (defaulting to the session-level setting), and each set is check-off-able independently.
- **Solution**:
  - Client-side IndexedDB store `sessionActivitySets(id, sessionId, activityId, setIndex, completedAt, restSeconds, notes)`.
  - Server mirrors this with `session_activity_sets` table; the `completedAt` is authoritative for last-write-wins.

## Q6. What is the conflict rule when two offline clients submit the same session?

- **Question**: "Last-write-wins" is specified, but duplicate-detection grain is not.
- **Assumption**: Each mutation carries an `idempotency_key` (client UUID v7) and each entity carries a `client_updated_at` timestamp (monotonic per client). Server keeps the write with the later `client_updated_at`; ties broken by lexicographic `idempotency_key`.
- **Solution**:
  - All POST/PUT/PATCH accept header `Idempotency-Key: <uuid>`.
  - Server persists `(idempotency_key, request_hash, response_body, created_at)` for 72 hours. Exact replays return the cached 2xx; replays with a different body return `409 Conflict`.

## Q7. What does "approval workflow for permission changes and data exports" look like?

- **Question**: The description requires an approval workflow but not its stages or SLAs.
- **Assumption**: Two-stage: requestor → approver(Administrator). Exports above a threshold (> 10,000 rows or `Restricted` classification) require approval before the job runs. Permission grants that elevate a user to a new role always require approval.
- **Solution**:
  - Table `approval_requests(id, type ENUM('PERMISSION_CHANGE','EXPORT'), payload JSONB, status ENUM('PENDING','APPROVED','REJECTED','EXPIRED'), requested_by, reviewed_by, created_at, decided_at, reason)`.
  - Pending requests expire in 7 days → `EXPIRED`.
  - In-app notification to all Administrators on `PENDING` creation; to requestor on decision.

## Q8. How are export-rate anomalies counted per user or per IP?

- **Question**: "More than 20 export attempts in 10 minutes" — per user, per IP, or global?
- **Assumption**: Per-user AND per-source-IP sliding windows are both tracked; the anomaly fires if either exceeds 20 attempts in a 10-minute window.
- **Solution**:
  - Redis-free, in-Postgres sliding window using `audit_events` where `action='EXPORT_ATTEMPT'`.
  - Triggered in a scheduled 1-minute job that evaluates the last 10 minutes.

## Q9. What is the Recycle Bin's scope and restore semantics?

- **Question**: Optional 14-day recycle bin — which entities, and who can restore?
- **Assumption**: Soft-delete applies to `users`, `enrollments`, `courses`, `assessment_items`, `session_logs`, `reports`. Restoration is Administrator-only. After 14 days, a nightly purge hard-deletes rows.
- **Solution**:
  - Each supported table has `deleted_at TIMESTAMPTZ` and `deleted_by UUID`.
  - Endpoints: `GET /admin/recycle-bin`, `POST /admin/recycle-bin/{type}/{id}/restore`, `DELETE /admin/recycle-bin/{type}/{id}` (hard-delete, audited).

## Q10. What are the IP-range and export-template configuration surfaces?

- **Question**: "Access outside allowed IP ranges" and "editable templates" imply configuration UIs that are not detailed.
- **Assumption**: Administrators manage an allow-list of CIDR ranges (per-role or global) and a set of notification templates (subject + markdown body + variable list).
- **Solution**:
  - `allowed_ip_ranges(id, cidr, role_scope NULLABLE, note, created_by)`.
  - `notification_templates(id, key UNIQUE, subject, body_md, variables JSONB, updated_by, updated_at)`.
  - Seeded keys: `export.ready`, `export.failed`, `anomaly.newDevice`, `anomaly.ipOutOfRange`, `anomaly.exportBurst`, `approval.requested`, `approval.decided`, `cert.expiring30`, `cert.expiring60`, `cert.expiring90`.
