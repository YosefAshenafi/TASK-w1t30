package com.meridian.backups;

import com.meridian.backups.entity.BackupRun;
import com.meridian.backups.entity.RecoveryDrill;
import com.meridian.backups.repository.BackupRunRepository;
import com.meridian.backups.repository.RecoveryDrillRepository;
import com.meridian.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/backups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BackupController {

    private final BackupRunRepository backupRunRepository;
    private final RecoveryDrillRepository recoveryDrillRepository;
    private final BackupRunner backupRunner;
    private final RecoveryDrillRunner recoveryDrillRunner;

    // In-memory policy (could be persisted; simple Map for now)
    private static final Map<String, Object> POLICY = new ConcurrentHashMap<>(Map.of(
            "retentionDays", 30,
            "scheduleEnabled", true,
            "scheduleCron", "0 0 2 * * *"
    ));

    @GetMapping
    public ResponseEntity<PageResponse<BackupRun>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BackupRun> p = backupRunRepository.findAll(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(new PageResponse<>(p.getContent(), page, size, (int) p.getTotalElements()));
    }

    @PostMapping("/run")
    public ResponseEntity<BackupRun> triggerBackup(
            @RequestParam(defaultValue = "FULL") String mode,
            Authentication auth) {
        String type = mode.equalsIgnoreCase("INCREMENTAL") ? "INCREMENTAL" : "FULL";
        BackupRun run = new BackupRun();
        run.setType(type);
        run.setInitiatedBy(UUID.fromString(auth.getName()));
        backupRunRepository.save(run);
        backupRunner.execute(run);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(run);
    }

    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> getPolicy() {
        return ResponseEntity.ok(POLICY);
    }

    @PutMapping("/policy")
    public ResponseEntity<Map<String, Object>> updatePolicy(@RequestBody Map<String, Object> updates) {
        updates.forEach((k, v) -> {
            if (POLICY.containsKey(k)) {
                POLICY.put(k, v);
            }
        });
        return ResponseEntity.ok(POLICY);
    }

    @PostMapping("/recovery-drill")
    public ResponseEntity<RecoveryDrill> scheduleDrill(
            @RequestBody(required = false) DrillRequest req,
            Authentication auth) {
        BackupRun latest = backupRunRepository.findFirstByStatusOrderByStartedAtDesc("COMPLETED")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No completed backup available"));

        RecoveryDrill drill = new RecoveryDrill();
        drill.setBackupRunId(latest.getId());
        drill.setConductedBy(UUID.fromString(auth.getName()));
        if (req != null && req.notes() != null) {
            drill.setNotes(req.notes());
        }
        if (req != null && req.scheduledAt() != null) {
            drill.setScheduledAt(req.scheduledAt());
        }
        recoveryDrillRepository.save(drill);
        recoveryDrillRunner.execute(drill, latest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(drill);
    }

    @GetMapping("/recovery-drills")
    public ResponseEntity<PageResponse<RecoveryDrill>> listDrills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RecoveryDrill> p = recoveryDrillRepository.findAllByOrderByScheduledAtDesc(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(new PageResponse<>(p.getContent(), page, size, (int) p.getTotalElements()));
    }

    record DrillRequest(String notes, Instant scheduledAt) {}
}
