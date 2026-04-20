package com.meridian.courses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.courses.dto.AssessmentItemDto;
import com.meridian.courses.dto.AssessmentItemRequest;
import com.meridian.courses.entity.AssessmentItem;
import com.meridian.courses.repository.AssessmentItemRepository;
import com.meridian.governance.ClassificationPolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-items")
@RequiredArgsConstructor
public class AssessmentItemController {

    private final AssessmentItemRepository repo;
    private final ClassificationPolicy classificationPolicy;
    private final ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR')")
    public ResponseEntity<AssessmentItemDto> create(@Valid @RequestBody AssessmentItemRequest req) {
        AssessmentItem item = new AssessmentItem();
        applyRequest(item, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(repo.save(item)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY_MENTOR')")
    public ResponseEntity<AssessmentItemDto> update(@PathVariable UUID id,
                                                    @Valid @RequestBody AssessmentItemRequest req) {
        AssessmentItem item = repo.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment item not found"));
        applyRequest(item, req);
        return ResponseEntity.ok(toDto(repo.save(item)));
    }

    private void applyRequest(AssessmentItem item, AssessmentItemRequest req) {
        item.setCourseId(req.courseId());
        item.setKnowledgePointId(req.knowledgePointId());
        item.setType(req.type());
        item.setStem(req.stem());
        if (req.choices() != null) {
            try {
                item.setChoices(objectMapper.writeValueAsString(req.choices()));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid choices format");
            }
        }
    }

    private AssessmentItemDto toDto(AssessmentItem item) {
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
}
