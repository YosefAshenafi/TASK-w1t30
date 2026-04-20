package com.meridian.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserDeviceFingerprintId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "fingerprint_hash")
    private String fingerprintHash;
}
