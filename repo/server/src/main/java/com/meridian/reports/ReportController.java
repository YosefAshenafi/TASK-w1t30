package com.meridian.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.approvals.entity.ApprovalRequest;
import com.meridian.approvals.repository.ApprovalRequestRepository;
import com.meridian.common.web.PageResponse;
import com.meridian.notifications.NotificationService;
import com.meridian.auth.repository.UserRepository;
import com.meridian.reports.dto.*;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.entity.ReportSchedule;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.reports.repository.ReportScheduleRepository;
import com.meridian.reports.runner.ReportRunner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final int APPROVAL_THRESHOLD = 10_000;

    private final ReportRunRepository runRepository;
    private final ReportScheduleRepository scheduleRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ReportRunner reportRunner;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ReportRunDto> create(@Valid @RequestBody ReportRequest req,
                                               Authentication auth) throws Exception {
        UUID userId = UUID.fromString(auth.getName());

        boolean needsApproval = "RESTRICTED".equals(req.kind());

        String params = objectMapper.writeValueAsString(req);

        ReportRun run = new ReportRun();
        run.setType(req.kind());
        run.setParameters(params);
        run.setRequestedBy(userId);
        run.setOrganizationId(req.organizationId());
        run.setClassification("INTERNAL");

        if (needsApproval) {
            ApprovalRequest ar = ApprovalRequest.create("EXPORT", params, userId);
            ar = approvalRepository.save(ar);
            run.setStatus("NEEDS_APPROVAL");
            run.setApprovalRequestId(ar.getId());
            run = runRepository.save(run);

            List<UUID> adminIds = userRepository.findActiveAdmins().stream()
                    .map(u -> u.getId()).toList();
            notificationService.sendToAll(adminIds, "approval.requested",
                    "{\"type\":\"EXPORT\",\"runId\":\"" + run.getId() + "\"}");
        } else {
            run = runRepository.save(run);
            reportRunner.execute(run);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReportRunDto.from(run));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportRunDto> get(@PathVariable UUID id) {
        ReportRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"));
        return ResponseEntity.ok(ReportRunDto.from(run));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReportRunDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        String role = extractRole(auth);
        UUID userId = "ADMIN".equals(role) ? null : UUID.fromString(auth.getName());
        Page<ReportRun> result = runRepository.findByRequester(userId,
                PageRequest.of(page, Math.min(size, 200), Sort.by("queuedAt").descending()));
        return ResponseEntity.ok(PageResponse.from(result.map(ReportRunDto::from)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, Authentication auth) {
        ReportRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"));
        if (List.of("QUEUED", "NEEDS_APPROVAL").contains(run.getStatus())) {
            run.setStatus("CANCELLED");
            runRepository.save(run);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules")
    public ResponseEntity<PageResponse<ReportScheduleDto>> listSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Page<ReportSchedule> result = scheduleRepository.findAllByOwnerId(userId,
                PageRequest.of(page, Math.min(size, 200)));
        return ResponseEntity.ok(PageResponse.from(result.map(ReportScheduleDto::from)));
    }

    @PostMapping("/schedules")
    public ResponseEntity<ReportScheduleDto> createSchedule(@Valid @RequestBody CreateScheduleRequest req,
                                                            Authentication auth) throws Exception {
        UUID userId = UUID.fromString(auth.getName());
        ReportSchedule s = new ReportSchedule();
        s.setType(req.kind());
        s.setParameters(objectMapper.writeValueAsString(req));
        s.setCronExpr(req.cronExpr());
        s.setOwnerId(userId);
        s.setNextRunAt(Instant.now().plus(1, ChronoUnit.MINUTES));
        s = scheduleRepository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportScheduleDto.from(s));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id, Authentication auth) {
        ReportSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        if (!s.getOwnerId().equals(UUID.fromString(auth.getName())) && !"ADMIN".equals(extractRole(auth))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        scheduleRepository.delete(s);
        return ResponseEntity.noContent().build();
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT");
    }
}
