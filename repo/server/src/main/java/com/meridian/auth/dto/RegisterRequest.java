package com.meridian.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64)
        String username,

        @NotBlank
        String password,

        @NotBlank
        String displayName,

        @NotBlank @Pattern(regexp = "STUDENT|CORPORATE_MENTOR|FACULTY_MENTOR|ADMIN")
        String requestedRole,

        String organizationCode
) {}
