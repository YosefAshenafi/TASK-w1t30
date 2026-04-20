package com.meridian.sessions.repository;

import com.meridian.sessions.entity.SessionActivitySet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionActivitySetRepository extends JpaRepository<SessionActivitySet, UUID> {

    Optional<SessionActivitySet> findBySessionIdAndActivityIdAndSetIndex(
            UUID sessionId, UUID activityId, int setIndex);

    @Query("SELECT MAX(s.setIndex) FROM SessionActivitySet s WHERE s.sessionId = :sessionId AND s.activityId = :activityId")
    Optional<Integer> findMaxSetIndex(@Param("sessionId") UUID sessionId,
                                      @Param("activityId") UUID activityId);
}
