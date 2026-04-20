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
        UUID learnerId
) {}
