package com.meridian.sessions.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "training_sessions")
@Getter
@Setter
@NoArgsConstructor
public class TrainingSession {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "cohort_id")
    private UUID cohortId;

    @Column(name = "rest_seconds_default", nullable = false)
    private int restSecondsDefault = 60;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "client_updated_at", nullable = false)
    private Instant clientUpdatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;
}
