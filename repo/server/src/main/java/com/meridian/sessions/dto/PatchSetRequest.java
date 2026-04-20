package com.meridian.sessions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record PatchSetRequest(
        @Min(15) @Max(300) Integer restSeconds,
        Instant completedAt,
        String notes,
        Instant clientUpdatedAt
) {}
