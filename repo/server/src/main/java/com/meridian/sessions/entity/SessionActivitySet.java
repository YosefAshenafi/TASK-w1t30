package com.meridian.sessions.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "session_activity_sets")
@Getter
@Setter
@NoArgsConstructor
public class SessionActivitySet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "set_index", nullable = false)
    private int setIndex;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds = 60;

    @Column(name = "completed_at")
    private Instant completedAt;

    private String notes;

    @Column(name = "client_updated_at", nullable = false)
    private Instant clientUpdatedAt = Instant.now();
}
