package com.meridian.courses.dto;

import java.util.UUID;

public record ActivityDto(UUID id, UUID courseId, String name, String description, int sortOrder) {}
