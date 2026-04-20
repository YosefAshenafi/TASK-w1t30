package com.meridian.security.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDto(UUID id, UUID actorId, String action, String targetType,
                             String targetId, String ipAddress, String details, Instant occurredAt) {
    public static AuditEventDto from(AuditEvent e) {
        return new AuditEventDto(e.getId(), e.getActorId(), e.getAction(), e.getTargetType(),
                e.getTargetId(), e.getIpAddress(), e.getDetails(), e.getOccurredAt());
    }
}
