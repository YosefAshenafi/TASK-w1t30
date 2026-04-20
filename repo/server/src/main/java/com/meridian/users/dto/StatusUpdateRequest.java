package com.meridian.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "PENDING|ACTIVE|SUSPENDED|LOCKED|DELETED",
                message = "status must be one of PENDING, ACTIVE, SUSPENDED, LOCKED, DELETED")
        String status
) {}
