package com.meridian.sessions.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chosen_answer", columnDefinition = "jsonb")
    private String chosenAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();
}
