package com.meridian.notifications;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.web.PageResponse;
import com.meridian.notifications.dto.NotificationDto;
import com.meridian.notifications.entity.InAppNotification;
import com.meridian.notifications.entity.NotificationTemplate;
import com.meridian.notifications.repository.InAppNotificationRepository;
import com.meridian.notifications.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationRepository notifRepo;
    private final NotificationTemplateRepository templateRepo;
    private final TemplateRenderer renderer;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> list(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        size = Math.min(size, 200);

        List<InAppNotification> all = unread
                ? notifRepo.findUnreadByUserId(userId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                : notifRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        long total = unread ? notifRepo.countUnreadByUserId(userId) : notifRepo.countByUserId(userId);

        List<NotificationDto> items = all.stream().map(n -> toDto(n)).toList();
        return ResponseEntity.ok(new PageResponse<>(items, page, size, total));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, Authentication auth) {
        InAppNotification n = notifRepo.findById(id)
                .filter(notif -> notif.getUserId().equals(UUID.fromString(auth.getName())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setReadAt(Instant.now());
        notifRepo.save(n);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        List<InAppNotification> unread = notifRepo.findUnreadByUserId(userId,
                PageRequest.of(0, 1000, Sort.by("createdAt").descending()));
        Instant now = Instant.now();
        unread.forEach(n -> n.setReadAt(now));
        notifRepo.saveAll(unread);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        long count = notifRepo.countUnreadByUserId(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    private NotificationDto toDto(InAppNotification n) {
        Map<String, String> vars = parsePayloadVars(n.getPayload());
        NotificationTemplate tmpl = templateRepo.findById(n.getTemplateKey()).orElse(null);
        String subject = tmpl != null ? renderer.render(tmpl.getTitleTmpl(), vars) : n.getTemplateKey();
        String bodyHtml = tmpl != null ? renderer.renderToHtml(tmpl.getBodyTmpl(), vars) : "";
        String severity = n.getSeverity() != null ? n.getSeverity() : "INFO";
        return new NotificationDto(n.getId(), n.getUserId(), n.getTemplateKey(),
                n.getPayload(),
                new NotificationDto.RenderedContent(subject, bodyHtml),
                severity, n.getReadAt(), n.getCreatedAt());
    }

    private Map<String, String> parsePayloadVars(String payload) {
        Map<String, String> vars = new HashMap<>();
        if (payload == null || payload.isBlank()) return vars;
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.isObject()) {
                node.fields().forEachRemaining(e -> {
                    JsonNode v = e.getValue();
                    if (v.isValueNode()) {
                        vars.put(e.getKey(), v.asText());
                    } else if (!v.isNull()) {
                        vars.put(e.getKey(), v.toString());
                    }
                });
            }
        } catch (Exception e) {
            log.debug("Could not parse notification payload as JSON object: {}", e.getMessage());
        }
        return vars;
    }
}
