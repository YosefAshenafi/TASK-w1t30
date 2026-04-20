package com.meridian.notifications.repository;

import com.meridian.notifications.entity.InAppNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    @Query("SELECT n FROM InAppNotification n WHERE n.userId = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    List<InAppNotification> findUnreadByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM InAppNotification n WHERE n.userId = :userId AND n.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(n) FROM InAppNotification n WHERE n.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
