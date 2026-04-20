package com.meridian.backups.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_runs")
@Getter
@Setter
@NoArgsConstructor
public class BackupRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256")
    private String checksumSha256;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
