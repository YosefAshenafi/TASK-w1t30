package com.meridian.reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.approvals.entity.ApprovalRequest;
import com.meridian.approvals.repository.ApprovalRequestRepository;
import com.meridian.common.security.AuthPrincipal;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
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

    // When true, admins must also go through the approval workflow for exports.
    // Default is false (admins bypass) to avoid self-approval loops; operators
    // in regulated environments should set this to true so all exports are
    // governed consistently.
    @Value("${app.exports.admin-approval-required:true}")
    private boolean adminApprovalRequired;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CORPORATE_MENTOR','FACULTY_MENTOR')")
    public ResponseEntity<ReportRunDto> create(@Valid @RequestBody ReportRequest req,
                                               Authentication auth) throws Exception {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        UUID userId = principal.userId();

        // Data exports require approval by default. Admins bypass to avoid
        // self-approval loops unless regulated mode has been enabled via
        // app.exports.admin-approval-required, in which case they too must
        // go through the approval workflow.
        boolean needsApproval = adminApprovalRequired || !"ADMIN".equals(principal.role());

        UUID effectiveOrgId = resolveReportOrgId(principal, req.organizationId());

        String params = objectMapper.writeValueAsString(req);

        ReportRun run = new ReportRun();
        run.setType(req.kind());
        run.setParameters(params);
        run.setRequestedBy(userId);
        run.setOrganizationId(effectiveOrgId);
        run.setClassification(req.classification() != null ? req.classification() : "INTERNAL");

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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportRunDto> get(@PathVariable UUID id, Authentication auth) {
        ReportRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"));
        requireOwnerOrAdmin(run, auth);
        return ResponseEntity.ok(ReportRunDto.from(run));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable UUID id, Authentication auth) {
        ReportRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"));
        requireOwnerOrAdmin(run, auth);
        if (!"SUCCEEDED".equals(run.getStatus()) || run.getFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report not available for download");
        }
        Path path = Path.of(run.getFilePath());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report file not found");
        }
        Resource resource = new FileSystemResource(path);
        String filename = path.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CORPORATE_MENTOR','FACULTY_MENTOR')")
    public ResponseEntity<PageResponse<ReportRunDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        UUID userId = "ADMIN".equals(principal.role()) ? null : principal.userId();
        Page<ReportRun> result = runRepository.findByRequester(userId,
                PageRequest.of(page, Math.min(size, 200), Sort.by("queuedAt").descending()));
        return ResponseEntity.ok(PageResponse.from(result.map(ReportRunDto::from)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable UUID id, Authentication auth) {
        ReportRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"));
        requireOwnerOrAdmin(run, auth);
        if (List.of("QUEUED", "NEEDS_APPROVAL").contains(run.getStatus())) {
            run.setStatus("CANCELLED");
            runRepository.save(run);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','CORPORATE_MENTOR','FACULTY_MENTOR')")
    public ResponseEntity<PageResponse<ReportScheduleDto>> listSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID userId = AuthPrincipal.userId(auth);
        Page<ReportSchedule> result = scheduleRepository.findAllByOwnerId(userId,
                PageRequest.of(page, Math.min(size, 200)));
        return ResponseEntity.ok(PageResponse.from(result.map(ReportScheduleDto::from)));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','CORPORATE_MENTOR','FACULTY_MENTOR')")
    public ResponseEntity<ReportScheduleDto> createSchedule(@Valid @RequestBody CreateScheduleRequest req,
                                                            Authentication auth) throws Exception {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        UUID userId = principal.userId();
        UUID effectiveOrgId = resolveReportOrgId(principal, req.organizationId());
        ReportSchedule s = new ReportSchedule();
        s.setType(req.kind());
        s.setParameters(objectMapper.writeValueAsString(req));
        s.setCronExpr(req.cronExpr());
        s.setOwnerId(userId);
        s.setOrganizationId(effectiveOrgId);
        s.setNextRunAt(Instant.now().plus(1, ChronoUnit.MINUTES));
        s = scheduleRepository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportScheduleDto.from(s));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportScheduleDto> updateSchedule(@PathVariable UUID id,
                                                             @RequestBody ReportSchedule updates,
                                                             Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        ReportSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        if (!s.getOwnerId().equals(principal.userId()) && !"ADMIN".equals(principal.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        s.setEnabled(updates.isEnabled());
        if (updates.getCronExpr() != null) s.setCronExpr(updates.getCronExpr());
        s = scheduleRepository.save(s);
        return ResponseEntity.ok(ReportScheduleDto.from(s));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id, Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        ReportSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
        if (!s.getOwnerId().equals(principal.userId()) && !"ADMIN".equals(principal.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        scheduleRepository.delete(s);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(ReportRun run, Authentication auth) {
        AuthPrincipal principal = AuthPrincipal.of(auth);
        if (!"ADMIN".equals(principal.role()) && !run.getRequestedBy().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private UUID resolveReportOrgId(AuthPrincipal principal, UUID requestedOrgId) {
        if ("CORPORATE_MENTOR".equals(principal.role())) {
            if (principal.organizationId() == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Corporate mentor has no organization scope");
            }
            return principal.organizationId();
        }
        return requestedOrgId;
    }

    private String extractRole(Authentication auth) {
        return AuthPrincipal.role(auth);
    }
}
