package com.meridian.reports.dto;

import com.meridian.reports.entity.ReportSchedule;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record ReportScheduleDto(
        UUID id,
        String type,
        String cronExpr,
        UUID ownerId,
        boolean enabled,
        Instant nextRunAt,
        Instant createdAt
) {
    public static ReportScheduleDto from(ReportSchedule s) {
        return new ReportScheduleDto(s.getId(), s.getType(), s.getCronExpr(),
                s.getOwnerId(), s.isEnabled(), s.getNextRunAt(), s.getCreatedAt());
    }
}
