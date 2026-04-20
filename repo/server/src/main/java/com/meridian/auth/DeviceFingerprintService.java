package com.meridian.auth;

import com.meridian.auth.entity.UserDeviceFingerprint;
import com.meridian.auth.repository.UserDeviceFingerprintRepository;
import com.meridian.auth.repository.UserRepository;
import com.meridian.notifications.NotificationService;
import com.meridian.security.entity.AnomalyEvent;
import com.meridian.security.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private final UserDeviceFingerprintRepository fingerprintRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public boolean processFingerprint(UUID userId, String fingerprintHash, String clientIp) {
        Optional<UserDeviceFingerprint> existing =
                fingerprintRepository.findByIdUserIdAndIdFingerprintHash(userId, fingerprintHash);

        if (existing.isPresent()) {
            existing.get().setLastSeenAt(Instant.now());
            fingerprintRepository.save(existing.get());
            return false;
        }

        fingerprintRepository.save(UserDeviceFingerprint.create(userId, fingerprintHash));

        anomalyEventRepository.save(AnomalyEvent.of(userId, "NEW_DEVICE", clientIp,
                "{\"fingerprint\":\"" + fingerprintHash + "\"}"));

        String payload = "{\"userId\":\"" + userId + "\"}";
        notificationService.send(userId, "anomaly.newDevice", payload);

        List<UUID> adminIds = userRepository.findActiveAdmins().stream()
                .map(u -> u.getId())
                .filter(id -> !id.equals(userId))
                .toList();
        notificationService.sendToAll(adminIds, "anomaly.newDevice", payload);

        log.warn("New device fingerprint detected for user={}", userId);
        return true;
    }
}
