package com.meridian.sessions.dto;

import java.time.Instant;
import java.util.UUID;

public record TrainingSessionDto(
        UUID id,
        UUID studentId,
        UUID courseId,
        UUID cohortId,
        int restSecondsDefault,
        String status,
        Instant startedAt,
        Instant endedAt,
        Instant clientUpdatedAt
) {}
