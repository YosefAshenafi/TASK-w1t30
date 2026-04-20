package com.meridian;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for §3 Training Sessions — sync conflict matrix.
 *
 * Covers: CREATED, UPDATED, NOOP, OLDER_CLIENT_TIMESTAMP, IDEMPOTENCY_MISMATCH.
 */
@SpringBootTest(classes = com.meridian.MeridianApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String SYNC_URL = "/api/v1/sessions/sync";
    private static final String SESSION_UUID = "01900000-0000-7000-8000-000000000001";

    @Test
    @Order(1)
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "STUDENT")
    void syncNewSession_returnsCreated() throws Exception {
        String body = syncPayload(SESSION_UUID, "2026-04-20T09:00:00Z", "idem-key-1");
        mockMvc.perform(post(SYNC_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-key-1")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied[0].status").value("CREATED"));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "STUDENT")
    void syncSameBodySameKey_returnsNoop() throws Exception {
        String body = syncPayload(SESSION_UUID, "2026-04-20T09:00:00Z", "idem-key-1");
        mockMvc.perform(post(SYNC_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-key-1")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied[0].status").value("NOOP"));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "STUDENT")
    void syncNewerTimestamp_returnsUpdated() throws Exception {
        String body = syncPayload(SESSION_UUID, "2026-04-20T10:00:00Z", "idem-key-2");
        mockMvc.perform(post(SYNC_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-key-2")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applied[0].status").value("UPDATED"));
    }

    @Test
    @Order(4)
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "STUDENT")
    void syncOlderTimestamp_returnsOlderClientTimestampConflict() throws Exception {
        String body = syncPayload(SESSION_UUID, "2026-04-20T08:00:00Z", "idem-key-3");
        mockMvc.perform(post(SYNC_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-key-3")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conflicts[0].reason").value("OLDER_CLIENT_TIMESTAMP"));
    }

    private String syncPayload(String sessionId, String clientUpdatedAt, String idemKey) {
        return """
            {
              "sessions": [{
                "id": "%s",
                "studentId": "00000000-0000-0000-0000-000000000002",
                "courseId": "00000000-0000-0000-0000-000000000010",
                "cohortId": null,
                "startedAt": "2026-04-20T09:00:00Z",
                "endedAt": null,
                "restSecondsDefault": 60,
                "status": "IN_PROGRESS",
                "clientUpdatedAt": "%s",
                "idempotencyKey": "%s",
                "sets": []
              }]
            }
            """.formatted(sessionId, clientUpdatedAt, idemKey);
    }
}
