package com.meridian.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meridian.approvals.entity.ApprovalRequest;
import com.meridian.approvals.repository.ApprovalRequestRepository;
import com.meridian.auth.entity.User;
import com.meridian.auth.repository.UserRepository;
import com.meridian.common.security.AuthPrincipal;
import com.meridian.governance.MaskingPolicy;
import com.meridian.notifications.NotificationService;
import com.meridian.organizations.repository.OrganizationRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import com.meridian.users.dto.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("PENDING", "ACTIVE", "SUSPENDED", "LOCKED", "DELETED");

    public static final String PERMISSION_CHANGE_TYPE = "PERMISSION_CHANGE";
    public static final String STATUS_UPDATE_ACTION = "STATUS_UPDATE";

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final NotificationService notificationService;
    private final ApprovalRequestRepository approvalRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;
    private final MaskingPolicy maskingPolicy;

    public List<UserSummaryDto> listUsers(String status, AuthPrincipal viewer) {
        return listUsers(status, null, null, null, viewer);
    }

    public List<UserSummaryDto> listUsers(String status) {
        return listUsers(status, null, null, null, null);
    }

    public List<UserSummaryDto> listUsers(String status, String role, String orgId,
                                          String orgCode, AuthPrincipal viewer) {
        UUID resolvedOrgId = resolveOrgFilter(orgId, orgCode);
        return userRepository.findByStatusFilter(status).stream()
                .filter(u -> role == null || role.isBlank() || role.equals(u.getRole()))
                .filter(u -> resolvedOrgId == null || resolvedOrgId.equals(u.getOrganizationId()))
                .map(u -> toSummary(u, viewer))
                .toList();
    }

    private UUID resolveOrgFilter(String orgId, String orgCode) {
        if (orgId != null && !orgId.isBlank()) {
            try {
                return UUID.fromString(orgId);
            } catch (IllegalArgumentException ex) {
                if (orgCode == null || orgCode.isBlank()) {
                    return organizationRepository.findByCode(orgId)
                            .map(org -> org.getId())
                            .orElse(null);
                }
            }
        }
        if (orgCode != null && !orgCode.isBlank()) {
            return organizationRepository.findByCode(orgCode)
                    .map(org -> org.getId())
                    .orElse(null);
        }
        return null;
    }

    @Transactional
    public void approve(UUID targetId, UUID actorId) {
        User user = findActive(targetId);
        if (!"PENDING".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not in PENDING status");
        }
        user.setStatus("ACTIVE");
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.of(
                actorId, "PERMISSION_CHANGE", "USER", targetId.toString(),
                "{\"action\":\"APPROVE\",\"newStatus\":\"ACTIVE\"}"));

        notificationService.send(targetId, "approval.decided",
                "{\"type\":\"ROLE_APPROVAL\",\"decision\":\"APPROVED\"}");
    }

    @Transactional
    public void reject(UUID targetId, UUID actorId, String reason) {
        User user = findActive(targetId);
        if (!"PENDING".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not in PENDING status");
        }
        user.setStatus("SUSPENDED");
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.of(
                actorId, "PERMISSION_CHANGE", "USER", targetId.toString(),
                "{\"action\":\"REJECT\",\"reason\":\"" + reason.replace("\"", "\\\"") + "\"}"));

        notificationService.send(targetId, "approval.decided",
                "{\"type\":\"ROLE_APPROVAL\",\"decision\":\"REJECTED\"}");
    }

    @Transactional
    public void unlock(UUID targetId, UUID actorId) {
        User user = findActive(targetId);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if ("LOCKED".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);

        auditEventRepository.save(AuditEvent.of(
                actorId, "UNLOCK", "USER", targetId.toString(), "{}"));
    }

    public UserSummaryDto getById(UUID id) {
        return toSummary(findActive(id), null);
    }

    public UserSummaryDto getById(UUID id, AuthPrincipal viewer) {
        return toSummary(findActive(id), viewer);
    }

    @Transactional
    public ApprovalRequest requestStatusChange(UUID targetId, String status, UUID actorId) {
        validateStatus(status);
        User user = findActive(targetId);
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("action", STATUS_UPDATE_ACTION);
            payload.put("targetUserId", targetId.toString());
            payload.put("oldStatus", user.getStatus());
            payload.put("newStatus", status);

            ApprovalRequest ar = ApprovalRequest.create(PERMISSION_CHANGE_TYPE,
                    objectMapper.writeValueAsString(payload), actorId);
            ar = approvalRepository.save(ar);

            auditEventRepository.save(AuditEvent.of(actorId, "PERMISSION_CHANGE_REQUESTED",
                    "USER", targetId.toString(),
                    "{\"approvalId\":\"" + ar.getId() + "\",\"newStatus\":\"" + status + "\"}"));

            return ar;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to build permission-change payload", e);
        }
    }

    @Transactional
    public void updateStatus(UUID targetId, String status, UUID actorId) {
        validateStatus(status);
        User user = findActive(targetId);
        String old = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);
        auditEventRepository.save(AuditEvent.of(
                actorId, "PERMISSION_CHANGE", "USER", targetId.toString(),
                "{\"action\":\"STATUS_UPDATE\",\"from\":\"" + old + "\",\"to\":\"" + status + "\"}"));
    }

    @Transactional
    public void applyApprovedStatusChange(String payloadJson, UUID reviewerId) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            JsonNode targetNode = payload.get("targetUserId");
            JsonNode newStatusNode = payload.get("newStatus");
            if (targetNode == null || newStatusNode == null) {
                log.warn("Permission change approval missing fields: {}", payloadJson);
                return;
            }
            UUID targetId = UUID.fromString(targetNode.asText());
            String newStatus = newStatusNode.asText();
            updateStatus(targetId, newStatus, reviewerId);
        } catch (Exception e) {
            log.error("Failed to apply approved permission change: {}", payloadJson, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to apply approved permission change");
        }
    }

    public void validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status; must be one of " + ALLOWED_STATUSES);
        }
    }

    private User findActive(UUID id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserSummaryDto toSummary(User u, AuthPrincipal viewer) {
        boolean canUnmask = viewer == null
                ? true
                : maskingPolicy.canUnmask(viewer.role(), viewer.userId(),
                        viewer.organizationId(), u.getId(), u.getOrganizationId());
        String username = canUnmask ? u.getUsername() : maskingPolicy.maskUsername(u.getUsername());
        String displayName = canUnmask ? u.getDisplayName() : maskingPolicy.maskDisplayName(u.getDisplayName());
        return new UserSummaryDto(u.getId(), username, displayName,
                u.getRole(), u.getStatus(), u.getOrganizationId(), u.getCreatedAt());
    }
}
