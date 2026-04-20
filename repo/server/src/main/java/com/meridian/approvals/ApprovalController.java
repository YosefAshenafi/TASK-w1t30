package com.meridian.approvals;

import com.meridian.approvals.dto.ApprovalDto;
import com.meridian.approvals.repository.ApprovalRequestRepository;
import com.meridian.common.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalRequestRepository approvalRepository;
    private final ApprovalService approvalService;

    @GetMapping
    public ResponseEntity<PageResponse<ApprovalDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = approvalRepository.findByStatusFilter(status, PageRequest.of(page, Math.min(size, 200)));
        return ResponseEntity.ok(PageResponse.from(result.map(ApprovalDto::from)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalDto> approve(@PathVariable UUID id,
                                               @RequestBody(required = false) ReasonRequest req,
                                               Authentication auth) {
        return ResponseEntity.ok(ApprovalDto.from(
                approvalService.approve(id, UUID.fromString(auth.getName()),
                        req != null ? req.reason() : null)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalDto> reject(@PathVariable UUID id,
                                              @Valid @RequestBody ReasonRequest req,
                                              Authentication auth) {
        return ResponseEntity.ok(ApprovalDto.from(
                approvalService.reject(id, UUID.fromString(auth.getName()), req.reason())));
    }

    record ReasonRequest(String reason) {}
}
