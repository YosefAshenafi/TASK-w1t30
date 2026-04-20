package com.meridian.recyclebin;

import com.meridian.auth.entity.User;
import com.meridian.auth.repository.UserRepository;
import com.meridian.courses.entity.Course;
import com.meridian.courses.repository.CourseRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Enforces the recycle-bin retention policy by hard-deleting soft-deleted
 * records once they exceed the configured retention window (defaults to
 * 14 days; set {@code app.recycle-bin.retention-days=0} to disable purging).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecycleBinRetentionScheduler {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditEventRepository auditEventRepository;

    @Value("${app.recycle-bin.retention-days:14}")
    private int retentionDays;

    @Scheduled(initialDelay = 10 * 60_000L, fixedDelay = 6 * 60 * 60_000L)
    @Transactional
    public void purge() {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        List<Course> expiredCourses = courseRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() != null && c.getDeletedAt().isBefore(cutoff))
                .toList();
        for (Course c : expiredCourses) {
            courseRepository.deleteById(c.getId());
            auditEventRepository.save(AuditEvent.of(null, "RECYCLE_BIN_PURGE",
                    "courses", c.getId().toString(),
                    "{\"retentionDays\":" + retentionDays + "}"));
        }

        List<User> expiredUsers = userRepository.findAll().stream()
                .filter(u -> u.getDeletedAt() != null && u.getDeletedAt().isBefore(cutoff))
                .toList();
        for (User u : expiredUsers) {
            userRepository.deleteById(u.getId());
            auditEventRepository.save(AuditEvent.of(null, "RECYCLE_BIN_PURGE",
                    "users", u.getId().toString(),
                    "{\"retentionDays\":" + retentionDays + "}"));
        }

        if (!expiredCourses.isEmpty() || !expiredUsers.isEmpty()) {
            log.info("Recycle bin retention: purged {} courses and {} users older than {} days",
                    expiredCourses.size(), expiredUsers.size(), retentionDays);
        }
    }
}
