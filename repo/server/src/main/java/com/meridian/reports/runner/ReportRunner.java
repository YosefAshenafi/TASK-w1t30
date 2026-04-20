package com.meridian.reports.runner;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRunner {

    private final ReportRunRepository reportRunRepository;
    private final AuditEventRepository auditEventRepository;
    private final JdbcTemplate jdbc;

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

        } catch (Exception e) {
            log.error("Report run {} failed", run.getId(), e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(Instant.now());
            reportRunRepository.save(run);
        }
    }

    private List<Map<String, Object>> fetchData(ReportRun run) {
        return switch (run.getType()) {
            case "ENROLLMENTS" -> jdbc.queryForList("""
                    SELECT e.id, u.username, u.display_name, c.name AS cohort_name,
                           co.code AS course_code, e.enrolled_at, e.refunded_at
                    FROM enrollments e
                    JOIN users u ON u.id = e.student_id
                    JOIN cohorts c ON c.id = e.cohort_id
                    JOIN courses co ON co.id = c.course_id
                    WHERE e.deleted_at IS NULL
                    LIMIT 50000
                    """);
            case "SEAT_UTILIZATION" -> jdbc.queryForList("""
                    SELECT c.id, c.name, co.code, c.total_seats,
                           COUNT(e.id) AS active_enrollments,
                           COUNT(e.id)::numeric / c.total_seats AS utilization
                    FROM cohorts c
                    JOIN courses co ON co.id = c.course_id
                    LEFT JOIN enrollments e ON e.cohort_id = c.id AND e.deleted_at IS NULL
                    GROUP BY c.id, c.name, co.code, c.total_seats
                    """);
            case "CERT_EXPIRING" -> jdbc.queryForList("""
                    SELECT cert.id, u.username, co.code, cert.issued_at, cert.expires_at
                    FROM certifications cert
                    JOIN users u ON u.id = cert.student_id
                    JOIN courses co ON co.id = cert.course_id
                    WHERE cert.expires_at <= NOW() + INTERVAL '90 days'
                    ORDER BY cert.expires_at
                    """);
            default -> jdbc.queryForList("SELECT 'No data for type: " + run.getType() + "' AS message");
        };
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
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            sb.append("{");
            rows.get(i).forEach((k, v) ->
                    sb.append("\"").append(k).append("\":\"").append(v != null ? v.toString().replace("\"", "\\\"") : "").append("\","));
            if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
            if (i < rows.size() - 1) sb.append(",");
        }
        sb.append("]");
        Files.writeString(Path.of(filePath), sb.toString());
    }

    private String extractFormat(String params) {
        if (params != null && params.contains("CSV")) return "CSV";
        if (params != null && params.contains("PDF")) return "PDF";
        if (params != null && params.contains("JSON")) return "JSON";
        return "CSV";
    }
}
