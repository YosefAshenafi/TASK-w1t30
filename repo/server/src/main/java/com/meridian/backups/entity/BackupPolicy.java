package com.meridian.backups.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "backup_policy")
@Getter
@Setter
@NoArgsConstructor
public class BackupPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays = 30;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean scheduleEnabled = true;

    @Column(name = "schedule_cron", nullable = false, length = 100)
    private String scheduleCron = "0 0 2 * * *";

    @Column(name = "backup_path", length = 500)
    private String backupPath;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
