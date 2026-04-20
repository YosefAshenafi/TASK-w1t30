package com.meridian.security.audit;

import com.meridian.common.security.AesAttributeConverter;
import com.meridian.common.web.RequestIdFilter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", length = 40)
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "request_id")
    private String requestId;

    @Convert(converter = AesAttributeConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String details = "{}";

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public static AuditEvent of(UUID actorId, String action, String targetType, String targetId, String details) {
        AuditEvent e = new AuditEvent();
        e.actorId = actorId;
        e.action = action;
        e.targetType = targetType;
        e.targetId = targetId;
        e.details = details;
        // Pick up the current request id (set by RequestIdFilter) so the
        // audit trail can be correlated with application logs.
        String rid = MDC.get(RequestIdFilter.REQUEST_ID_HEADER);
        if (rid != null && !rid.isBlank()) {
            e.requestId = rid;
        }
        return e;
    }
}
