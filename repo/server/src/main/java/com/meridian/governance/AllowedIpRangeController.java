package com.meridian.governance;

import com.meridian.security.entity.AllowedIpRange;
import com.meridian.security.repository.AllowedIpRangeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/allowed-ip-ranges")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AllowedIpRangeController {

    private final AllowedIpRangeRepository repo;

    @GetMapping
    public ResponseEntity<List<AllowedIpRangeDto>> list() {
        return ResponseEntity.ok(repo.findAll().stream().map(AllowedIpRangeDto::from).toList());
    }

    @PostMapping
    public ResponseEntity<AllowedIpRangeDto> create(@Valid @RequestBody CreateIpRangeRequest req,
                                                    Authentication auth) {
        AllowedIpRange range = new AllowedIpRange();
        range.setCidr(req.cidr());
        range.setRoleScope(req.roleScope());
        range.setNote(req.note());
        range.setCreatedBy(UUID.fromString(auth.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(AllowedIpRangeDto.from(repo.save(range)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IP range not found");
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    record AllowedIpRangeDto(UUID id, String cidr, String roleScope, String note) {
        static AllowedIpRangeDto from(AllowedIpRange r) {
            return new AllowedIpRangeDto(r.getId(), r.getCidr(), r.getRoleScope(), r.getNote());
        }
    }

    record CreateIpRangeRequest(@NotBlank String cidr, String roleScope, String note) {}
}
