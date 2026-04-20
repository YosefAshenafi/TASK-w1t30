package com.meridian.reports.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.meridian.auth.repository.UserRepository;
import com.meridian.governance.MaskingPolicy;
import com.meridian.notifications.NotificationService;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRunner {

    private static final Set<Integer> ALLOWED_CERT_WINDOWS = Set.of(30, 60, 90);

    private final ReportRunRepository reportRunRepository;
    private final AuditEventRepository auditEventRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final MaskingPolicy maskingPolicy;
    private final UserRepository userRepository;

    @Value("${app.export-path:/app/exports}")
    private String exportPath;

    @Async
    @Transactional
    public void execute(ReportRun run) {
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now());
        reportRunRepository.save(run);

        auditEventRepository.save(AuditEvent.of(run.getRequestedBy(), "EXPORT_ATTEMPT",
                "REPORT", run.getId().toString(), "{}"));

        try {
            List<Map<String, Object>> rows = fetchData(run);
            run.setRowCount(rows.size());

            Path outDir = Path.of(exportPath);
            Files.createDirectories(outDir);

            String format = extractFormat(run.getParameters());
            String filePath = exportPath + "/" + run.getId() + "." + format.toLowerCase();

            if ("PDF".equalsIgnoreCase(format)) {
                writePdf(rows, filePath, run.getType());
            } else if ("JSON".equalsIgnoreCase(format)) {
                writeJson(rows, filePath);
            } else {
                writeCsv(rows, filePath);
            }

            run.setFilePath(filePath);
            run.setStatus("SUCCEEDED");
            run.setCompletedAt(Instant.now());
            reportRunRepository.save(run);

            auditEventRepository.save(AuditEvent.of(run.getRequestedBy(), "EXPORT_SUCCESS",
                    "REPORT", run.getId().toString(), "{\"filePath\":\"" + filePath + "\"}"));

            notificationService.send(run.getRequestedBy(), "export.ready", "{\"runId\":\"" + run.getId() + "\",\"type\":\"" + run.getType() + "\"}");

        } catch (Exception e) {
            log.error("Report run {} failed", run.getId(), e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(Instant.now());
            reportRunRepository.save(run);

            notificationService.send(run.getRequestedBy(), "export.failed", "{\"runId\":\"" + run.getId() + "\"}");
        }
    }

    private List<Map<String, Object>> fetchData(ReportRun run) {
        boolean canUnmask = canUnmaskForRun(run);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orgId", run.getOrganizationId());

        return switch (run.getType()) {
            case "ENROLLMENTS" -> {
                List<Map<String, Object>> rows = jdbc.queryForList("""
                        SELECT e.id, u.username, u.display_name, u.email, c.name AS cohort_name,
                               co.code AS course_code, e.enrolled_at, e.refunded_at
                        FROM enrollments e
                        JOIN users u ON u.id = e.student_id
                        JOIN cohorts c ON c.id = e.cohort_id
                        JOIN courses co ON co.id = c.course_id
                        WHERE e.deleted_at IS NULL
                          AND (CAST(:orgId AS uuid) IS NULL OR e.organization_id = CAST(:orgId AS uuid))
                        LIMIT 50000
                        """, params);
                yield canUnmask ? rows : maskRows(rows, "username", "display_name", "email");
            }
            case "SEAT_UTILIZATION" -> jdbc.queryForList("""
                    SELECT c.id, c.name, co.code, c.total_seats,
                           COUNT(e.id) AS active_enrollments,
                           COUNT(e.id)::numeric / c.total_seats AS utilization
                    FROM cohorts c
                    JOIN courses co ON co.id = c.course_id
                    LEFT JOIN enrollments e ON e.cohort_id = c.id AND e.deleted_at IS NULL
                    WHERE (CAST(:orgId AS uuid) IS NULL
                           OR (SELECT COUNT(1) FROM enrollments oe
                               WHERE oe.cohort_id = c.id
                                 AND oe.organization_id = CAST(:orgId AS uuid)
                                 AND oe.deleted_at IS NULL) > 0)
                    GROUP BY c.id, c.name, co.code, c.total_seats
                    """, params);
            case "CERT_EXPIRING" -> {
                int days = resolveCertWindow(run.getParameters());
                params.addValue("days", days);
                List<Map<String, Object>> rows = jdbc.queryForList("""
                        SELECT cert.id, u.username, u.display_name, u.email, co.code,
                               cert.issued_at, cert.expires_at
                        FROM certifications cert
                        JOIN users u ON u.id = cert.student_id
                        JOIN courses co ON co.id = cert.course_id
                        WHERE cert.expires_at <= NOW() + make_interval(days => :days)
                          AND (CAST(:orgId AS uuid) IS NULL OR u.organization_id = CAST(:orgId AS uuid))
                        ORDER BY cert.expires_at
                        """, params);
                yield canUnmask ? rows : maskRows(rows, "username", "display_name", "email");
            }
            case "REFUND_RETURN_RATE" -> jdbc.queryForList("""
                    SELECT
                        DATE_TRUNC('month', e.enrolled_at) AS month,
                        COUNT(e.id) AS total_enrollments,
                        COUNT(e.refunded_at) AS total_refunds,
                        ROUND(COUNT(e.refunded_at)::numeric / NULLIF(COUNT(e.id), 0) * 100, 2) AS refund_rate_pct,
                        o.name AS organization_name
                    FROM enrollments e
                    LEFT JOIN organizations o ON o.id = e.organization_id
                    WHERE e.deleted_at IS NULL
                      AND (CAST(:orgId AS uuid) IS NULL OR e.organization_id = CAST(:orgId AS uuid))
                    GROUP BY DATE_TRUNC('month', e.enrolled_at), o.name
                    ORDER BY month DESC
                    LIMIT 24
                    """, params);
            case "INVENTORY_LEVELS" -> jdbc.queryForList("""
                    SELECT
                        COALESCE(ot.metadata->>'materialCode', 'UNSPECIFIED') AS material_code,
                        COALESCE(ot.metadata->>'materialName', 'Unspecified Material') AS material_name,
                        SUM(CASE WHEN ot.amount > 0 THEN ot.amount ELSE 0 END) AS total_received,
                        SUM(CASE WHEN ot.amount < 0 THEN ABS(ot.amount) ELSE 0 END) AS total_consumed,
                        SUM(ot.amount) AS current_stock,
                        MAX(ot.occurred_at) AS last_updated
                    FROM operational_transactions ot
                    WHERE ot.type = 'INVENTORY_ADJUST'
                      AND (CAST(:orgId AS uuid) IS NULL OR ot.organization_id = CAST(:orgId AS uuid))
                    GROUP BY ot.metadata->>'materialCode', ot.metadata->>'materialName'
                    ORDER BY current_stock ASC
                    LIMIT 200
                    """, params);
            default -> List.of(Map.of("message", "No data for type: " + run.getType()));
        };
    }

    private boolean canUnmaskForRun(ReportRun run) {
        if (run.getRequestedBy() == null) return false;
        return userRepository.findById(run.getRequestedBy())
                .map(u -> "ADMIN".equals(u.getRole()) || "FACULTY_MENTOR".equals(u.getRole()))
                .orElse(false);
    }

    private List<Map<String, Object>> maskRows(List<Map<String, Object>> rows, String... fields) {
        return rows.stream().map(row -> {
            Map<String, Object> copy = new LinkedHashMap<>(row);
            for (String field : fields) {
                Object val = copy.get(field);
                if (val != null) {
                    copy.put(field, maskingPolicy.maskField(field, val.toString()));
                }
            }
            return (Map<String, Object>) copy;
        }).toList();
    }

    private int resolveCertWindow(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) return 90;
        try {
            JsonNode node = objectMapper.readTree(parametersJson);
            JsonNode field = node.get("certExpiringDays");
            if (field != null && field.isInt()) {
                int value = field.asInt();
                if (ALLOWED_CERT_WINDOWS.contains(value)) return value;
            }
        } catch (Exception e) {
            log.debug("Failed to parse certExpiringDays from parameters", e);
        }
        return 90;
    }

    private void writeCsv(List<Map<String, Object>> rows, String filePath) throws Exception {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            if (!rows.isEmpty()) {
                String[] headers = rows.get(0).keySet().toArray(new String[0]);
                writer.writeNext(headers);
                for (Map<String, Object> row : rows) {
                    writer.writeNext(row.values().stream()
                            .map(v -> v != null ? v.toString() : "")
                            .toArray(String[]::new));
                }
            }
        }
    }

    private void writePdf(List<Map<String, Object>> rows, String filePath, String title) throws Exception {
        Document document = new Document();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            PdfWriter.getInstance(document, fos);
            document.open();
            document.add(new Paragraph("Report: " + title));
            document.add(new Paragraph("Generated: " + Instant.now()));
            document.add(new Paragraph("Total rows: " + rows.size()));
            for (Map<String, Object> row : rows) {
                document.add(new Paragraph(row.toString()));
            }
        }
    }

    private void writeJson(List<Map<String, Object>> rows, String filePath) throws Exception {
        Files.writeString(Path.of(filePath), objectMapper.writeValueAsString(rows));
    }

    private String extractFormat(String params) {
        if (params != null && params.contains("CSV")) return "CSV";
        if (params != null && params.contains("PDF")) return "PDF";
        if (params != null && params.contains("JSON")) return "JSON";
        return "CSV";
    }
}
