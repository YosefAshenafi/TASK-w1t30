package com.meridian.users;

import com.meridian.approvals.dto.ApprovalDto;
import com.meridian.approvals.entity.ApprovalRequest;
import com.meridian.common.security.AuthPrincipal;
import com.meridian.common.web.PageResponse;
import com.meridian.users.dto.RejectRequest;
import com.meridian.users.dto.StatusUpdateRequest;
import com.meridian.users.dto.UserSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PageResponse<UserSummaryDto>> listUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String orgCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {
        List<UserSummaryDto> users = adminUserService.listUsers(status, role, orgId, orgCode,
                AuthPrincipal.of(auth));
        size = Math.min(Math.max(size, 1), 200);
        int from = Math.max(page, 0) * size;
        int to = Math.min(from + size, users.size());
        List<UserSummaryDto> window = from >= users.size() ? List.of() : users.subList(from, to);
        return ResponseEntity.ok(new PageResponse<>(window, page, size, users.size()));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID id, Authentication auth) {
        adminUserService.approve(id, UUID.fromString(auth.getName()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                       @Valid @RequestBody RejectRequest req,
                                       Authentication auth) {
        adminUserService.reject(id, UUID.fromString(auth.getName()), req.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSummaryDto> getById(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(adminUserService.getById(id, AuthPrincipal.of(auth)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApprovalDto> requestStatusChange(@PathVariable UUID id,
                                                           @Valid @RequestBody StatusUpdateRequest req,
                                                           Authentication auth) {
        ApprovalRequest ar = adminUserService.requestStatusChange(id, req.status(),
                UUID.fromString(auth.getName()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApprovalDto.from(ar));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable UUID id, Authentication auth) {
        adminUserService.unlock(id, UUID.fromString(auth.getName()));
        return ResponseEntity.noContent().build();
    }
}
