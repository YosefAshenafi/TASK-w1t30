package com.meridian.security.anomaly;

import com.meridian.common.web.PageResponse;
import com.meridian.security.entity.AnomalyEvent;
import com.meridian.security.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/anomalies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyEventRepository anomalyRepo;

    @GetMapping
    public ResponseEntity<PageResponse<AnomalyEventDto>> list(
            @RequestParam(defaultValue = "false") boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AnomalyEvent> result = resolved
                ? anomalyRepo.findAll(PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending()))
                : anomalyRepo.findByResolvedAtIsNull(PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(result.map(AnomalyEventDto::from)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id) {
        AnomalyEvent event = anomalyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anomaly not found"));
        event.setResolvedAt(Instant.now());
        anomalyRepo.save(event);
        return ResponseEntity.noContent().build();
    }
}
