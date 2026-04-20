package com.meridian.courses.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssessmentItemDto(
        UUID id,
        UUID courseId,
        UUID knowledgePointId,
        String type,
        BigDecimal difficulty,
        BigDecimal discrimination,
        String stem,
        Object choices
) {}
