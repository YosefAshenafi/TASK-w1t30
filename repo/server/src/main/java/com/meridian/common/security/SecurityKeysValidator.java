package com.meridian.common.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("!test")
public class SecurityKeysValidator {

    private static final Set<String> PLACEHOLDER_JWT_KEYS = Set.of(
            "changeme-replace-in-production-must-be-long", "", "secret", "changeme"
    );

    private static final Set<String> PLACEHOLDER_AES_KEYS = Set.of(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", ""
    );

    @Value("${app.jwt.signing-key}")
    private String jwtSigningKey;

    @Value("${app.aes-key-base64}")
    private String aesKeyBase64;

    @PostConstruct
    public void validate() {
        if (jwtSigningKey == null || PLACEHOLDER_JWT_KEYS.contains(jwtSigningKey.trim())) {
            throw new IllegalStateException(
                    "app.jwt.signing-key must be set to a strong secret via JWT_SIGNING_KEY environment variable. " +
                    "Refusing to start with placeholder key.");
        }
        if (jwtSigningKey.length() < 32) {
            throw new IllegalStateException(
                    "app.jwt.signing-key must be at least 32 characters long.");
        }
        if (aesKeyBase64 == null || PLACEHOLDER_AES_KEYS.contains(aesKeyBase64.trim())) {
            throw new IllegalStateException(
                    "app.aes-key-base64 must be set to a valid AES-256 key via AES_KEY_BASE64 environment variable. " +
                    "Refusing to start with placeholder key.");
        }
    }
}
