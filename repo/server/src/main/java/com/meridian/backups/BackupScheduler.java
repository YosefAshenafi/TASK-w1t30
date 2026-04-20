package com.meridian.backups;

import com.meridian.auth.repository.UserRepository;
import com.meridian.backups.entity.BackupPolicy;
import com.meridian.backups.entity.BackupRun;
import com.meridian.backups.entity.RecoveryDrill;
import com.meridian.backups.repository.BackupPolicyRepository;
import com.meridian.backups.repository.BackupRunRepository;
import com.meridian.backups.repository.RecoveryDrillRepository;
import com.meridian.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    // Quarterly drills are considered overdue if the last PASSED drill is
    // more than this many days old. 92 ≈ one calendar quarter + a few days
    // of slack to avoid flapping near quarter boundaries.
    private static final long DRILL_INTERVAL_DAYS = 92;

    private final BackupPolicyRepository policyRepository;
    private final BackupRunRepository backupRunRepository;
    private final RecoveryDrillRepository recoveryDrillRepository;
    private final BackupRunner backupRunner;
    private final RecoveryDrillRunner recoveryDrillRunner;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60_000)
    public void runScheduledBackup() {
        BackupPolicy policy = policyRepository.findAll().stream().findFirst().orElse(null);
        if (policy == null || !policy.isScheduleEnabled()) {
            log.debug("Scheduled backup skipped: policy disabled or not configured");
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next = CronExpression.parse(policy.getScheduleCron())
                .next(now.minusMinutes(1));
        if (next == null || next.truncatedTo(ChronoUnit.MINUTES)
                .isAfter(now.truncatedTo(ChronoUnit.MINUTES))) {
            log.debug("Scheduled backup skipped: cron expression does not match current time");
            return;
        }

        String type = (now.getDayOfWeek() == DayOfWeek.SUNDAY) ? "FULL" : "INCREMENTAL";
        log.info("Starting scheduled backup (type={}, policy: retentionDays={})", type, policy.getRetentionDays());
        BackupRun run = new BackupRun();
        run.setType(type);
        backupRunRepository.save(run);
        backupRunner.execute(run);

        enforceRetention(policy.getRetentionDays());
    }

    private void enforceRetention(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<BackupRun> old = backupRunRepository.findByStartedAtNotNullAndStartedAtBefore(cutoff);
        if (!old.isEmpty()) {
            for (BackupRun r : old) {
                if (r.getFilePath() != null) {
                    try {
                        Files.deleteIfExists(Path.of(r.getFilePath()));
                    } catch (Exception ex) {
                        log.warn("Failed to delete backup file {}: {}", r.getFilePath(), ex.getMessage());
                    }
                }
            }
            log.info("Retention enforcement: deleting {} backup run records older than {} days", old.size(), retentionDays);
            backupRunRepository.deleteAll(old);
        }
    }

    // Runs once every 24 hours (at startup + every day thereafter). The
    // scheduler automatically triggers a recovery drill when the previous
    // PASSED drill is older than DRILL_INTERVAL_DAYS, satisfying the
    // quarterly-drill compliance requirement.
    @Scheduled(initialDelay = 15 * 60_000L, fixedDelay = 24 * 60 * 60_000L)
    public void runQuarterlyRecoveryDrill() {
        if (isDrillCurrent()) {
            return;
        }
        BackupRun latest = backupRunRepository.findFirstByStatusOrderByStartedAtDesc("COMPLETED")
                .orElse(null);
        if (latest == null) {
            notifyAdmins("Quarterly recovery drill overdue and no completed backup is available to test");
            return;
        }
        RecoveryDrill drill = new RecoveryDrill();
        drill.setBackupRunId(latest.getId());
        drill.setScheduledAt(Instant.now());
        drill.setNotes("Auto-scheduled quarterly drill");
        recoveryDrillRepository.save(drill);
        log.info("Auto-scheduled quarterly recovery drill {} against backup {}", drill.getId(), latest.getId());
        recoveryDrillRunner.execute(drill, latest);
    }

    private boolean isDrillCurrent() {
        return recoveryDrillRepository.findFirstByStatusOrderByCompletedAtDesc("PASSED")
                .map(d -> d.getCompletedAt() != null
                        && d.getCompletedAt().isAfter(Instant.now().minus(DRILL_INTERVAL_DAYS, ChronoUnit.DAYS)))
                .orElse(false);
    }

    private void notifyAdmins(String message) {
        List<UUID> admins = userRepository.findActiveAdmins().stream()
                .map(u -> u.getId()).toList();
        if (admins.isEmpty()) return;
        String payload = "{\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        notificationService.sendToAll(admins, "backup.drillOverdue", payload);
    }
}
