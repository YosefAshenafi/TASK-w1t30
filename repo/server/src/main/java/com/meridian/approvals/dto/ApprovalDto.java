package com.meridian.approvals.dto;

import com.meridian.approvals.entity.ApprovalRequest;

import java.time.Instant;
import java.util.UUID;

public record ApprovalDto(
        UUID id,
        String type,
        String status,
        UUID requestedBy,
        UUID reviewedBy,
        String reason,
        Instant createdAt,
        Instant decidedAt,
        Instant expiresAt
) {
    public static ApprovalDto from(ApprovalRequest a) {
        return new ApprovalDto(a.getId(), a.getType(), a.getStatus(),
                a.getRequestedBy(), a.getReviewedBy(), a.getReason(),
                a.getCreatedAt(), a.getDecidedAt(), a.getExpiresAt());
    }
}
