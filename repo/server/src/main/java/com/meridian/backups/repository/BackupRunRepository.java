package com.meridian.backups.repository;

import com.meridian.backups.entity.BackupRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BackupRunRepository extends JpaRepository<BackupRun, UUID> {
    Optional<BackupRun> findFirstByStatusOrderByStartedAtDesc(String status);
}
