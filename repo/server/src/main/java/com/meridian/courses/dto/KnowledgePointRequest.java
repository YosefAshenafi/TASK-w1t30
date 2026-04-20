package com.meridian.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgePointRequest(@NotBlank String name, String description) {}
