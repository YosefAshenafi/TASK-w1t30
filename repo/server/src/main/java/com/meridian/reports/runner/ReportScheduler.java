package com.meridian.reports.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.entity.ReportSchedule;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.reports.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportScheduleRepository scheduleRepository;
    private final ReportRunRepository runRepository;
    private final ReportRunner reportRunner;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void runDueSchedules() {
        List<ReportSchedule> due = scheduleRepository.findDue(Instant.now());
        for (ReportSchedule schedule : due) {
            try {
                ReportRun run = new ReportRun();
                run.setType(schedule.getType());
                run.setParameters(schedule.getParameters());
                run.setRequestedBy(schedule.getOwnerId());
                run.setOrganizationId(schedule.getOrganizationId());
                run = runRepository.save(run);

                Instant now = Instant.now();
                schedule.setLastRunAt(now);
                schedule.setNextRunAt(computeNextRunAt(schedule.getCronExpr(), now));
                scheduleRepository.save(schedule);

                reportRunner.execute(run);
            } catch (Exception e) {
                log.error("Failed to trigger scheduled report {}", schedule.getId(), e);
            }
        }
    }

    Instant computeNextRunAt(String cronExpr, Instant from) {
        if (cronExpr == null || cronExpr.isBlank()) {
            return from.plus(1, ChronoUnit.DAYS);
        }
        try {
            CronExpression expr = CronExpression.parse(cronExpr);
            LocalDateTime base = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
            LocalDateTime next = expr.next(base);
            if (next == null) {
                return from.plus(1, ChronoUnit.DAYS);
            }
            return next.toInstant(ZoneOffset.UTC);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cron expression '{}' on schedule, falling back to +1 day", cronExpr);
            return from.plus(1, ChronoUnit.DAYS);
        }
    }
}
