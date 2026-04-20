package com.meridian.sessions.repository;

import com.meridian.sessions.entity.AttemptDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttemptDraftRepository extends JpaRepository<AttemptDraft, String> {

    List<AttemptDraft> findBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    long deleteBySessionIdAndStudentId(UUID sessionId, UUID studentId);
}
