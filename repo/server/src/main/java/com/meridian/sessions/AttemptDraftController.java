package com.meridian.sessions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.idempotency.IdempotencyService;
import com.meridian.courses.entity.AssessmentItem;
import com.meridian.courses.repository.AssessmentItemRepository;
import com.meridian.sessions.entity.AssessmentAttempt;
import com.meridian.sessions.entity.AttemptDraft;
import com.meridian.sessions.repository.AssessmentAttemptRepository;
import com.meridian.sessions.repository.AttemptDraftRepository;
import com.meridian.sessions.repository.TrainingSessionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class AttemptDraftController {

    private final AttemptDraftRepository draftRepository;
    private final TrainingSessionRepository sessionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentItemRepository assessmentItemRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping("/attempt-drafts")
    @Transactional
    public ResponseEntity<AttemptDraftDto> upsert(@Valid @RequestBody DraftRequest req, Authentication auth) {
        UUID studentId = UUID.fromString(auth.getName());
        ensureSessionOwner(req.sessionId(), studentId);

        AttemptDraft draft = draftRepository.findById(req.id()).orElseGet(AttemptDraft::new);
        if (draft.getClientUpdatedAt() != null && !req.clientUpdatedAt().isAfter(draft.getClientUpdatedAt())) {
            // Ignore stale client state (last-write-wins by clientUpdatedAt)
            return ResponseEntity.ok(AttemptDraftDto.from(draft));
        }
        draft.setId(req.id());
        draft.setSessionId(req.sessionId());
        draft.setItemId(req.itemId());
        draft.setStudentId(studentId);
        draft.setChosenAnswer(req.chosenAnswer());
        draft.setClientUpdatedAt(req.clientUpdatedAt());
        draft = draftRepository.save(draft);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AttemptDraftDto.from(draft));
    }

    @GetMapping("/{sessionId}/attempt-drafts")
    public ResponseEntity<List<AttemptDraftDto>> list(@PathVariable UUID sessionId, Authentication auth) {
        UUID studentId = UUID.fromString(auth.getName());
        ensureSessionOwner(sessionId, studentId);
        List<AttemptDraftDto> drafts = draftRepository.findBySessionIdAndStudentId(sessionId, studentId).stream()
                .map(AttemptDraftDto::from)
                .toList();
        return ResponseEntity.ok(drafts);
    }

    @DeleteMapping("/{sessionId}/attempt-drafts")
    @Transactional
    public ResponseEntity<Void> clearForSession(@PathVariable UUID sessionId, Authentication auth) {
        UUID studentId = UUID.fromString(auth.getName());
        ensureSessionOwner(sessionId, studentId);
        draftRepository.deleteBySessionIdAndStudentId(sessionId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/submit-attempts")
    @Transactional
    public ResponseEntity<SubmitResult> submitAttempts(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {

        UUID studentId = UUID.fromString(auth.getName());
        ensureSessionOwner(sessionId, studentId);

        String requestHash = idempotencyService.hashBody(sessionId + ":" + studentId);
        if (idempotencyKey != null) {
            Optional<SubmitResult> cached = idempotencyService.check(
                    idempotencyKey, studentId, requestHash, SubmitResult.class);
            if (cached.isPresent()) {
                return ResponseEntity.ok(cached.get());
            }
        }

        List<AttemptDraft> drafts = draftRepository.findBySessionIdAndStudentId(sessionId, studentId);
        if (drafts.isEmpty()) {
            SubmitResult result = new SubmitResult(0, 0);
            if (idempotencyKey != null) {
                idempotencyService.store(idempotencyKey, studentId, requestHash, result);
            }
            return ResponseEntity.ok(result);
        }

        Set<UUID> itemIds = drafts.stream().map(AttemptDraft::getItemId).collect(Collectors.toSet());
        Map<UUID, AssessmentItem> itemMap = assessmentItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(AssessmentItem::getId, Function.identity()));

        Instant now = Instant.now();
        List<AssessmentAttempt> attempts = drafts.stream().map(draft -> {
            AssessmentAttempt a = new AssessmentAttempt();
            a.setStudentId(studentId);
            a.setItemId(draft.getItemId());
            a.setChosenAnswer(asJsonValue(draft.getChosenAnswer()));
            a.setAttemptedAt(now);
            AssessmentItem item = itemMap.get(draft.getItemId());
            if (item != null) {
                a.setIsCorrect(evaluateCorrectness(item.getChoices(), draft.getChosenAnswer()));
            }
            return a;
        }).toList();

        attemptRepository.saveAll(attempts);
        draftRepository.deleteBySessionIdAndStudentId(sessionId, studentId);

        long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        SubmitResult result = new SubmitResult(attempts.size(), (int) correct);
        if (idempotencyKey != null) {
            idempotencyService.store(idempotencyKey, studentId, requestHash, result);
        }
        return ResponseEntity.ok(result);
    }

    private String asJsonValue(String raw) {
        if (raw == null) return null;
        try {
            objectMapper.readTree(raw);
            return raw;
        } catch (Exception e) {
            return objectMapper.valueToTree(raw).toString();
        }
    }

    private Boolean evaluateCorrectness(String choices, String chosenAnswer) {
        if (choices == null || chosenAnswer == null || chosenAnswer.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(choices);
            if (root.isArray()) {
                List<String> correctKeys = new ArrayList<>();
                for (JsonNode option : root) {
                    JsonNode correctNode = option.get("correct");
                    if (correctNode != null && correctNode.asBoolean()) {
                        JsonNode keyNode = option.has("key") ? option.get("key") : option.get("id");
                        if (keyNode != null) correctKeys.add(keyNode.asText());
                    }
                }
                if (correctKeys.isEmpty()) return null;
                if (correctKeys.size() == 1) {
                    return correctKeys.get(0).equals(chosenAnswer.trim());
                }
                // Multi-answer: chosenAnswer may be a JSON array of keys
                try {
                    JsonNode answerNode = objectMapper.readTree(chosenAnswer);
                    if (answerNode.isArray()) {
                        Set<String> chosen = new HashSet<>();
                        answerNode.forEach(n -> chosen.add(n.asText()));
                        return chosen.equals(new HashSet<>(correctKeys));
                    }
                } catch (Exception ignored) {}
                return correctKeys.contains(chosenAnswer.trim());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void ensureSessionOwner(UUID sessionId, UUID studentId) {
        boolean owns = sessionRepository.findById(sessionId)
                .map(s -> studentId.equals(s.getStudentId()))
                .orElse(false);
        if (!owns) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session not owned by caller");
        }
    }

    public record DraftRequest(
            @NotBlank @Size(max = 128) String id,
            @NotNull UUID sessionId,
            @NotNull UUID itemId,
            String chosenAnswer,
            @NotNull Instant clientUpdatedAt
    ) {}

    public record AttemptDraftDto(
            String id,
            UUID sessionId,
            UUID itemId,
            String chosenAnswer,
            Instant clientUpdatedAt,
            Instant serverUpdatedAt
    ) {
        public static AttemptDraftDto from(AttemptDraft d) {
            return new AttemptDraftDto(d.getId(), d.getSessionId(), d.getItemId(),
                    d.getChosenAnswer(), d.getClientUpdatedAt(), d.getServerUpdatedAt());
        }
    }

    public record SubmitResult(int saved, int correct) {}
}
