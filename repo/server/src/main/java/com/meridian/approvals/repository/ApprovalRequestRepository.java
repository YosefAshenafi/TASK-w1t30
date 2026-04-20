package com.meridian.approvals.repository;

import com.meridian.approvals.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    @Query("SELECT a FROM ApprovalRequest a WHERE (:status IS NULL OR a.status = :status)")
    Page<ApprovalRequest> findByStatusFilter(@Param("status") String status, Pageable pageable);

    @Modifying
    @Query("UPDATE ApprovalRequest a SET a.status = 'EXPIRED' WHERE a.status = 'PENDING' AND a.expiresAt < :now")
    int expireOldRequests(@Param("now") Instant now);
}
