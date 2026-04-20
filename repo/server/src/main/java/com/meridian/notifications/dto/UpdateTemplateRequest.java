package com.meridian.notifications.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateTemplateRequest(
        @NotBlank String subject,
        @NotBlank String bodyMarkdown,
        List<String> variables
) {}
