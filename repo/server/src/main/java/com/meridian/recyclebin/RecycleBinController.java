package com.meridian.recyclebin;

import com.meridian.auth.entity.User;
import com.meridian.auth.repository.UserRepository;
import com.meridian.common.web.PageResponse;
import com.meridian.courses.entity.Course;
import com.meridian.courses.repository.CourseRepository;
import com.meridian.security.audit.AuditEvent;
import com.meridian.security.audit.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/recycle-bin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RecycleBinController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditEventRepository auditEventRepository;

    @GetMapping
    public ResponseEntity<PageResponse<RecycleBinEntry>> list(
            @RequestParam(defaultValue = "courses") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<RecycleBinEntry> entries = switch (type) {
            case "users" -> userRepository.findAll().stream()
                    .filter(u -> u.getDeletedAt() != null)
                    .map(u -> new RecycleBinEntry(u.getId(), "users", u.getUsername(), u.getDeletedAt()))
                    .toList();
            default -> courseRepository.findAll().stream()
                    .filter(c -> c.getDeletedAt() != null)
                    .map(c -> new RecycleBinEntry(c.getId(), "courses", c.getTitle(), c.getDeletedAt()))
                    .toList();
        };

        return ResponseEntity.ok(new PageResponse<>(entries, page, size, entries.size()));
    }

    @PostMapping("/{type}/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String type,
                                        @PathVariable UUID id,
                                        Authentication auth) {
        UUID actorId = UUID.fromString(auth.getName());
        switch (type) {
            case "users" -> {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                user.setDeletedAt(null);
                user.setDeletedBy(null);
                userRepository.save(user);
            }
            case "courses" -> {
                Course course = courseRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
                course.setDeletedAt(null);
                course.setDeletedBy(null);
                courseRepository.save(course);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown type: " + type);
        }
        auditEventRepository.save(AuditEvent.of(actorId, "DATA_RESTORE", type, id.toString(), "{}"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable String type,
                                           @PathVariable UUID id,
                                           Authentication auth) {
        UUID actorId = UUID.fromString(auth.getName());
        switch (type) {
            case "users" -> userRepository.deleteById(id);
            case "courses" -> courseRepository.deleteById(id);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown type: " + type);
        }
        auditEventRepository.save(AuditEvent.of(actorId, "DATA_DELETE", type, id.toString(), "{}"));
        return ResponseEntity.noContent().build();
    }

    record RecycleBinEntry(UUID id, String type, String label, Instant deletedAt) {}
}
