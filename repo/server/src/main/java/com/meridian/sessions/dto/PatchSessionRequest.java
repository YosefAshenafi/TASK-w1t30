package com.meridian.sessions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record PatchSessionRequest(
        String status,
        @Min(15) @Max(300) Integer restSecondsDefault,
        Instant endedAt,
        Instant clientUpdatedAt
) {}
