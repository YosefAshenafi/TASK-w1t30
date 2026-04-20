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
 * Guards against accidental leakage of sensitive fields in API responses.
 *
 * Covers §8.2 gap: "No automated guard for accidental sensitive data leakage."
 * Verifies that password hashes, raw tokens, and unmasked PII are never
 * returned in response DTOs accessible to standard callers.
 */
@SpringBootTest(classes = com.meridian.MeridianApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SensitiveDataApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String STUDENT_ID = "00000000-0000-0000-0000-000000000002";
    private static final String ADMIN_ID   = "00000000-0000-0000-0000-000000000001";

    // ---- Registration response ----

    @Test
    @Order(1)
    void register_responseDoesNotExposePasswordHash() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "username": "leak_test_user",
                          "displayName": "Leak Test",
                          "email": "leak@example.com",
                          "password": "Str0ng!Pa$$word",
                          "requestedRole": "STUDENT"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordBcrypt").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---- /users/me — no password hash ----

    @Test
    @Order(2)
    @WithMockUser(username = STUDENT_ID, roles = "STUDENT")
    void usersMe_doesNotExposePasswordBcrypt() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordBcrypt").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @Order(3)
    @WithMockUser(username = STUDENT_ID, roles = "STUDENT")
    void usersMe_doesNotExposeRefreshToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    // ---- Session responses — no other-user data ----

    @Test
    @Order(4)
    @WithMockUser(username = STUDENT_ID, roles = "STUDENT")
    void sessionList_studentOnlySeesOwnStudentId() throws Exception {
        mockMvc.perform(get("/api/v1/sessions?size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // Every session in the list must be owned by the calling student
                .andExpect(jsonPath("$.content[?(@.studentId != '" + STUDENT_ID + "')]").isEmpty());
    }

    @Test
    @Order(5)
    @WithMockUser(username = STUDENT_ID, roles = "STUDENT")
    void sessionList_doesNotExposeOtherUserPasswordFields() throws Exception {
        mockMvc.perform(get("/api/v1/sessions?size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].passwordBcrypt").isEmpty())
                .andExpect(jsonPath("$.content[*].password").isEmpty());
    }

    // ---- Admin user list — no password hash ----

    @Test
    @Order(6)
    @WithMockUser(username = ADMIN_ID, roles = "ADMIN")
    void adminUserList_doesNotExposePasswordBcrypt() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].passwordBcrypt").isEmpty())
                .andExpect(jsonPath("$.content[*].password").isEmpty());
    }

    // ---- Notification list — no raw token or internal IDs ----

    @Test
    @Order(7)
    @WithMockUser(username = STUDENT_ID, roles = "STUDENT")
    void notificationList_doesNotExposeRawTokens() throws Exception {
        mockMvc.perform(get("/api/v1/notifications?size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].token").isEmpty())
                .andExpect(jsonPath("$.content[*].refreshToken").isEmpty());
    }
}
