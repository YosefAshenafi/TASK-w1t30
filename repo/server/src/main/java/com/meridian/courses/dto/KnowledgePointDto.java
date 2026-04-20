package com.meridian.courses.dto;

import java.util.UUID;

public record KnowledgePointDto(UUID id, UUID courseId, String name, String description) {}
