package com.meridian.security.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes audit events in their own transaction so failed business transactions
 * (e.g. an authentication rollback) still leave a persistent audit trail.
 */
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final AuditEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent publish(AuditEvent event) {
        return repository.save(event);
    }
}
