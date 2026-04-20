package com.meridian.courses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.common.web.PageResponse;
import com.meridian.courses.dto.*;
import com.meridian.courses.entity.*;
import com.meridian.courses.repository.*;
import com.meridian.governance.ClassificationPolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepository;
    private final CohortRepository cohortRepository;
    private final AssessmentItemRepository assessmentItemRepository;
    private final ClassificationPolicy classificationPolicy;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<PageResponse<CourseDto>> list(
            @RequestParam(required = false) String version,
            @RequestParam(required = false) UUID location,
            @RequestParam(required = false) UUID instructor,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication auth) {

        size = Math.min(size, 200);
        String role = extractRole(auth);
        String classificationFilter = classificationPolicy.canView("CONFIDENTIAL", role) ? null : "PUBLIC";

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> result = courseRepository.findFiltered(version, location, instructor, classificationFilter, q, pageable);

        List<CourseDto> items = result.getContent().stream()
                .filter(c -> classificationPolicy.canView(c.getClassification(), role))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(items, page, size, result.getTotalElements()));
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CourseRequest req, Authentication auth) {
        requireCanModify(auth);
        Course course = new Course();
        applyRequest(course, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(courseRepository.save(course)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> update(@PathVariable UUID id,
                                            @Valid @RequestBody CourseRequest req,
                                            Authentication auth) {
        requireCanModify(auth);
        Course course = courseRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        applyRequest(course, req);
        return ResponseEntity.ok(toDto(courseRepository.save(course)));
    }

    @GetMapping("/{id}/cohorts")
    public ResponseEntity<List<CohortDto>> cohorts(@PathVariable UUID id) {
        ensureCourseExists(id);
        return ResponseEntity.ok(cohortRepository.findByCourseId(id).stream()
                .map(c -> new CohortDto(c.getId(), c.getCourseId(), c.getName(),
                        c.getTotalSeats(), c.getStartsAt(), c.getEndsAt()))
                .toList());
    }

    @GetMapping("/{id}/assessment-items")
    public ResponseEntity<PageResponse<AssessmentItemDto>> items(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        size = Math.min(size, 200);
        ensureCourseExists(id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<AssessmentItem> result = assessmentItemRepository.findByCourseIdAndDeletedAtIsNull(id, pageable);
        List<AssessmentItemDto> items = result.getContent().stream().map(this::toItemDto).toList();
        return ResponseEntity.ok(new PageResponse<>(items, page, size, result.getTotalElements()));
    }

    private void ensureCourseExists(UUID id) {
        if (!courseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

    private void requireCanModify(Authentication auth) {
        if (!classificationPolicy.canModify(extractRole(auth))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
    }

    private void applyRequest(Course course, CourseRequest req) {
        course.setCode(req.code());
        course.setTitle(req.title());
        course.setVersion(req.version());
        course.setLocationId(req.locationId());
        course.setInstructorId(req.instructorId());
        course.setClassification(req.classification() != null ? req.classification() : "INTERNAL");
    }

    private CourseDto toDto(Course c) {
        return new CourseDto(c.getId(), c.getCode(), c.getTitle(), c.getVersion(),
                c.getLocationId(), c.getInstructorId(), c.getClassification(), c.getCreatedAt());
    }

    private AssessmentItemDto toItemDto(AssessmentItem item) {
        Object choices = null;
        if (item.getChoices() != null) {
            try {
                choices = objectMapper.readValue(item.getChoices(), Object.class);
            } catch (JsonProcessingException e) {
                choices = item.getChoices();
            }
        }
        return new AssessmentItemDto(item.getId(), item.getCourseId(), item.getKnowledgePointId(),
                item.getType(), item.getDifficulty(), item.getDiscrimination(), item.getStem(), choices);
    }

    private String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
    }
}
