package com.meridian.courses.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseDto(
        UUID id,
        String code,
        String title,
        String version,
        UUID locationId,
        UUID instructorId,
        String classification,
        Instant createdAt
) {}
