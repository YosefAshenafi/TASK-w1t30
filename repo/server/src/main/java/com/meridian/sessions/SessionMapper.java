package com.meridian.sessions;

import com.meridian.sessions.dto.SessionSetDto;
import com.meridian.sessions.dto.TrainingSessionDto;
import com.meridian.sessions.entity.SessionActivitySet;
import com.meridian.sessions.entity.TrainingSession;

public final class SessionMapper {

    private SessionMapper() {}

    public static TrainingSessionDto toDto(TrainingSession s) {
        return new TrainingSessionDto(s.getId(), s.getStudentId(), s.getCourseId(),
                s.getCohortId(), s.getRestSecondsDefault(), s.getStatus(),
                s.getStartedAt(), s.getEndedAt(), s.getClientUpdatedAt());
    }

    public static SessionSetDto toSetDto(SessionActivitySet s) {
        return new SessionSetDto(s.getId(), s.getSessionId(), s.getActivityId(),
                s.getSetIndex(), s.getRestSeconds(), s.getCompletedAt(),
                s.getNotes(), s.getClientUpdatedAt());
    }
}
