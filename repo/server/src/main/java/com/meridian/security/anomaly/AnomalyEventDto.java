package com.meridian.security.anomaly;

import com.meridian.security.entity.AnomalyEvent;

import java.time.Instant;
import java.util.UUID;

public record AnomalyEventDto(UUID id, UUID userId, String type, String ipAddress,
                               String details, Instant resolvedAt, Instant createdAt) {
    public static AnomalyEventDto from(AnomalyEvent e) {
        return new AnomalyEventDto(e.getId(), e.getUserId(), e.getType(), e.getIpAddress(),
                e.getDetails(), e.getResolvedAt(), e.getCreatedAt());
    }
}
