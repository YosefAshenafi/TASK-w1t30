package com.meridian.reports.repository;

import com.meridian.reports.entity.ReportRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReportRunRepository extends JpaRepository<ReportRun, UUID> {

    @Query("SELECT r FROM ReportRun r WHERE (:requestedBy IS NULL OR r.requestedBy = :requestedBy) ORDER BY r.queuedAt DESC")
    Page<ReportRun> findByRequester(@Param("requestedBy") UUID requestedBy, Pageable pageable);

    List<ReportRun> findAllByStatus(String status);

    List<ReportRun> findAllByApprovalRequestIdAndStatus(UUID approvalRequestId, String status);
}
