package com.meridian.analytics;

import com.meridian.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @GetMapping("/mastery-trends")
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

        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId,
                enforceLearnerScope(learnerId, auth), locationId, instructorId, courseVersion);
        return ResponseEntity.ok(analyticsService.masteryTrends(filter));
    }

    @GetMapping("/wrong-answers")
    public ResponseEntity<WrongAnswerDistribution> wrongAnswers(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            Authentication auth) {

        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId,
                enforceLearnerScope(learnerId, auth), null, null, null);
        return ResponseEntity.ok(analyticsService.wrongAnswers(filter));
    }

    @GetMapping("/weak-knowledge-points")
    public ResponseEntity<WeakKnowledgePointList> weakKnowledgePoints(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID learnerId,
            Authentication auth) {

        UUID scopedLearnerId = enforceLearnerScope(learnerId, auth);
        UUID orgScopedLearner = enforceOrgScope(scopedLearnerId, auth);
        AnalyticsFilter filter = buildFilter(from, to, courseId, cohortId, orgScopedLearner, null, null, null);
        return ResponseEntity.ok(analyticsService.weakKnowledgePoints(filter));
    }

    @GetMapping("/item-stats")
    public ResponseEntity<ItemStatsList> itemStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID courseId,
            Authentication auth) {

        AnalyticsFilter filter = buildFilter(from, to, courseId, null,
                enforceLearnerScope(null, auth), null, null, null);
        return ResponseEntity.ok(analyticsService.itemStats(filter));
    }

    private UUID enforceLearnerScope(UUID requested, Authentication auth) {
        String role = extractRole(auth);
        if ("STUDENT".equals(role)) {
            return UUID.fromString(auth.getName());
        }
        return requested;
    }

    private UUID enforceOrgScope(UUID learnerId, Authentication auth) {
        String role = extractRole(auth);
        if ("CORPORATE_MENTOR".equals(role)) {
            return userRepository.findById(UUID.fromString(auth.getName()))
                    .map(u -> u.getOrganizationId() != null ? learnerId : learnerId)
                    .orElse(learnerId);
        }
        return learnerId;
    }

    private AnalyticsFilter buildFilter(String from, String to, UUID courseId, UUID cohortId,
                                         UUID learnerId, UUID locationId, UUID instructorId,
                                         String courseVersion) {
        return new AnalyticsFilter(
                from != null ? Instant.parse(from) : null,
                to != null ? Instant.parse(to) : null,
                locationId, instructorId, courseId, courseVersion, cohortId, learnerId);
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT");
    }
}
