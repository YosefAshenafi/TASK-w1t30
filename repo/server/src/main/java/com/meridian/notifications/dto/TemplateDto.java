package com.meridian.notifications.dto;

import java.time.Instant;
import java.util.List;

public record TemplateDto(
        String key,
        String subject,
        String bodyMarkdown,
        List<String> variables,
        Instant updatedAt
) {}
