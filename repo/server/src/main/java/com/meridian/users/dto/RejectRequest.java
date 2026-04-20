package com.meridian.users.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String reason) {}
