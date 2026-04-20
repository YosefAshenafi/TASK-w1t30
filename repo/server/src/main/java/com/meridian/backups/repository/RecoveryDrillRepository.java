package com.meridian.backups.repository;

import com.meridian.backups.entity.RecoveryDrill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecoveryDrillRepository extends JpaRepository<RecoveryDrill, UUID> {
    Page<RecoveryDrill> findAllByOrderByScheduledAtDesc(Pageable pageable);

    Optional<RecoveryDrill> findFirstByStatusOrderByCompletedAtDesc(String status);
}
