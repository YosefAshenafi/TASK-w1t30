package com.meridian.courses;

import com.meridian.courses.dto.ActivityDto;
import com.meridian.courses.dto.ActivityRequest;
import com.meridian.courses.entity.Activity;
import com.meridian.courses.repository.ActivityRepository;
import com.meridian.courses.repository.CourseRepository;
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
@RequestMapping("/api/v1/courses/{courseId}/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityRepository repo;
    private final CourseRepository courseRepository;
    private final ClassificationPolicy classificationPolicy;

    @GetMapping
    public ResponseEntity<List<ActivityDto>> list(@PathVariable UUID courseId) {
        ensureExists(courseId);
        return ResponseEntity.ok(repo.findByCourseIdOrderBySortOrder(courseId).stream()
                .map(a -> new ActivityDto(a.getId(), a.getCourseId(), a.getName(), a.getDescription(), a.getSortOrder()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<ActivityDto> create(@PathVariable UUID courseId,
                                              @Valid @RequestBody ActivityRequest req,
                                              Authentication auth) {
        if (!classificationPolicy.canModify(extractRole(auth))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
        ensureExists(courseId);
        Activity a = new Activity();
        a.setCourseId(courseId);
        a.setName(req.name());
        a.setDescription(req.description());
        a.setSortOrder(req.sortOrder());
        a = repo.save(a);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ActivityDto(a.getId(), a.getCourseId(), a.getName(), a.getDescription(), a.getSortOrder()));
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
