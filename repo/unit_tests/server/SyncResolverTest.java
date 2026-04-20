package com.meridian.sessions;

// NOTE: This is a conceptual unit test illustrating the SyncResolver LWW + idempotency contract.
// To run it, copy to server/src/test/java/com/meridian/sessions/ and ensure Mockito is on the test classpath.

import com.meridian.common.idempotency.IdempotencyService;
import com.meridian.sessions.dto.SyncRequest;
import com.meridian.sessions.dto.SyncResult;
import com.meridian.sessions.entity.TrainingSession;
import com.meridian.sessions.repository.SessionActivitySetRepository;
import com.meridian.sessions.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies LWW (last-write-wins) and idempotency-key conflict handling in SyncResolver.
 *
 * LWW rule:
 *   - newer clientUpdatedAt → UPDATED
 *   - older clientUpdatedAt → NOOP (OLDER_CLIENT_TIMESTAMP conflict)
 *   - same idem key + same body → NOOP
 *   - same idem key + different body → IDEMPOTENCY_MISMATCH conflict
 */
class SyncResolverTest {

    private TrainingSessionRepository sessionRepo;
    private SessionActivitySetRepository setRepo;
    private IdempotencyService idempotencyService;
    private SyncResolver resolver;

    @BeforeEach
    void setUp() {
        sessionRepo = Mockito.mock(TrainingSessionRepository.class);
        setRepo = Mockito.mock(SessionActivitySetRepository.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        resolver = new SyncResolver(sessionRepo, setRepo, idempotencyService);
    }

    @Test
    void newSessionIsCreated() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SyncRequest.SessionSyncItem item = buildSessionItem(id, Instant.now(), List.of());

        when(idempotencyService.hashBody(anyString())).thenReturn("hash1");
        when(idempotencyService.check(anyString(), eq("hash1"), any())).thenReturn(Optional.empty());
        when(sessionRepo.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = resolver.resolve(new SyncRequest(List.of(item)), userId);

        assertThat(result.applied()).hasSize(1);
        assertThat(result.applied().get(0).status()).isEqualTo("CREATED");
        assertThat(result.conflicts()).isEmpty();
    }

    @Test
    void existingSessionWithNewerClientTimestampIsUpdated() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant serverTs = Instant.parse("2026-04-01T12:00:00Z");
        Instant clientTs = Instant.parse("2026-04-01T13:00:00Z"); // newer

        TrainingSession existing = new TrainingSession();
        existing.setId(id);
        existing.setStudentId(userId);
        existing.setClientUpdatedAt(serverTs);
        existing.setStatus("IN_PROGRESS");

        SyncRequest.SessionSyncItem item = buildSessionItem(id, clientTs, List.of());

        when(idempotencyService.hashBody(anyString())).thenReturn("hash2");
        when(idempotencyService.check(anyString(), eq("hash2"), any())).thenReturn(Optional.empty());
        when(sessionRepo.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncResult result = resolver.resolve(new SyncRequest(List.of(item)), userId);

        assertThat(result.applied()).hasSize(1);
        assertThat(result.applied().get(0).status()).isEqualTo("UPDATED");
    }

    @Test
    void existingSessionWithOlderClientTimestampBecomesConflict() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant serverTs = Instant.parse("2026-04-01T14:00:00Z");
        Instant clientTs = Instant.parse("2026-04-01T12:00:00Z"); // older

        TrainingSession existing = new TrainingSession();
        existing.setId(id);
        existing.setStudentId(userId);
        existing.setClientUpdatedAt(serverTs);
        existing.setStatus("COMPLETED");

        SyncRequest.SessionSyncItem item = buildSessionItem(id, clientTs, List.of());

        when(idempotencyService.hashBody(anyString())).thenReturn("hash3");
        when(idempotencyService.check(anyString(), eq("hash3"), any())).thenReturn(Optional.empty());
        when(sessionRepo.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(existing));

        SyncResult result = resolver.resolve(new SyncRequest(List.of(item)), userId);

        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).reason()).isEqualTo("OLDER_CLIENT_TIMESTAMP");
    }

    @Test
    void idempotencyMismatchBecomesConflict() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SyncRequest.SessionSyncItem item = buildSessionItem(id, Instant.now(), List.of());

        when(idempotencyService.hashBody(anyString())).thenReturn("hash4");
        when(idempotencyService.check(anyString(), eq("hash4"), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "IDEMPOTENCY_MISMATCH"));
        when(sessionRepo.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        SyncResult result = resolver.resolve(new SyncRequest(List.of(item)), userId);

        assertThat(result.conflicts()).hasSize(1);
        assertThat(result.conflicts().get(0).reason()).isEqualTo("IDEMPOTENCY_MISMATCH");
    }

    @Test
    void cachedIdempotencyKeyReturnsCachedResponse() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SyncRequest.SessionSyncItem item = buildSessionItem(id, Instant.now(), List.of());

        when(idempotencyService.hashBody(anyString())).thenReturn("hash5");
        when(idempotencyService.check(anyString(), eq("hash5"), any()))
            .thenReturn(Optional.of(Mockito.mock(com.meridian.sessions.dto.TrainingSessionDto.class)));

        SyncResult result = resolver.resolve(new SyncRequest(List.of(item)), userId);

        assertThat(result.applied()).hasSize(1);
        assertThat(result.applied().get(0).status()).isEqualTo("NOOP");
        verify(sessionRepo, never()).save(any());
    }

    private SyncRequest.SessionSyncItem buildSessionItem(UUID id, Instant clientTs, List<SyncRequest.SetSyncItem> sets) {
        return new SyncRequest.SessionSyncItem(
            id, UUID.randomUUID(), UUID.randomUUID(), null, clientTs, null, 60, "IN_PROGRESS", clientTs,
            UUID.randomUUID().toString(), sets
        );
    }
}
