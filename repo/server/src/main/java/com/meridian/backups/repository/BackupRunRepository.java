package com.meridian.backups.repository;

import com.meridian.backups.entity.BackupRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackupRunRepository extends JpaRepository<BackupRun, UUID> {
    Optional<BackupRun> findFirstByStatusOrderByStartedAtDesc(String status);
    List<BackupRun> findByStartedAtNotNullAndStartedAtBefore(Instant cutoff);
}
