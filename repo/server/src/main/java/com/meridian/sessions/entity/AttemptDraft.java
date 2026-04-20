package com.meridian.sessions.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt_drafts")
@Getter
@Setter
@NoArgsConstructor
public class AttemptDraft {

    @Id
    @Column(length = 128)
    private String id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "chosen_answer")
    private String chosenAnswer;

    @Column(name = "client_updated_at", nullable = false)
    private Instant clientUpdatedAt;

    @UpdateTimestamp
    @Column(name = "server_updated_at", nullable = false)
    private Instant serverUpdatedAt;
}
