package com.meridian.sessions.repository;

import com.meridian.sessions.entity.TrainingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
           SELECT s FROM TrainingSession s
           WHERE s.deletedAt IS NULL
             AND (:studentId IS NULL OR s.studentId = :studentId)
             AND (:from IS NULL OR s.startedAt >= :from)
             AND (:to IS NULL OR s.startedAt <= :to)
           """)
    Page<TrainingSession> findFiltered(@Param("studentId") UUID studentId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       Pageable pageable);
}
