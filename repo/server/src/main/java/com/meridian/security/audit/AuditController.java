package com.meridian.security.audit;

import com.meridian.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    @GetMapping
    public ResponseEntity<PageResponse<AuditEventDto>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<AuditEvent> spec = Specification.where(null);
        if (action != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (actorId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("actorId"), actorId));
        }
        if (from != null) {
            Instant fromInstant = Instant.parse(from);
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), fromInstant));
        }
        if (to != null) {
            Instant toInstant = Instant.parse(to);
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), toInstant));
        }

        Page<AuditEvent> result = auditEventRepository.findAll(spec,
                PageRequest.of(page, Math.min(size, 200), Sort.by("occurredAt").descending()));
        return ResponseEntity.ok(PageResponse.from(result.map(AuditEventDto::from)));
    }
}
