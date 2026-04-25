package com.meridian.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotBlank String version,
        UUID locationId,
        UUID instructorId,
        @Pattern(regexp = "PUBLIC|INTERNAL|CONFIDENTIAL|RESTRICTED")
        String classification
) {}
