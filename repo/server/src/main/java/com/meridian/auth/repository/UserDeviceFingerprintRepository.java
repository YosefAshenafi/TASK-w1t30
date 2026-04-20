package com.meridian.auth.repository;

import com.meridian.auth.entity.UserDeviceFingerprint;
import com.meridian.auth.entity.UserDeviceFingerprintId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserDeviceFingerprintRepository extends JpaRepository<UserDeviceFingerprint, UserDeviceFingerprintId> {

    Optional<UserDeviceFingerprint> findByIdUserIdAndIdFingerprintHash(UUID userId, String fingerprintHash);
}
