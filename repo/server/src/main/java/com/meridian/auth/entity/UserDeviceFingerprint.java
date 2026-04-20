package com.meridian.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_device_fingerprints")
@Getter
@Setter
@NoArgsConstructor
public class UserDeviceFingerprint {

    @EmbeddedId
    private UserDeviceFingerprintId id;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @Column(nullable = false)
    private boolean trusted = false;

    public static UserDeviceFingerprint create(java.util.UUID userId, String fingerprintHash) {
        UserDeviceFingerprint fp = new UserDeviceFingerprint();
        fp.id = new UserDeviceFingerprintId(userId, fingerprintHash);
        fp.firstSeenAt = Instant.now();
        fp.lastSeenAt = Instant.now();
        return fp;
    }
}
