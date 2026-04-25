# Test Coverage Audit

## Scope and Method
- Audit mode: static inspection only (no execution).
- Evidence scope inspected: backend controllers/routes, backend tests, frontend unit tests, E2E tests, and README.
- Project type declaration: **fullstack** from `repo/README.md`.

## Backend Endpoint Inventory
- `DELETE /api/v1/admin/allowed-ip-ranges/{var}` (source: `repo/server/src/main/java/com/meridian/governance/AllowedIpRangeController.java`)
- `DELETE /api/v1/admin/recycle-bin/{var}/{var}` (source: `repo/server/src/main/java/com/meridian/recyclebin/RecycleBinController.java`)
- `DELETE /api/v1/courses/{var}` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `DELETE /api/v1/reports/schedules/{var}` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `DELETE /api/v1/sessions/{var}/attempt-drafts` (source: `repo/server/src/main/java/com/meridian/sessions/AttemptDraftController.java`)
- `GET /api/v1/admin/allowed-ip-ranges` (source: `repo/server/src/main/java/com/meridian/governance/AllowedIpRangeController.java`)
- `GET /api/v1/admin/anomalies` (source: `repo/server/src/main/java/com/meridian/security/anomaly/AnomalyController.java`)
- `GET /api/v1/admin/approvals` (source: `repo/server/src/main/java/com/meridian/approvals/ApprovalController.java`)
- `GET /api/v1/admin/audit` (source: `repo/server/src/main/java/com/meridian/security/audit/AuditController.java`)
- `GET /api/v1/admin/backups` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `GET /api/v1/admin/backups/policy` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `GET /api/v1/admin/backups/recovery-drills` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `GET /api/v1/admin/notification-templates` (source: `repo/server/src/main/java/com/meridian/notifications/TemplateController.java`)
- `GET /api/v1/admin/recycle-bin` (source: `repo/server/src/main/java/com/meridian/recyclebin/RecycleBinController.java`)
- `GET /api/v1/admin/recycle-bin/policy` (source: `repo/server/src/main/java/com/meridian/recyclebin/RecycleBinController.java`)
- `GET /api/v1/admin/users` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `GET /api/v1/admin/users/{var}` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `GET /api/v1/analytics/item-stats` (source: `repo/server/src/main/java/com/meridian/analytics/AnalyticsController.java`)
- `GET /api/v1/analytics/mastery-trends` (source: `repo/server/src/main/java/com/meridian/analytics/AnalyticsController.java`)
- `GET /api/v1/analytics/weak-knowledge-points` (source: `repo/server/src/main/java/com/meridian/analytics/AnalyticsController.java`)
- `GET /api/v1/analytics/wrong-answers` (source: `repo/server/src/main/java/com/meridian/analytics/AnalyticsController.java`)
- `GET /api/v1/courses` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `GET /api/v1/courses/{var}/activities` (source: `repo/server/src/main/java/com/meridian/courses/ActivityController.java`)
- `GET /api/v1/courses/{var}/assessment-items` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `GET /api/v1/courses/{var}/cohorts` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `GET /api/v1/courses/{var}/knowledge-points` (source: `repo/server/src/main/java/com/meridian/courses/KnowledgePointController.java`)
- `GET /api/v1/health` (source: `repo/server/src/main/java/com/meridian/HealthController.java`)
- `GET /api/v1/notifications` (source: `repo/server/src/main/java/com/meridian/notifications/NotificationController.java`)
- `GET /api/v1/notifications/unread-count` (source: `repo/server/src/main/java/com/meridian/notifications/NotificationController.java`)
- `GET /api/v1/reports` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `GET /api/v1/reports/schedules` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `GET /api/v1/reports/{var}` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `GET /api/v1/reports/{var}/download` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `GET /api/v1/sessions` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `GET /api/v1/sessions/{var}` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `GET /api/v1/sessions/{var}/attempt-drafts` (source: `repo/server/src/main/java/com/meridian/sessions/AttemptDraftController.java`)
- `GET /api/v1/users/me` (source: `repo/server/src/main/java/com/meridian/users/UserController.java`)
- `PATCH /api/v1/admin/users/{var}/status` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `PATCH /api/v1/sessions/{var}` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `PATCH /api/v1/sessions/{var}/sets/{var}` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/admin/allowed-ip-ranges` (source: `repo/server/src/main/java/com/meridian/governance/AllowedIpRangeController.java`)
- `POST /api/v1/admin/anomalies/{var}/resolve` (source: `repo/server/src/main/java/com/meridian/security/anomaly/AnomalyController.java`)
- `POST /api/v1/admin/approvals/{var}/approve` (source: `repo/server/src/main/java/com/meridian/approvals/ApprovalController.java`)
- `POST /api/v1/admin/approvals/{var}/reject` (source: `repo/server/src/main/java/com/meridian/approvals/ApprovalController.java`)
- `POST /api/v1/admin/backups/recovery-drill` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `POST /api/v1/admin/backups/run` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `POST /api/v1/admin/recycle-bin/{var}/{var}/restore` (source: `repo/server/src/main/java/com/meridian/recyclebin/RecycleBinController.java`)
- `POST /api/v1/admin/users/{var}/approve` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `POST /api/v1/admin/users/{var}/reject` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `POST /api/v1/admin/users/{var}/unlock` (source: `repo/server/src/main/java/com/meridian/users/AdminUserController.java`)
- `POST /api/v1/assessment-items` (source: `repo/server/src/main/java/com/meridian/courses/AssessmentItemController.java`)
- `POST /api/v1/auth/login` (source: `repo/server/src/main/java/com/meridian/auth/AuthController.java`)
- `POST /api/v1/auth/logout` (source: `repo/server/src/main/java/com/meridian/auth/AuthController.java`)
- `POST /api/v1/auth/refresh` (source: `repo/server/src/main/java/com/meridian/auth/AuthController.java`)
- `POST /api/v1/auth/register` (source: `repo/server/src/main/java/com/meridian/auth/AuthController.java`)
- `POST /api/v1/courses` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `POST /api/v1/courses/{var}/activities` (source: `repo/server/src/main/java/com/meridian/courses/ActivityController.java`)
- `POST /api/v1/courses/{var}/knowledge-points` (source: `repo/server/src/main/java/com/meridian/courses/KnowledgePointController.java`)
- `POST /api/v1/notifications/read-all` (source: `repo/server/src/main/java/com/meridian/notifications/NotificationController.java`)
- `POST /api/v1/notifications/{var}/read` (source: `repo/server/src/main/java/com/meridian/notifications/NotificationController.java`)
- `POST /api/v1/reports` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `POST /api/v1/reports/schedules` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `POST /api/v1/reports/{var}/cancel` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)
- `POST /api/v1/sessions` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/sessions/attempt-drafts` (source: `repo/server/src/main/java/com/meridian/sessions/AttemptDraftController.java`)
- `POST /api/v1/sessions/sync` (source: `repo/server/src/main/java/com/meridian/sessions/SessionSyncController.java`)
- `POST /api/v1/sessions/{var}/complete` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/sessions/{var}/continue` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/sessions/{var}/pause` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/sessions/{var}/sets` (source: `repo/server/src/main/java/com/meridian/sessions/SessionController.java`)
- `POST /api/v1/sessions/{var}/submit-attempts` (source: `repo/server/src/main/java/com/meridian/sessions/AttemptDraftController.java`)
- `PUT /api/v1/admin/backups/policy` (source: `repo/server/src/main/java/com/meridian/backups/BackupController.java`)
- `PUT /api/v1/admin/notification-templates/{var}` (source: `repo/server/src/main/java/com/meridian/notifications/TemplateController.java`)
- `PUT /api/v1/assessment-items/{var}` (source: `repo/server/src/main/java/com/meridian/courses/AssessmentItemController.java`)
- `PUT /api/v1/courses/{var}` (source: `repo/server/src/main/java/com/meridian/courses/CourseController.java`)
- `PUT /api/v1/reports/schedules/{var}` (source: `repo/server/src/main/java/com/meridian/reports/ReportController.java`)

## API Test Mapping Table
| Endpoint | Covered | Test type | Test file | Evidence |
|---|---|---|---|---|
| `DELETE /api/v1/admin/allowed-ip-ranges/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `allowedIpRanges_studentJwt_returns403OnAllMethods` |
| `DELETE /api/v1/admin/recycle-bin/{var}/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `recycleBinHardDelete_studentJwt_returns403` |
| `DELETE /api/v1/courses/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesDelete_studentJwt_returns403` |
| `DELETE /api/v1/reports/schedules/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsScheduleLifecycle_createUpdateListDelete_overRealHttp` |
| `DELETE /api/v1/sessions/{var}/attempt-drafts` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsAttemptDrafts_fullCrudOverRealHttp` |
| `GET /api/v1/admin/allowed-ip-ranges` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `allowedIpRanges_fullCrud_overRealHttp` |
| `GET /api/v1/admin/anomalies` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminAnomalies_withAdminJwt_returnsPage` |
| `GET /api/v1/admin/approvals` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminApprovals_withAdminJwt_returnsPage` |
| `GET /api/v1/admin/audit` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminAudit_withAdminJwt_returnsPageShape` |
| `GET /api/v1/admin/backups` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminBackups_list_withAdminJwt_returnsPageShape` |
| `GET /api/v1/admin/backups/policy` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `backupsPolicy_getAndRoundtrip_overRealHttp` |
| `GET /api/v1/admin/backups/recovery-drills` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminBackups_recoveryDrillsList_withAdminJwt_returnsPageShape` |
| `GET /api/v1/admin/notification-templates` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notificationTemplatesList_adminJwt_returnsPage` |
| `GET /api/v1/admin/recycle-bin` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `recycleBinList_adminJwt_returnsPage` |
| `GET /api/v1/admin/recycle-bin/policy` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `recycleBinPolicy_adminJwt_returnsRetentionDays` |
| `GET /api/v1/admin/users` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsers_withAdminJwt_returnsPageShape` |
| `GET /api/v1/admin/users/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsersById_withAdminJwt_returns200Or404` |
| `GET /api/v1/analytics/item-stats` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `analytics_itemStats_asStudent_returns403` |
| `GET /api/v1/analytics/mastery-trends` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `analytics_masteryTrends_asStudentViewingAnotherStudent_notForbidden` |
| `GET /api/v1/analytics/weak-knowledge-points` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `analytics_weakKnowledgePoints_withFacultyJwt_reachesRouteAndIsAuthorized` |
| `GET /api/v1/analytics/wrong-answers` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `analyticsWrongAnswers_anonymous_returns401` |
| `GET /api/v1/courses` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesList_withStudentJwt_returnsPage` |
| `GET /api/v1/courses/{var}/activities` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesActivities_publicCourse_returnsArray` |
| `GET /api/v1/courses/{var}/assessment-items` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesAssessmentItems_publicCourse_returnsPage` |
| `GET /api/v1/courses/{var}/cohorts` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesCohorts_adminJwt_returns200` |
| `GET /api/v1/courses/{var}/knowledge-points` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesKnowledgePoints_publicCourse_returnsArray` |
| `GET /api/v1/health` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `health_overRealHttp_returnsUpWithVersion` |
| `GET /api/v1/notifications` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notifications_list_withStudentJwt_returnsPageShape` |
| `GET /api/v1/notifications/unread-count` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notifications_unreadCount_returnsUnreadCountField` |
| `GET /api/v1/reports` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsLifecycle_createGetCancelList_overRealHttp` |
| `GET /api/v1/reports/schedules` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsScheduleLifecycle_createUpdateListDelete_overRealHttp` |
| `GET /api/v1/reports/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsScheduleLifecycle_createUpdateListDelete_overRealHttp` |
| `GET /api/v1/reports/{var}/download` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsLifecycle_createGetCancelList_overRealHttp` |
| `GET /api/v1/sessions` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsList_asStudent_scopedToOwnSessions` |
| `GET /api/v1/sessions/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsGet_crossStudent_returnsForbiddenOrNotFound` |
| `GET /api/v1/sessions/{var}/attempt-drafts` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsAttemptDrafts_foreignStudent_returns403` |
| `GET /api/v1/users/me` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `usersMe_withGarbageJwt_returns401` |
| `PATCH /api/v1/admin/users/{var}/status` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsersPatchStatus_invalidValue_returns400` |
| `PATCH /api/v1/sessions/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsLifecycle_createPausePatchCompleteGet_assertsEachTransition` |
| `PATCH /api/v1/sessions/{var}/sets/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsSetsLifecycle_createPatchOverRealHttp` |
| `POST /api/v1/admin/allowed-ip-ranges` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `allowedIpRanges_create_emptyCidr_returns400` |
| `POST /api/v1/admin/anomalies/{var}/resolve` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminAnomalies_resolve_knownId_withAdminJwt_returns200` |
| `POST /api/v1/admin/approvals/{var}/approve` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminApprovals_approve_knownPendingId_withAdminJwt_returns200` |
| `POST /api/v1/admin/approvals/{var}/reject` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `approvalsReject_withStudentJwt_returns403` |
| `POST /api/v1/admin/backups/recovery-drill` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `backupsRecoveryDrill_withNoBackup_returns409or202` |
| `POST /api/v1/admin/backups/run` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminBackups_run_withAdminJwt_triggersBackupOrAccepts409` |
| `POST /api/v1/admin/recycle-bin/{var}/{var}/restore` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `recycleBinRestore_unknownId_returns404` |
| `POST /api/v1/admin/users/{var}/approve` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsersApprove_unknownId_withAdminJwt_returns4xx` |
| `POST /api/v1/admin/users/{var}/reject` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsersReject_emptyReason_withAdminJwt_returns400` |
| `POST /api/v1/admin/users/{var}/unlock` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `adminUsersUnlock_withAdminJwt_idempotentlyReturns2xx` |
| `POST /api/v1/assessment-items` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `courseAuthoring_endToEnd_createUpdateDelete` |
| `POST /api/v1/auth/login` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `authLogin_withSeededAdmin_returnsAccessAndRefreshTokens` |
| `POST /api/v1/auth/logout` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `authLogout_withValidRefreshToken_returns204` |
| `POST /api/v1/auth/refresh` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `authRefresh_withValidRefreshToken_returnsNewAccessToken` |
| `POST /api/v1/auth/register` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `authRegister_newStudent_returnsPendingResponse` |
| `POST /api/v1/courses` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `courseAuthoring_endToEnd_createUpdateDelete` |
| `POST /api/v1/courses/{var}/activities` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesActivitiesPost_studentJwt_returns403` |
| `POST /api/v1/courses/{var}/knowledge-points` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesKnowledgePointsPost_studentJwt_returns403` |
| `POST /api/v1/notifications/read-all` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notifications_markAllRead_returns204` |
| `POST /api/v1/notifications/{var}/read` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notificationsRead_anonymous_returns401` |
| `POST /api/v1/reports` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsCreate_asStudent_returns403` |
| `POST /api/v1/reports/schedules` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsScheduleLifecycle_createUpdateListDelete_overRealHttp` |
| `POST /api/v1/reports/{var}/cancel` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsLifecycle_createGetCancelList_overRealHttp` |
| `POST /api/v1/sessions` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsAttemptDrafts_foreignStudent_returns403` |
| `POST /api/v1/sessions/attempt-drafts` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsAttemptDrafts_foreignStudent_returns403` |
| `POST /api/v1/sessions/sync` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsSync_asStudent_returnsAppliedAndConflictsEnvelope` |
| `POST /api/v1/sessions/{var}/complete` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsLifecycle_createPausePatchCompleteGet_assertsEachTransition` |
| `POST /api/v1/sessions/{var}/continue` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsLifecycle_createPausePatchCompleteGet_assertsEachTransition` |
| `POST /api/v1/sessions/{var}/pause` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsLifecycle_createPausePatchCompleteGet_assertsEachTransition` |
| `POST /api/v1/sessions/{var}/sets` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsSetsLifecycle_createPatchOverRealHttp` |
| `POST /api/v1/sessions/{var}/submit-attempts` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `sessionsAttemptDrafts_fullCrudOverRealHttp` |
| `PUT /api/v1/admin/backups/policy` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `backupsPolicy_getAndRoundtrip_overRealHttp` |
| `PUT /api/v1/admin/notification-templates/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `notificationTemplatesUpdate_emptyBody_returns400` |
| `PUT /api/v1/assessment-items/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `courseAuthoring_endToEnd_createUpdateDelete` |
| `PUT /api/v1/courses/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `coursesPut_studentJwt_returns403` |
| `PUT /api/v1/reports/schedules/{var}` | yes | true no-mock HTTP | `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java` | `reportsScheduleLifecycle_createUpdateListDelete_overRealHttp` |

