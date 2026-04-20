package com.meridian.reports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ReportRequest(
        @NotBlank @Pattern(regexp = "ENROLLMENTS|SEAT_UTILIZATION|REFUND_RETURN_RATE|INVENTORY_LEVELS|CERT_EXPIRING")
        String kind,
        @Pattern(regexp = "DAY|WEEK|MONTH|QUARTER|CUSTOM")
        String window,
        String from,
        String to,
        Integer certExpiringDays,
        @NotBlank @Pattern(regexp = "CSV|PDF|JSON")
        String format,
        UUID organizationId,
        @Pattern(regexp = "INTERNAL|CONFIDENTIAL|RESTRICTED")
        String classification
) {}
