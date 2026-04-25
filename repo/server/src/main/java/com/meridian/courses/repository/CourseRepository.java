package com.meridian.courses.repository;

import com.meridian.courses.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    @Query("""
           SELECT c FROM Course c
           WHERE c.deletedAt IS NULL
             AND (:version IS NULL OR c.version = :version)
             AND (:locationId IS NULL OR c.locationId = :locationId)
             AND (:instructorId IS NULL OR c.instructorId = :instructorId)
             AND (:#{#classifications} IS NULL OR c.classification IN :classifications)
             AND (:qLike IS NULL OR LOWER(c.code) LIKE :qLike
                             OR LOWER(c.title) LIKE :qLike)
           """)
    Page<Course> findFiltered(@Param("version") String version,
                              @Param("locationId") UUID locationId,
                              @Param("instructorId") UUID instructorId,
                              @Param("classifications") Collection<String> classifications,
                              @Param("qLike") String qLike,
                              Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.deletedAt IS NOT NULL ORDER BY c.deletedAt DESC")
    Page<Course> findSoftDeleted(Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.deletedAt IS NOT NULL AND c.deletedAt < :cutoff")
    List<Course> findSoftDeletedBefore(@Param("cutoff") Instant cutoff);
}