## API Test Classification
1. True No-Mock HTTP
- `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java`
  Evidence: `TestRestTemplate`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, and class comment explicitly states no MockMvc/mocked principals/mocked services.

2. HTTP with Mocking
- 21 files, including:
- `repo/server/src/test/java/com/meridian/AuthApiTest.java`
- `repo/server/src/test/java/com/meridian/SessionLifecycleApiTest.java`
- `repo/server/src/test/java/com/meridian/OrgScopeApiTest.java`
- `repo/server/src/test/java/com/meridian/ClassificationApiTest.java`
  Evidence: `MockMvc` + `@AutoConfigureMockMvc` and widespread `@WithMockUser` usage.

3. Non-HTTP (unit/integration without HTTP)
- 45 files, including:
- `repo/server/src/test/java/com/meridian/auth/AuthServiceTest.java`
- `repo/server/src/test/java/com/meridian/sessions/SessionControllerUnitTest.java`
- `repo/server/src/test/java/com/meridian/governance/AllowedIpRangeControllerUnitTest.java`
- `repo/unit_tests/server/SyncResolverTest.java`

## Mock Detection (Strict)
- Mocked principal / auth context:
  - WHAT: mocked security principal via `@WithMockUser`
  - WHERE: `repo/server/src/test/java/com/meridian/ClassificationApiTest.java`, `repo/server/src/test/java/com/meridian/OrgScopeApiTest.java`, `repo/server/src/test/java/com/meridian/OrgIsolationContentApiTest.java`, `repo/server/src/test/java/com/meridian/ReportApiTest.java`.
