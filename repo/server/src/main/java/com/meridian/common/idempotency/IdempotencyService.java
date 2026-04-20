package com.meridian.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.sessions.entity.IdempotencyKey;
import com.meridian.sessions.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public String hashBody(String body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public <T> Optional<T> check(String idemKey, String requestHash, Class<T> responseType) {
        if (idemKey == null) return Optional.empty();

        return repository.findById(idemKey).map(cached -> {
            if (!cached.getRequestHash().equals(requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "IDEMPOTENCY_MISMATCH");
            }
            try {
                return objectMapper.readValue(cached.getResponseJson(), responseType);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached idempotency response for key={}", idemKey, e);
                return null;
            }
        });
    }

    @Transactional
    public <T> void store(String idemKey, UUID userId, String requestHash, T response) {
        if (idemKey == null) return;
        try {
            String json = objectMapper.writeValueAsString(response);
            repository.save(IdempotencyKey.of(idemKey, userId, requestHash, json));
        } catch (Exception e) {
            log.warn("Failed to store idempotency response for key={}", idemKey, e);
        }
    }
}
