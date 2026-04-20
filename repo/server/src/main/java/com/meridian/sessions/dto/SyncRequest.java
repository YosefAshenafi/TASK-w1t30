package com.meridian.sessions.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncRequest(
        @NotNull @Size(max = 100) List<SessionSyncItem> sessions,
        @NotNull @Size(max = 100) List<SetSyncItem> sets
) {
    public record SessionSyncItem(
            @NotBlank String idempotencyKey,
            @NotNull UUID id,
            @NotNull UUID courseId,
            UUID cohortId,
            @Min(15) @Max(300) int restSecondsDefault,
            String status,
            Instant startedAt,
            Instant endedAt,
            @NotNull Instant clientUpdatedAt
    ) {}

    public record SetSyncItem(
            @NotBlank String idempotencyKey,
            @NotNull UUID sessionId,
            @NotNull UUID activityId,
            @Min(1) int setIndex,
            @Min(15) @Max(300) int restSeconds,
            Instant completedAt,
            String notes,
            @NotNull Instant clientUpdatedAt
    ) {}
}
