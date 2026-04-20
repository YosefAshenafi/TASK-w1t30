package com.meridian.courses;

import com.meridian.courses.dto.KnowledgePointDto;
import com.meridian.courses.dto.KnowledgePointRequest;
import com.meridian.courses.entity.KnowledgePoint;
import com.meridian.courses.repository.CourseRepository;
import com.meridian.courses.repository.KnowledgePointRepository;
import com.meridian.governance.ClassificationPolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/knowledge-points")
@RequiredArgsConstructor
public class KnowledgePointController {

    private final KnowledgePointRepository repo;
    private final CourseRepository courseRepository;
    private final ClassificationPolicy classificationPolicy;

    @GetMapping
    public ResponseEntity<List<KnowledgePointDto>> list(@PathVariable UUID courseId) {
        ensureExists(courseId);
        return ResponseEntity.ok(repo.findByCourseId(courseId).stream()
                .map(kp -> new KnowledgePointDto(kp.getId(), kp.getCourseId(), kp.getName(), kp.getDescription()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<KnowledgePointDto> create(@PathVariable UUID courseId,
                                                    @Valid @RequestBody KnowledgePointRequest req,
                                                    Authentication auth) {
        if (!classificationPolicy.canModify(extractRole(auth))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
        ensureExists(courseId);
        KnowledgePoint kp = new KnowledgePoint();
        kp.setCourseId(courseId);
        kp.setName(req.name());
        kp.setDescription(req.description());
        kp = repo.save(kp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KnowledgePointDto(kp.getId(), kp.getCourseId(), kp.getName(), kp.getDescription()));
    }

    private void ensureExists(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("STUDENT");
    }
}
