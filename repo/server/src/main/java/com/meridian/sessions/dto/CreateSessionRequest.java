package com.meridian.sessions.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record CreateSessionRequest(
        @NotNull UUID id,
        @NotNull UUID courseId,
        UUID cohortId,
        @Min(15) @Max(300) int restSecondsDefault,
        Instant startedAt,
        @NotNull Instant clientUpdatedAt
) {}
