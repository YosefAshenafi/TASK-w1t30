package com.meridian.courses.repository;

import com.meridian.courses.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CohortRepository extends JpaRepository<Cohort, UUID> {
    List<Cohort> findByCourseId(UUID courseId);
}
