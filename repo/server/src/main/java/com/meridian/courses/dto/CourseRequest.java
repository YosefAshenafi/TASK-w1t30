package com.meridian.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotBlank @Pattern(regexp = "\\d{4}\\.\\d+", message = "Version must match pattern YYYY.N (e.g. 2024.1)")
        String version,
        UUID locationId,
        UUID instructorId,
        @Pattern(regexp = "PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED")
        String classification
) {}
