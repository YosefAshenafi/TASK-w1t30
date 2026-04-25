package com.meridian.analytics;

import com.meridian.auth.repository.UserRepository;
import com.meridian.common.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/mastery-trends")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR','CORPORATE_MENTOR','STUDENT')")
    public ResponseEntity<MasteryTrendSeries> masteryTrends(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) String courseVersion,
            Authentication auth) {

        UUID scopedLearnerId = enforceLearnerScope(learnerId, auth);
        enforceOrgScope(scopedLearnerId, auth);
        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId, scopedLearnerId,
                locationId, instructorId, courseVersion, extractOrgFilter(scopedLearnerId, auth));
        return ResponseEntity.ok(analyticsService.masteryTrends(filter));
    }

    @GetMapping("/wrong-answers")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR','CORPORATE_MENTOR','STUDENT')")
    public ResponseEntity<WrongAnswerDistribution> wrongAnswers(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) String courseVersion,
            Authentication auth) {

        UUID scopedLearnerId = enforceLearnerScope(learnerId, auth);
        enforceOrgScope(scopedLearnerId, auth);
        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId, scopedLearnerId,
                locationId, instructorId, courseVersion, extractOrgFilter(scopedLearnerId, auth));
        return ResponseEntity.ok(analyticsService.wrongAnswers(filter));
    }

    @GetMapping("/weak-knowledge-points")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR','CORPORATE_MENTOR','STUDENT')")
    public ResponseEntity<WeakKnowledgePointList> weakKnowledgePoints(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) String courseVersion,
            Authentication auth) {

        UUID scopedLearnerId = enforceLearnerScope(learnerId, auth);
        enforceOrgScope(scopedLearnerId, auth);
        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId, scopedLearnerId,
                locationId, instructorId, courseVersion, extractOrgFilter(scopedLearnerId, auth));
        return ResponseEntity.ok(analyticsService.weakKnowledgePoints(filter));
    }

    @GetMapping("/item-stats")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR','CORPORATE_MENTOR')")
    public ResponseEntity<ItemStatsList> itemStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) String courseVersion,
            Authentication auth) {

        AuthPrincipal principal = AuthPrincipal.of(auth);
        UUID orgFilter = "CORPORATE_MENTOR".equals(principal.role())
                ? requireOrgScope(principal)
                : null;
        enforceOrgScope(learnerId, auth);
        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId, learnerId,
                locationId, instructorId, courseVersion, orgFilter);
        return ResponseEntity.ok(analyticsService.itemStats(filter));
    }

    private UUID enforceLearnerScope(UUID requested, Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        if ("STUDENT".equals(principal.role())) {
            return principal.userId();
        }
        return requested;
    }

    private void enforceOrgScope(UUID learnerId, Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        if ("CORPORATE_MENTOR".equals(principal.role())) {
            UUID orgId = resolveMentorOrg(principal);
            if (learnerId != null) {
                boolean inOrg = userRepository.findById(learnerId)
                        .map(u -> orgId.equals(u.getOrganizationId()))
                        .orElse(false);
                if (!inOrg) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: learner not in your org");
                }
            }
        }
    }

    /**
     * Returns the corporate mentor's organization id. Prefers the value already on the
     * principal; falls back to a user-repo lookup for cases where the security context
     * carries only a username (e.g. {@code @WithMockUser} test setups, or filters that
     * have not yet enriched the principal). Throws 403 if no org can be resolved.
     */
    private UUID resolveMentorOrg(AuthPrincipal principal) {
        UUID orgId = principal.organizationId();
        if (orgId == null) {
            orgId = userRepository.findById(principal.userId())
                    .map(u -> u.getOrganizationId())
                    .orElse(null);
        }
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: no org scope");
        }
        return orgId;
    }

    private UUID requireOrgScope(AuthPrincipal principal) {
        return resolveMentorOrg(principal);
    }

    private UUID extractOrgFilter(UUID learnerId, Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        if ("CORPORATE_MENTOR".equals(principal.role())) {
            return resolveMentorOrg(principal);
        }
        return null;
    }

    private AnalyticsFilter buildFilter(String from, String to, UUID courseId, UUID cohortId,
                                         UUID learnerId, UUID locationId, UUID instructorId,
                                         String courseVersion, UUID organizationId) {
        return new AnalyticsFilter(
                from != null ? Instant.parse(from) : null,
                to != null ? Instant.parse(to) : null,
                locationId, instructorId, courseId, courseVersion, cohortId, learnerId, organizationId);
    }
}
