package com.meridian.auth.repository;

import com.meridian.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByStatus(String status);

    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN' AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    List<User> findActiveAdmins();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (:status IS NULL OR u.status = :status)")
    List<User> findByStatusFilter(@Param("status") String status);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL ORDER BY u.deletedAt DESC")
    Page<User> findSoftDeleted(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :cutoff")
    List<User> findSoftDeletedBefore(@Param("cutoff") Instant cutoff);
}
