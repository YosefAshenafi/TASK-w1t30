package com.meridian.security.repository;

import com.meridian.security.entity.AnomalyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID> {

    Page<AnomalyEvent> findByResolvedAtIsNull(Pageable pageable);

    Page<AnomalyEvent> findAll(Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) > 0 FROM anomaly_events
            WHERE user_id = :userId AND type = :type
              AND created_at >= NOW() - INTERVAL '10 minutes'
            """, nativeQuery = true)
    boolean existsRecentByUserIdAndType(@Param("userId") UUID userId, @Param("type") String type);
}