- Mockito mocks in unit tests:
  - WHAT: mocked repositories/services (`Mockito.mock`, `@Mock`)
  - WHERE: `repo/server/src/test/java/com/meridian/auth/AuthServiceTest.java`, `repo/server/src/test/java/com/meridian/analytics/AnalyticsServiceTest.java`, `repo/server/src/test/java/com/meridian/reports/runner/ReportRunnerTest.java`.
- Direct controller invocation bypassing HTTP layer:
  - WHAT: direct method calls on controllers with mocked dependencies
  - WHERE: `repo/server/src/test/java/com/meridian/sessions/SessionControllerUnitTest.java`, `repo/server/src/test/java/com/meridian/governance/AllowedIpRangeControllerUnitTest.java`.

## Coverage Summary
- Total endpoints: **76**
- Endpoints with HTTP tests: **76**
- Endpoints with TRUE no-mock tests: **76**
- HTTP coverage: **100.0%**
- True API coverage: **100.0%**

## Unit Test Summary
### Backend Unit Tests
- Present in `repo/server/src/test/java/com/meridian/**` and mirror set in `repo/unit_tests/server/**`.
- Covered module types (evidence):
  - Controllers: `sessions/SessionControllerUnitTest.java`, `governance/AllowedIpRangeControllerUnitTest.java`
  - Services: `auth/AuthServiceTest.java`, `users/AdminUserServiceTest.java`, `approvals/ApprovalServiceTest.java`, `notifications/NotificationServiceTest.java`
  - Repositories/boundaries: `sessions/TrainingSessionRepositoryTest.java`
  - Auth/guards/middleware/filters: `auth/JwtAuthenticationFilterTest.java`, `common/ratelimit/RateLimitFilterTest.java`, `common/idempotency/IdempotencyInterceptorTest.java`, `common/web/RequestIdFilterTest.java`
