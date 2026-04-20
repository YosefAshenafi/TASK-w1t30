package com.meridian.auth;

import com.meridian.auth.entity.User;
import com.meridian.auth.repository.UserRepository;
import com.meridian.notifications.NotificationService;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Watches PENDING user registrations and escalates to admins once they exceed
 * the configured SLA (default 2 business days, weekends excluded).
 * One escalation notification per user per SLA breach.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationSlaScheduler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditEventRepository auditEventRepository;

    @Value("${app.registration.sla-business-days:2}")
    private long slaBusinessDays;

    // In-memory record of users we have already escalated so we don't notify
    // admins repeatedly. Cleared on restart which is acceptable — a repeat
    // notification once per restart is not a regression.
    private final Set<UUID> escalated = new HashSet<>();

    @Scheduled(initialDelay = 5 * 60_000L, fixedDelay = 60 * 60_000L)
    @Transactional
    public void escalateOverduePending() {
        if (slaBusinessDays <= 0) return;
        Instant now = Instant.now();
        List<User> overdue = userRepository.findAllByStatus("PENDING").stream()
                .filter(u -> u.getDeletedAt() == null)
                .filter(u -> u.getCreatedAt() != null
                        && businessDaysBetween(u.getCreatedAt(), now) >= slaBusinessDays)
                .filter(u -> escalated.add(u.getId()))
                .toList();
        if (overdue.isEmpty()) return;

        List<UUID> admins = userRepository.findActiveAdmins().stream()
                .map(User::getId).toList();

        for (User u : overdue) {
            long days = businessDaysBetween(u.getCreatedAt(), now);
            String payload = "{\"username\":\"" + safeJson(u.getUsername()) + "\","
                    + "\"userId\":\"" + u.getId() + "\","
                    + "\"businessDays\":" + days + "}";
            if (!admins.isEmpty()) {
                notificationService.sendToAll(admins, "registration.slaOverdue", payload);
            }
            auditEventRepository.save(AuditEvent.of(null, "REGISTRATION_SLA_OVERDUE",
                    "USER", u.getId().toString(), payload));
            log.warn("Registration SLA overdue: user={} pendingForBusinessDays={}", u.getId(), days);
        }
    }

    private long businessDaysBetween(Instant start, Instant end) {
        LocalDate startDate = start.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = end.atZone(ZoneOffset.UTC).toLocalDate();
        if (!endDate.isAfter(startDate)) return 0;
        long count = 0;
        LocalDate current = startDate;
        while (current.isBefore(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    private static String safeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
