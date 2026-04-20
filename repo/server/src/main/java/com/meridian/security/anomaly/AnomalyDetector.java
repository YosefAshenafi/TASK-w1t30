package com.meridian.security.anomaly;

import com.meridian.auth.repository.UserRepository;
import com.meridian.notifications.NotificationService;
import com.meridian.security.entity.AnomalyEvent;
import com.meridian.security.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetector {

    private final JdbcTemplate jdbc;
    private final AnomalyEventRepository anomalyEventRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void detectExportBurst() {
        String sql = """
                SELECT actor_id, ip_address, COUNT(*) AS cnt
                FROM audit_events
                WHERE action = 'EXPORT_ATTEMPT'
                  AND occurred_at >= NOW() - INTERVAL '10 minutes'
                GROUP BY actor_id, ip_address
                HAVING COUNT(*) > 20
                """;

        List<Map<String, Object>> bursts = jdbc.queryForList(sql);
        for (Map<String, Object> row : bursts) {
            UUID userId = row.get("actor_id") != null
                    ? UUID.fromString(row.get("actor_id").toString()) : null;
            String ip = row.get("ip_address") != null ? row.get("ip_address").toString() : null;

            if (userId == null) continue;

            boolean alreadyFlagged = anomalyEventRepository
                    .existsRecentByUserIdAndType(userId, "EXPORT_BURST");

            if (!alreadyFlagged) {
                AnomalyEvent evt = AnomalyEvent.of(userId, "EXPORT_BURST", ip,
                        "{\"count\":" + row.get("cnt") + "}");
                anomalyEventRepository.save(evt);

                String payload = "{\"userId\":\"" + userId + "\",\"count\":" + row.get("cnt") + "}";
                notificationService.send(userId, "anomaly.exportBurst", payload);

                List<UUID> adminIds = userRepository.findActiveAdmins().stream()
                        .map(u -> u.getId()).filter(id -> !id.equals(userId)).toList();
                notificationService.sendToAll(adminIds, "anomaly.exportBurst", payload);

                log.warn("Export burst detected for user={} ip={} count={}", userId, ip, row.get("cnt"));
            }
        }
    }
}
