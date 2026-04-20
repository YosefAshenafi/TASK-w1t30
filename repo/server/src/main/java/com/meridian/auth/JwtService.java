package com.meridian.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 900;

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.signing-key}") String signingKey) {
        byte[] keyBytes = signingKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT signing key must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueAccessToken(UUID userId, String role, UUID organizationId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_TTL_SECONDS * 1000);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("orgId", organizationId != null ? organizationId.toString() : null)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public String extractOrgId(String token) {
        return parseToken(token).get("orgId", String.class);
    }

    public long getAccessTokenTtlSeconds() {
        return ACCESS_TOKEN_TTL_SECONDS;
    }
}
