package com.meridian.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String username,
        String displayName,
        String role,
        String status,
        UUID organizationId,
        List<String> allowedIpRanges,
        Instant lastLoginAt,
        Instant createdAt
) {}
