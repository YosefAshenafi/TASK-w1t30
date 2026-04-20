package com.meridian.reports.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.entity.ReportSchedule;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.reports.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

                schedule.setLastRunAt(Instant.now());
                schedule.setNextRunAt(Instant.now().plus(1, ChronoUnit.DAYS));
                scheduleRepository.save(schedule);

                reportRunner.execute(run);
            } catch (Exception e) {
                log.error("Failed to trigger scheduled report {}", schedule.getId(), e);
            }
        }
    }
}