- Important backend modules not directly unit-tested (class-level):
  - `repo/server/src/main/java/com/meridian/common/security/SecurityConfig.java`
  - `repo/server/src/main/java/com/meridian/common/web/WebMvcConfig.java`
  - `repo/server/src/main/java/com/meridian/common/security/SecurityKeysValidator.java`
  - `repo/server/src/main/java/com/meridian/common/web/GlobalExceptionHandler.java`
  - `repo/server/src/main/java/com/meridian/sessions/SessionMapper.java`

### Frontend Unit Tests (STRICT)
- Frontend test files: **present** in `repo/unit_tests/web/*.spec.ts` (48 files).
- Framework/tools detected (file evidence):
  - Jasmine/Karma style (`describe`, `it`, `expect`) across `repo/unit_tests/web/*.spec.ts`
  - Angular TestBed from `@angular/core/testing` in `repo/unit_tests/web/app.component.spec.ts`, `repo/unit_tests/web/login.component.spec.ts`
  - Angular HTTP testing in `repo/unit_tests/web/login.component.spec.ts` (`provideHttpClientTesting`, `HttpTestingController`)
- Components/modules covered (import evidence from tests):
  - Admin pages, analytics pages, auth pages, reports pages, session pages, shared UI components, guards, interceptors, stores, db/outbox/api services.
