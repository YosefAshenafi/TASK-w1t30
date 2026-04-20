package com.meridian.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String status,
        int approvalSlaBusinessDays
) {
    public static RegisterResponse pending(UUID userId) {
        return new RegisterResponse(userId, "PENDING", 2);
    }
}
