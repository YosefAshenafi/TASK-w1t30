package com.meridian.sessions.repository;

import com.meridian.sessions.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.createdAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
