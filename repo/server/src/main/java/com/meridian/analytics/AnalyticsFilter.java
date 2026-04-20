package com.meridian.analytics;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsFilter(
        Instant from,
        Instant to,
        UUID locationId,
        UUID instructorId,
        UUID courseId,
        String courseVersion,
        UUID cohortId,
        UUID learnerId,
        UUID organizationId
) {
    public AnalyticsFilter(Instant from, Instant to, UUID locationId, UUID instructorId,
                           UUID courseId, String courseVersion, UUID cohortId, UUID learnerId) {
        this(from, to, locationId, instructorId, courseId, courseVersion, cohortId, learnerId, null);
    }
}
