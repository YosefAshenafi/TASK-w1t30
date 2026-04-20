package com.meridian.backups;

import com.meridian.backups.entity.BackupPolicy;
import com.meridian.backups.entity.BackupRun;
import com.meridian.backups.entity.RecoveryDrill;
import com.meridian.backups.repository.BackupPolicyRepository;
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

@RestController
@RequestMapping("/api/v1/admin/backups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BackupController {

    private final BackupRunRepository backupRunRepository;
    private final RecoveryDrillRepository recoveryDrillRepository;
    private final BackupPolicyRepository backupPolicyRepository;
    private final BackupRunner backupRunner;
    private final RecoveryDrillRunner recoveryDrillRunner;

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
    public ResponseEntity<BackupPolicy> getPolicy() {
        BackupPolicy policy = backupPolicyRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    BackupPolicy p = new BackupPolicy();
                    return backupPolicyRepository.save(p);
                });
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/policy")
    public ResponseEntity<BackupPolicy> updatePolicy(@RequestBody Map<String, Object> updates) {
        BackupPolicy policy = backupPolicyRepository.findAll().stream().findFirst()
                .orElseGet(BackupPolicy::new);
        if (updates.containsKey("retentionDays")) {
            policy.setRetentionDays(((Number) updates.get("retentionDays")).intValue());
        }
        if (updates.containsKey("scheduleEnabled")) {
            policy.setScheduleEnabled((Boolean) updates.get("scheduleEnabled"));
        }
        if (updates.containsKey("scheduleCron")) {
            policy.setScheduleCron((String) updates.get("scheduleCron"));
        }
        if (updates.containsKey("backupPath")) {
            Object rawPath = updates.get("backupPath");
            String backupPath = rawPath == null ? null : rawPath.toString().trim();
            if (backupPath != null && !backupPath.isEmpty()) {
                validateBackupPath(backupPath);
                policy.setBackupPath(backupPath);
            } else {
                policy.setBackupPath(null);
            }
        }
        return ResponseEntity.ok(backupPolicyRepository.save(policy));
    }

    private void validateBackupPath(String path) {
        if (path.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "backupPath must be at most 500 characters");
        }
        if (path.contains("..") || path.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "backupPath contains unsafe characters");
        }
        if (!path.startsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "backupPath must be an absolute path");
        }
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
