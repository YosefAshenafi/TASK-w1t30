package com.meridian.reports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateScheduleRequest(
        @NotBlank @Pattern(regexp = "ENROLLMENTS|SEAT_UTILIZATION|REFUND_RETURN_RATE|INVENTORY_LEVELS|CERT_EXPIRING")
        String kind,
        @NotBlank String cronExpr,
        @NotBlank @Pattern(regexp = "CSV|PDF|JSON") String format,
        UUID organizationId,
        Integer certExpiringDays
) {}