- Important frontend components/modules NOT tested: **none found by static import mapping** from `repo/unit_tests/web/*.spec.ts` to `repo/web/src/app/**/*.ts`.
- **Frontend unit tests: PRESENT**

### Cross-Layer Observation
- Backend and frontend both have broad unit + API coverage; testing is not backend-only.

## API Observability Check
- Strong in true no-mock suite: method/path, request payloads, and response-body assertions are explicit in `repo/server/src/test/java/com/meridian/TrueNoMockHttpApiTest.java`.
- Weak spots exist in some MockMvc tests that assert status only (limited response observability), e.g. `repo/server/src/test/java/com/meridian/ClassificationApiTest.java`.

## Tests Check
- Success/failure/auth/validation/role-boundary coverage: strong across API and unit suites.
- Edge/cross-tenant/cross-role checks: present (`OrgScopeApiTest`, `OrgIsolationContentApiTest`, `SensitiveDataApiTest`).
- `run_tests.sh` check: Docker-centric execution confirmed (`docker run`, `docker compose run`).
- Caveat: e2e branch contains conditional `npm ci` inside container (`repo/run_tests.sh`), so zero-runtime-install claim is not absolute for that path.

## End-to-End Expectations (Fullstack)
- FE↔BE E2E tests are present under `repo/e2e_tests/tests/*.spec.ts` (Playwright).

