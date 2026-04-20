package com.meridian.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.web.PageResponse;
import com.meridian.notifications.dto.TemplateDto;
import com.meridian.notifications.dto.UpdateTemplateRequest;
import com.meridian.notifications.entity.NotificationTemplate;
import com.meridian.notifications.repository.NotificationTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notification-templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TemplateController {

    private final NotificationTemplateRepository templateRepo;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<PageResponse<TemplateDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<NotificationTemplate> all = templateRepo.findAll();
        List<TemplateDto> items = all.stream().map(this::toDto).toList();
        return ResponseEntity.ok(new PageResponse<>(items, page, size, items.size()));
    }

    @PutMapping("/{key}")
    public ResponseEntity<TemplateDto> update(@PathVariable String key,
                                              @Valid @RequestBody UpdateTemplateRequest req,
                                              Authentication auth) {
        NotificationTemplate tmpl = templateRepo.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        tmpl.setTitleTmpl(req.subject());
        tmpl.setBodyTmpl(req.bodyMarkdown());
        try {
            tmpl.setVariables(objectMapper.writeValueAsString(
                    req.variables() != null ? req.variables() : List.of()));
        } catch (Exception e) {
            tmpl.setVariables("[]");
        }
        tmpl.setUpdatedBy(UUID.fromString(auth.getName()));
        tmpl.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toDto(templateRepo.save(tmpl)));
    }

    private TemplateDto toDto(NotificationTemplate t) {
        List<String> vars = List.of();
        try {
            if (t.getVariables() != null) {
                vars = objectMapper.readValue(t.getVariables(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception ignored) {}
        return new TemplateDto(t.getKey(), t.getTitleTmpl(), t.getBodyTmpl(), vars, t.getUpdatedAt());
    }
}
