package com.meridian.sessions.repository;

import com.meridian.sessions.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {
}
