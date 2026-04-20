package com.meridian.courses.dto;

import java.time.Instant;
import java.util.UUID;

public record CohortDto(UUID id, UUID courseId, String name, int totalSeats, Instant startsAt, Instant endsAt) {}
