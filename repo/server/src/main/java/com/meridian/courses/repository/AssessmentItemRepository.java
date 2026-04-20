package com.meridian.courses.repository;

import com.meridian.courses.entity.AssessmentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentItemRepository extends JpaRepository<AssessmentItem, UUID> {
    Page<AssessmentItem> findByCourseIdAndDeletedAtIsNull(UUID courseId, Pageable pageable);
}
