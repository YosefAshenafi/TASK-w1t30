package com.meridian.courses.repository;

import com.meridian.courses.entity.KnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, UUID> {
    List<KnowledgePoint> findByCourseId(UUID courseId);
}