## Test Coverage Score (0–100)
- **93/100**

## Score Rationale
- + Full endpoint inventory covered by HTTP tests and true no-mock HTTP evidence.
- + Strong cross-role/auth/multi-tenant negative-path testing.
- + Frontend unit tests are present and broad.
- - Some MockMvc tests are status-only (reduced observability depth).
- - Some infrastructure/config classes have no direct unit tests.

## Key Gaps
- Weak observability in status-only MockMvc cases (selected API tests).
- No direct unit tests for `SecurityConfig`, `WebMvcConfig`, `SecurityKeysValidator`, `GlobalExceptionHandler`, `SessionMapper`.

## Confidence & Assumptions
- Confidence: **high** for route/test mapping (controller annotations + static call-site extraction).
- Assumption: endpoint surface is limited to Spring `@*Mapping` in `*Controller.java` files under `repo/server/src/main/java/com/meridian`.
- Assumption: `repo/server/src/test/java` is authoritative; `repo/api_tests` and `repo/unit_tests` include mirrors/archives per README.

## Test Coverage Audit Verdict
- **PASS (strong)**

---

# README Audit

## README Presence
- Required file exists: `repo/README.md`.

## Hard Gate Check
- Formatting/readability: **PASS**
- Project type declaration near top: **PASS** (`fullstack`)
- Startup instruction includes `docker-compose up`: **PASS** (`repo/README.md`, Quick Start)
- Access method (URL + port): **PASS** (`https://localhost:8443/`, API URLs/ports documented)
- Verification method: **PASS** (curl API checks + UI smoke steps)
- Environment rules (no local runtime install instructions): **PASS** (README explicitly forbids host runtime installs)
- Demo credentials with roles (auth exists): **PASS** (admin, student1, student2, mentor1, faculty1 all documented)

## High Priority Issues
- None.

## Medium Priority Issues
- README claims no runtime install steps are needed in user workflows, but the `--e2e` path in `repo/run_tests.sh` includes a conditional `npm ci` inside container. This is Docker-contained, but still a runtime dependency installation path.

## Low Priority Issues
- Minor command-style duplication (`docker-compose` and `docker compose`) increases command surface.

## Hard Gate Failures
- None.

## Engineering Quality Notes
- Tech stack clarity: strong.
- Architecture explanation: adequate and concrete.
- Test workflow guidance: detailed and mostly consistent.
- Security/role clarity: good (credentials and role matrix documented).
- Presentation quality: high.

## README Verdict (PASS / PARTIAL PASS / FAIL)
- **PASS**
