package com.meridian.approvals;

import com.meridian.approvals.entity.ApprovalRequest;
import com.meridian.approvals.repository.ApprovalRequestRepository;
import com.meridian.notifications.NotificationService;
import com.meridian.reports.entity.ReportRun;
import com.meridian.reports.repository.ReportRunRepository;
import com.meridian.reports.runner.ReportRunner;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRequestRepository approvalRepository;
    private final ReportRunRepository reportRunRepository;
    private final ReportRunner reportRunner;
    private final NotificationService notificationService;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public ApprovalRequest approve(UUID approvalId, UUID reviewerId, String reason) {
        ApprovalRequest ar = findPending(approvalId);
        ar.setStatus("APPROVED");
        ar.setReviewedBy(reviewerId);
        ar.setReason(reason);
        ar.setDecidedAt(Instant.now());
        approvalRepository.save(ar);

        notificationService.send(ar.getRequestedBy(), "approval.decided",
                "{\"type\":\"" + ar.getType() + "\",\"decision\":\"APPROVED\"}");

        if ("EXPORT".equals(ar.getType())) {
            List<ReportRun> runs = reportRunRepository.findAllByApprovalRequestIdAndStatus(
                    ar.getId(), "NEEDS_APPROVAL");
            for (ReportRun run : runs) {
                reportRunner.execute(run);
            }
        }

        return ar;
    }

    @Transactional
    public ApprovalRequest reject(UUID approvalId, UUID reviewerId, String reason) {
        ApprovalRequest ar = findPending(approvalId);
        ar.setStatus("REJECTED");
        ar.setReviewedBy(reviewerId);
        ar.setReason(reason);
        ar.setDecidedAt(Instant.now());
        approvalRepository.save(ar);

        auditEventRepository.save(AuditEvent.of(reviewerId, "PERMISSION_CHANGE",
                "APPROVAL", approvalId.toString(),
                "{\"action\":\"REJECT\",\"reason\":\"" + reason + "\"}"));

        notificationService.send(ar.getRequestedBy(), "approval.decided",
                "{\"type\":\"" + ar.getType() + "\",\"decision\":\"REJECTED\"}");

        if ("EXPORT".equals(ar.getType())) {
            List<ReportRun> runs = reportRunRepository.findAllByApprovalRequestIdAndStatus(
                    ar.getId(), "NEEDS_APPROVAL");
            for (ReportRun run : runs) {
                run.setStatus("FAILED");
                run.setErrorMessage("Export approval rejected");
                run.setCompletedAt(Instant.now());
                reportRunRepository.save(run);
            }
        }

        return ar;
    }

    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void expireOldRequests() {
        int expired = approvalRepository.expireOldRequests(Instant.now());
        if (expired > 0) {
            log.info("Expired {} approval requests", expired);
        }
    }

    private ApprovalRequest findPending(UUID id) {
        ApprovalRequest ar = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!"PENDING".equals(ar.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval is not in PENDING status");
        }
        return ar;
    }
}
