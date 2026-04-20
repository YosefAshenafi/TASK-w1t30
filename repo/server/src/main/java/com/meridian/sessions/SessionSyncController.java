package com.meridian.sessions;

import com.meridian.sessions.dto.SyncRequest;
import com.meridian.sessions.dto.SyncResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/sync")
@RequiredArgsConstructor
public class SessionSyncController {

    private final SyncResolver syncResolver;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<SyncResult> sync(@Valid @RequestBody SyncRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(syncResolver.resolve(req, userId));
    }
}
