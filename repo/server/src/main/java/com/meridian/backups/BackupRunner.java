package com.meridian.backups;

import com.meridian.backups.entity.BackupRun;
import com.meridian.backups.repository.BackupRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupRunner {

    private final BackupRunRepository backupRunRepository;

    @Value("${app.backup-path:/app/backups}")
    private String backupPath;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/meridian}")
    private String datasourceUrl;

    @Async
    public void execute(BackupRun run) {
        try {
            Path dir = Path.of(backupPath);
            Files.createDirectories(dir);

            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    .replace(":", "-").replace(".", "-");
            String filename = timestamp + "-" + run.getType().toLowerCase() + ".dump";
            String filePath = backupPath + "/" + filename;

            String dbName = extractDbName(datasourceUrl);
            ProcessBuilder pb = new ProcessBuilder("pg_dump", "-Fc", "-d", dbName, "-f", filePath);
            pb.environment().put("PGPASSWORD", System.getenv("DB_PASSWORD") != null ?
                    System.getenv("DB_PASSWORD") : "meridian_secret");
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                Path dumpFile = Path.of(filePath);
                long size = Files.exists(dumpFile) ? Files.size(dumpFile) : 0;
                run.setFilePath(filePath);
                run.setSizeBytes(size);
                run.setStatus("COMPLETED");
                run.setCompletedAt(Instant.now());
            } else {
                run.setStatus("FAILED");
                run.setErrorMessage("pg_dump exited with code " + exitCode);
                run.setCompletedAt(Instant.now());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Backup failed", e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            run.setCompletedAt(Instant.now());
        }
        backupRunRepository.save(run);
    }

    private String extractDbName(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1];
        return last.contains("?") ? last.split("\\?")[0] : last;
    }
}
