package com.meridian.backups.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_drills")
@Getter
@Setter
@NoArgsConstructor
public class RecoveryDrill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "backup_run_id", nullable = false)
    private UUID backupRunId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    private String notes;

    @Column(name = "conducted_by")
    private UUID conductedBy;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
