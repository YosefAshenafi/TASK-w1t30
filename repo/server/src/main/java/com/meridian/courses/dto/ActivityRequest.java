package com.meridian.courses.dto;

import jakarta.validation.constraints.NotBlank;

public record ActivityRequest(@NotBlank String name, String description, int sortOrder) {}
