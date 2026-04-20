package com.meridian.reports.repository;

import com.meridian.reports.entity.ReportSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

    Page<ReportSchedule> findAllByOwnerId(UUID ownerId, Pageable pageable);

    @Query("SELECT s FROM ReportSchedule s WHERE s.enabled = true AND (s.nextRunAt IS NULL OR s.nextRunAt <= :now)")
    List<ReportSchedule> findDue(Instant now);
}
