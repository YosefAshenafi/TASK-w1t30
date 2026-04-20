package com.meridian.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_events")
@Getter
@Setter
@NoArgsConstructor
public class AnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String details = "{}";

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static AnomalyEvent of(UUID userId, String type, String ipAddress, String details) {
        AnomalyEvent e = new AnomalyEvent();
        e.userId = userId;
        e.type = type;
        e.ipAddress = ipAddress;
        e.details = details;
        return e;
    }
}
