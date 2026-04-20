package com.meridian.courses.repository;

import com.meridian.courses.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByCourseIdOrderBySortOrder(UUID courseId);
}
