package com.meridian.reports.dto;

import com.meridian.reports.entity.ReportRun;

import java.time.Instant;
import java.util.UUID;

public record ReportRunDto(
        UUID id,
        String kind,
        String status,
        Integer rowCount,
        String outputPath,
        UUID requestedBy,
        UUID approvalRequestId,
        String classification,
        Instant createdAt,
        Instant completedAt
) {
    public static ReportRunDto from(ReportRun r) {
        return new ReportRunDto(r.getId(), r.getType(), r.getStatus(), r.getRowCount(),
                r.getFilePath(), r.getRequestedBy(), r.getApprovalRequestId(),
                r.getClassification(), r.getQueuedAt(), r.getCompletedAt());
    }
}
