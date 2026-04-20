package com.meridian.sessions.repository;

import com.meridian.sessions.entity.TrainingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findByIdAndDeletedAtIsNull(UUID id);

    @Query(value = """
           SELECT * FROM training_sessions ts
           WHERE ts.deleted_at IS NULL
             AND (CAST(:studentId AS uuid) IS NULL OR ts.student_id = CAST(:studentId AS uuid))
             AND (:status IS NULL OR ts.status = :status)
             AND (CAST(:fromTs AS timestamptz) IS NULL OR ts.started_at >= CAST(:fromTs AS timestamptz))
             AND (CAST(:toTs AS timestamptz) IS NULL OR ts.started_at <= CAST(:toTs AS timestamptz))
           """,
           countQuery = """
           SELECT COUNT(*) FROM training_sessions ts
           WHERE ts.deleted_at IS NULL
             AND (CAST(:studentId AS uuid) IS NULL OR ts.student_id = CAST(:studentId AS uuid))
             AND (:status IS NULL OR ts.status = :status)
             AND (CAST(:fromTs AS timestamptz) IS NULL OR ts.started_at >= CAST(:fromTs AS timestamptz))
             AND (CAST(:toTs AS timestamptz) IS NULL OR ts.started_at <= CAST(:toTs AS timestamptz))
           """,
           nativeQuery = true)
    Page<TrainingSession> findFiltered(@Param("studentId") String studentId,
                                       @Param("status") String status,
                                       @Param("fromTs") String fromTs,
                                       @Param("toTs") String toTs,
                                       Pageable pageable);

    @Query(value = """
           SELECT ts.* FROM training_sessions ts
           WHERE ts.deleted_at IS NULL
             AND ts.student_id IN (
               SELECT u.id FROM users u
               WHERE u.organization_id = CAST(:orgId AS uuid)
                 AND u.deleted_at IS NULL
             )
             AND (CAST(:studentId AS uuid) IS NULL OR ts.student_id = CAST(:studentId AS uuid))
             AND (:status IS NULL OR ts.status = :status)
             AND (CAST(:fromTs AS timestamptz) IS NULL OR ts.started_at >= CAST(:fromTs AS timestamptz))
             AND (CAST(:toTs AS timestamptz) IS NULL OR ts.started_at <= CAST(:toTs AS timestamptz))
           """,
           countQuery = """
           SELECT COUNT(*) FROM training_sessions ts
           WHERE ts.deleted_at IS NULL
             AND ts.student_id IN (
               SELECT u.id FROM users u
               WHERE u.organization_id = CAST(:orgId AS uuid)
                 AND u.deleted_at IS NULL
             )
             AND (CAST(:studentId AS uuid) IS NULL OR ts.student_id = CAST(:studentId AS uuid))
             AND (:status IS NULL OR ts.status = :status)
             AND (CAST(:fromTs AS timestamptz) IS NULL OR ts.started_at >= CAST(:fromTs AS timestamptz))
             AND (CAST(:toTs AS timestamptz) IS NULL OR ts.started_at <= CAST(:toTs AS timestamptz))
           """,
           nativeQuery = true)
    Page<TrainingSession> findFilteredByOrg(@Param("orgId") String orgId,
                                             @Param("studentId") String studentId,
                                             @Param("status") String status,
                                             @Param("fromTs") String fromTs,
                                             @Param("toTs") String toTs,
                                             Pageable pageable);

    @Query("""
           SELECT s FROM TrainingSession s
           WHERE s.deletedAt IS NULL
             AND s.studentId IN (
               SELECT u.id FROM User u WHERE u.organizationId = :orgId AND u.deletedAt IS NULL
             )
           """)
    List<UUID> findStudentIdsByOrg(@Param("orgId") UUID orgId);
}
