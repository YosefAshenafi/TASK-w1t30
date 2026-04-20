package com.meridian.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.auth.dto.*;
import com.meridian.auth.entity.RefreshToken;
import com.meridian.auth.entity.User;
import com.meridian.auth.repository.RefreshTokenRepository;
import com.meridian.auth.repository.UserRepository;
import com.meridian.notifications.NotificationService;
import com.meridian.organizations.repository.OrganizationRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventPublisher;
import com.meridian.security.audit.AuditEventRepository;
import com.meridian.security.entity.AnomalyEvent;
import com.meridian.security.repository.AllowedIpRangeRepository;
import com.meridian.security.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long IDLE_TTL_HOURS = 8;
    private static final long ABSOLUTE_TTL_HOURS = 12;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final AllowedIpRangeRepository allowedIpRangeRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        PasswordPolicy.validate(req.password());

        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_TAKEN");
        }

        if ("CORPORATE_MENTOR".equals(req.requestedRole()) &&
                (req.organizationCode() == null || req.organizationCode().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "organizationCode is required for CORPORATE_MENTOR role");
        }

        UUID resolvedOrgId = null;
        if ("CORPORATE_MENTOR".equals(req.requestedRole()) && req.organizationCode() != null && !req.organizationCode().isBlank()) {
            resolvedOrgId = organizationRepository.findByCode(req.organizationCode())
                    .map(org -> org.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNKNOWN_ORGANIZATION_CODE"));
        }

        User user = new User();
        user.setUsername(req.username());
        user.setDisplayName(req.displayName());
        user.setEmail(req.email());
        user.setPasswordBcrypt(passwordEncoder.encode(req.password()));
        user.setRole(req.requestedRole());
        user.setStatus("PENDING");
        if (resolvedOrgId != null) { user.setOrganizationId(resolvedOrgId); }

        user = userRepository.save(user);
        log.info("Registered user={} role={}", user.getId(), user.getRole());
        return RegisterResponse.pending(user.getId());
    }

    @Transactional
    public LoginResponse login(LoginRequest req, String clientIp) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        LockoutPolicy.clearExpiredLock(user);

        switch (user.getStatus()) {
            case "PENDING" -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING");
            case "SUSPENDED", "DELETED" -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
            case "LOCKED" -> {
                if (LockoutPolicy.isLockedOut(user)) {
                    userRepository.save(user);
                    auditLogin("LOCKOUT", user.getId(), clientIp);
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
                }
            }
            default -> {}
        }

        checkIpAllowList(user, clientIp);

        if (!passwordEncoder.matches(req.password(), user.getPasswordBcrypt())) {
            LockoutPolicy.recordFailure(user);
            userRepository.save(user);
            auditLogin("LOGIN_FAILURE", user.getId(), clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        LockoutPolicy.resetOnSuccess(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditLogin("LOGIN_SUCCESS", user.getId(), clientIp);

        boolean newDevice = deviceFingerprintService.processFingerprint(
                user.getId(), req.deviceFingerprint(), clientIp);

        String accessToken = jwtService.issueAccessToken(user.getId(), user.getRole(), user.getOrganizationId());
        String rawRefreshToken = issueRefreshToken(user.getId(), UUID.randomUUID());

        List<String> ipRanges = allowedIpRangeRepository.findCidrsByRole(user.getRole());
        UserProfileDto profile = toProfile(user, ipRanges);

        return new LoginResponse(accessToken, rawRefreshToken, LoginResponse.EXPIRES_IN, profile, newDevice);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest req) {
        String tokenHash = sha256(req.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));

        if (token.isRevoked()) {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE");
        }

        if (token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED");
        }

        refreshTokenRepository.revokeById(token.getId(), Instant.now());

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));

        if (user.getDeletedAt() != null || !"ACTIVE".equals(user.getStatus())) {
            // Suspended/locked/pending/deleted users must not receive new tokens.
            // Revoke entire family to force re-login after an admin change.
            refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_ACTIVE");
        }

        String accessToken = jwtService.issueAccessToken(user.getId(), user.getRole(), user.getOrganizationId());
        String rawRefreshToken = issueRefreshToken(user.getId(), token.getFamilyId());

        List<String> ipRanges = allowedIpRangeRepository.findCidrsByRole(user.getRole());
        UserProfileDto profile = toProfile(user, ipRanges);

        return new LoginResponse(accessToken, rawRefreshToken, LoginResponse.EXPIRES_IN, profile, false);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        String tokenHash = sha256(req.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> refreshTokenRepository.revokeById(token.getId(), Instant.now()));
    }

    private void auditLogin(String action, UUID userId, String clientIp) {
        String details = toJson(Map.of("ip", clientIp != null ? clientIp : ""));
        AuditEvent event = AuditEvent.of(userId, action, "USER", userId.toString(), details);
        event.setIpAddress(clientIp);
        // Publish in REQUIRES_NEW so the audit trail survives a rolled-back
        // authentication transaction (e.g. bad password -> 401 rollback).
        auditEventPublisher.publish(event);
    }

    private void checkIpAllowList(User user, String clientIp) {
        long ruleCount = allowedIpRangeRepository.countRulesForRole(user.getRole());
        if (ruleCount == 0) return;

        boolean allowed = allowedIpRangeRepository.isIpAllowed(clientIp, user.getRole());
        if (!allowed) {
            anomalyEventRepository.save(AnomalyEvent.of(user.getId(), "IP_OUT_OF_RANGE", clientIp,
                    toJson(Map.of("role", user.getRole()))));
            notificationService.send(user.getId(), "anomaly.ipOutOfRange",
                    toJson(Map.of("ip", clientIp != null ? clientIp : "")));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "IP_NOT_ALLOWED");
        }
    }

    private String issueRefreshToken(UUID userId, UUID familyId) {
        String raw = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(sha256(raw));
        rt.setFamilyId(familyId);
        rt.setIdleExpiresAt(now.plus(IDLE_TTL_HOURS, ChronoUnit.HOURS));
        rt.setAbsoluteExpiresAt(now.plus(ABSOLUTE_TTL_HOURS, ChronoUnit.HOURS));
        refreshTokenRepository.save(rt);
        return raw;
    }

    private UserProfileDto toProfile(User user, List<String> ipRanges) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getOrganizationId(),
                ipRanges,
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit payload", e);
            return "{}";
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
