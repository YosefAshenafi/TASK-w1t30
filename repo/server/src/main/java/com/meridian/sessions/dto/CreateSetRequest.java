package com.meridian.sessions.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record CreateSetRequest(
        @NotNull UUID activityId,
        @Min(1) int setIndex,
        @Min(15) @Max(300) int restSeconds,
        Instant completedAt,
        String notes,
        @NotNull Instant clientUpdatedAt
) {}
