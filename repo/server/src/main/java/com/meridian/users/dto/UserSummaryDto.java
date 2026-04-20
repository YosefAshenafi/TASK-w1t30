package com.meridian.users.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String username,
        String displayName,
        String role,
        String status,
        UUID organizationId,
        Instant createdAt
) {}
