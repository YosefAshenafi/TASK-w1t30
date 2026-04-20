package com.meridian.sessions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.auth.repository.UserRepository;
import com.meridian.common.idempotency.IdempotencyService;
import com.meridian.common.security.AuthPrincipal;
import com.meridian.common.web.PageResponse;
import com.meridian.sessions.dto.*;
import com.meridian.sessions.entity.SessionActivitySet;
import com.meridian.sessions.entity.TrainingSession;
import com.meridian.sessions.repository.SessionActivitySetRepository;
import com.meridian.sessions.repository.TrainingSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final TrainingSessionRepository sessionRepo;
    private final SessionActivitySetRepository setRepo;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<TrainingSessionDto> create(@Valid @RequestBody CreateSessionRequest req,
                                                     Authentication auth,
                                                     HttpServletRequest httpReq) {
        UUID userId = UUID.fromString(auth.getName());
        String idemKey = (String) httpReq.getAttribute("idempotencyKey");

        if (idemKey != null) {
            String hash = idempotencyService.hashBody(serialize(req));
            Optional<TrainingSessionDto> cached = idempotencyService.check(idemKey, userId, hash, TrainingSessionDto.class);
            if (cached.isPresent()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(cached.get());
            }
            TrainingSessionDto dto = doCreate(req, userId);
            idempotencyService.store(idemKey, userId, hash, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(doCreate(req, userId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/{id}")
    public ResponseEntity<TrainingSessionDto> patch(@PathVariable UUID id,
                                                    @Valid @RequestBody PatchSessionRequest req,
                                                    Authentication auth) {
        TrainingSession session = requireSession(id, auth);
        if (req.status() != null) session.setStatus(req.status());
        if (req.restSecondsDefault() != null) session.setRestSecondsDefault(req.restSecondsDefault());
        if (req.endedAt() != null) session.setEndedAt(req.endedAt());
        if (req.clientUpdatedAt() != null) session.setClientUpdatedAt(req.clientUpdatedAt());
        return ResponseEntity.ok(SessionMapper.toDto(sessionRepo.save(session)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{id}/pause")
    public ResponseEntity<TrainingSessionDto> pause(@PathVariable UUID id, Authentication auth) {
        TrainingSession session = requireSession(id, auth);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not IN_PROGRESS");
        }
        session.setStatus("PAUSED");
        session.setClientUpdatedAt(Instant.now());
        return ResponseEntity.ok(SessionMapper.toDto(sessionRepo.save(session)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{id}/continue")
    public ResponseEntity<TrainingSessionDto> continueSession(@PathVariable UUID id, Authentication auth) {
        TrainingSession session = requireSession(id, auth);
        if (!"PAUSED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is not PAUSED");
        }
        session.setStatus("IN_PROGRESS");
        session.setClientUpdatedAt(Instant.now());
        return ResponseEntity.ok(SessionMapper.toDto(sessionRepo.save(session)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<TrainingSessionDto> complete(@PathVariable UUID id, Authentication auth) {
        TrainingSession session = requireSession(id, auth);
        session.setStatus("COMPLETED");
        session.setEndedAt(Instant.now());
        session.setClientUpdatedAt(Instant.now());
        return ResponseEntity.ok(SessionMapper.toDto(sessionRepo.save(session)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{id}/sets")
    public ResponseEntity<SessionSetDto> createSet(@PathVariable UUID id,
                                                   @Valid @RequestBody CreateSetRequest req,
                                                   Authentication auth) {
        requireSession(id, auth);
        SessionActivitySet set = new SessionActivitySet();
        set.setSessionId(id);
        set.setActivityId(req.activityId());
        set.setSetIndex(req.setIndex());
        set.setRestSeconds(req.restSeconds());
        set.setCompletedAt(req.completedAt());
        set.setNotes(req.notes());
        set.setClientUpdatedAt(req.clientUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionMapper.toSetDto(setRepo.save(set)));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/{id}/sets/{setId}")
    public ResponseEntity<SessionSetDto> patchSet(@PathVariable UUID id,
                                                  @PathVariable UUID setId,
                                                  @Valid @RequestBody PatchSetRequest req,
                                                  Authentication auth) {
        requireSession(id, auth);
        SessionActivitySet set = setRepo.findById(setId)
                .filter(s -> s.getSessionId().equals(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Set not found"));
        if (req.restSeconds() != null) set.setRestSeconds(req.restSeconds());
        if (req.completedAt() != null) set.setCompletedAt(req.completedAt());
        if (req.notes() != null) set.setNotes(req.notes());
        if (req.clientUpdatedAt() != null) set.setClientUpdatedAt(req.clientUpdatedAt());
        return ResponseEntity.ok(SessionMapper.toSetDto(setRepo.save(set)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingSessionDto> getById(@PathVariable UUID id, Authentication auth) {
        TrainingSession session = requireSession(id, auth);
        return ResponseEntity.ok(SessionMapper.toDto(session));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TrainingSessionDto>> list(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID learnerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {

        AuthPrincipal principal = AuthPrincipal.of(auth);
        String role = principal.role();
        // Accept legacy `learnerId` alias used by the Angular client.
        if (studentId == null && learnerId != null) {
            studentId = learnerId;
        }
        if ("STUDENT".equals(role)) {
            studentId = principal.userId();
        }
        Instant fromInstant = from != null ? Instant.parse(from) : null;
        Instant toInstant = to != null ? Instant.parse(to) : null;
        size = Math.min(size, 200);

        if ("CORPORATE_MENTOR".equals(role) && principal.organizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Corporate mentor has no organization scope");
        }
        String statusArg = status != null && !status.isBlank() ? status : null;
        String studentIdStr = studentId != null ? studentId.toString() : null;
        String fromStr = fromInstant != null ? fromInstant.toString() : null;
        String toStr = toInstant != null ? toInstant.toString() : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("started_at").descending());
        Page<TrainingSession> result;
        if ("CORPORATE_MENTOR".equals(role)) {
            result = sessionRepo.findFilteredByOrg(principal.organizationId().toString(), studentIdStr,
                    statusArg, fromStr, toStr, pageable);
        } else {
            result = sessionRepo.findFiltered(studentIdStr, statusArg, fromStr, toStr, pageable);
        }
        return ResponseEntity.ok(PageResponse.from(result.map(SessionMapper::toDto)));
    }

    private TrainingSessionDto doCreate(CreateSessionRequest req, UUID userId) {
        if (sessionRepo.existsById(req.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session ID already exists");
        }
        TrainingSession s = new TrainingSession();
        s.setId(req.id());
        s.setStudentId(userId);
        s.setCourseId(req.courseId());
        s.setCohortId(req.cohortId());
        s.setRestSecondsDefault(req.restSecondsDefault());
        s.setStartedAt(req.startedAt() != null ? req.startedAt() : Instant.now());
        s.setClientUpdatedAt(req.clientUpdatedAt());
        return SessionMapper.toDto(sessionRepo.save(s));
    }

    private TrainingSession requireSession(UUID id, Authentication auth) {
        TrainingSession session = sessionRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        AuthPrincipal principal = AuthPrincipal.of(auth);
        String role = principal.role();
        if ("STUDENT".equals(role) && !session.getStudentId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if ("CORPORATE_MENTOR".equals(role)) {
            UUID orgId = principal.organizationId();
            if (orgId == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            boolean inOrg = userRepository.findById(session.getStudentId())
                    .map(u -> orgId.equals(u.getOrganizationId()))
                    .orElse(false);
            if (!inOrg) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return session;
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT");
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
