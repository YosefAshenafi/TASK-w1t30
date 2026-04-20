package com.meridian.sessions.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionSetDto(
        UUID id,
        UUID sessionId,
        UUID activityId,
        int setIndex,
        int restSeconds,
        Instant completedAt,
        String notes,
        Instant clientUpdatedAt
) {}
