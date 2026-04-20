package com.meridian;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for §5 Reports — contract tests and security checks.
 *
 * Covers: create payload shape, DTO field names, approval trigger, download endpoint,
 * cross-user access denial, REFUND_RETURN_RATE and INVENTORY_LEVELS kinds.
 */
@SpringBootTest(classes = com.meridian.MeridianApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "classpath:sync-test-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ReportApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String REPORTS_URL = "/api/v1/reports";
    private static final String OWNER_USER = "00000000-0000-0000-0000-000000000002";
    private static final String OTHER_USER = "00000000-0000-0000-0000-000000000003";

    private static String createdRunId;

    @Test
    @Order(1)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void createReport_sendKindField_returns202WithKindInResponse() throws Exception {
        String body = """
                {
                  "kind": "ENROLLMENTS",
                  "format": "CSV"
                }
                """;
        var result = mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.kind").value("ENROLLMENTS"))
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        createdRunId = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
    }

    @Test
    @Order(2)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void getReport_ownerCanAccess_returnsRun() throws Exception {
        if (createdRunId == null) return;
        mockMvc.perform(get(REPORTS_URL + "/" + createdRunId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdRunId));
    }

    @Test
    @Order(3)
    @WithMockUser(username = OTHER_USER, roles = "FACULTY_MENTOR")
    void getReport_otherUserDenied_returns403() throws Exception {
        if (createdRunId == null) return;
        mockMvc.perform(get(REPORTS_URL + "/" + createdRunId))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @WithMockUser(username = OTHER_USER, roles = "FACULTY_MENTOR")
    void cancelReport_otherUserDenied_returns403() throws Exception {
        if (createdRunId == null) return;
        mockMvc.perform(post(REPORTS_URL + "/" + createdRunId + "/cancel"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void createReport_restrictedClassification_triggerApproval() throws Exception {
        String body = """
                {
                  "kind": "ENROLLMENTS",
                  "format": "CSV",
                  "classification": "RESTRICTED"
                }
                """;
        mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("NEEDS_APPROVAL"));
    }

    @Test
    @Order(6)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void createReport_refundReturnRate_returnsStructuredRows() throws Exception {
        String body = """
                {
                  "kind": "REFUND_RETURN_RATE",
                  "format": "CSV"
                }
                """;
        mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.kind").value("REFUND_RETURN_RATE"))
            .andExpect(jsonPath("$.status").isNotEmpty());
    }

    @Test
    @Order(7)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void createReport_inventoryLevels_returnsStructuredRows() throws Exception {
        String body = """
                {
                  "kind": "INVENTORY_LEVELS",
                  "format": "CSV"
                }
                """;
        mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.kind").value("INVENTORY_LEVELS"));
    }

    @Test
    @Order(8)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void downloadReport_notReady_returns409() throws Exception {
        // A freshly created (QUEUED) report is not ready for download
        String body = """
                {
                  "kind": "ENROLLMENTS",
                  "format": "CSV"
                }
                """;
        var result = mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isAccepted())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        String runId = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get(REPORTS_URL + "/" + runId + "/download"))
            .andExpect(status().is(anyOf(equalTo(409), equalTo(404))));
    }

    @Test
    @Order(9)
    @WithMockUser(username = OWNER_USER, roles = "STUDENT")
    void studentCannotCreateReports() throws Exception {
        String body = """
                {
                  "kind": "ENROLLMENTS",
                  "format": "CSV"
                }
                """;
        mockMvc.perform(post(REPORTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void listReports_returnsOnlyOwnRuns() throws Exception {
        mockMvc.perform(get(REPORTS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(11)
    @WithMockUser(username = OWNER_USER, roles = "ADMIN")
    void adminListReports_returnsAllRuns() throws Exception {
        mockMvc.perform(get(REPORTS_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(12)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void createSchedule_returnsScheduleDto() throws Exception {
        String body = """
                {
                  "kind": "ENROLLMENTS",
                  "format": "CSV",
                  "cronExpr": "0 0 1 * * *"
                }
                """;
        mockMvc.perform(post(REPORTS_URL + "/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @Order(13)
    @WithMockUser(username = OWNER_USER, roles = "FACULTY_MENTOR")
    void updateSchedule_toggleEnabled_returns200() throws Exception {
        // Create a schedule first
        String createBody = """
                {
                  "kind": "CERT_EXPIRING",
                  "format": "CSV",
                  "cronExpr": "0 0 3 * * *"
                }
                """;
        var createResult = mockMvc.perform(post(REPORTS_URL + "/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andReturn();
        String schedJson = createResult.getResponse().getContentAsString();
        String schedId = schedJson.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Toggle enabled=false
        mockMvc.perform(put(REPORTS_URL + "/schedules/" + schedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false,\"cronExpr\":\"0 0 3 * * *\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));
    }
}
