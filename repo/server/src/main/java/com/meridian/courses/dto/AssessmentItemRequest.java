package com.meridian.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AssessmentItemRequest(
        @NotNull UUID courseId,
        UUID knowledgePointId,
        @NotBlank @Pattern(regexp = "SINGLE|MULTI|SHORT|CODE") String type,
        @NotBlank String stem,
        Object choices
) {}
