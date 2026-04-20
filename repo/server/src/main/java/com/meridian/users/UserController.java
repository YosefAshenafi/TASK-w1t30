package com.meridian.users;

import com.meridian.auth.dto.UserProfileDto;
import com.meridian.auth.entity.User;
import com.meridian.auth.repository.UserRepository;
import com.meridian.security.repository.AllowedIpRangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AllowedIpRangeRepository allowedIpRangeRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMe(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<String> ipRanges = allowedIpRangeRepository.findCidrsByRole(user.getRole());
        return ResponseEntity.ok(new UserProfileDto(
                user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getStatus(), user.getOrganizationId(),
                ipRanges, user.getLastLoginAt(), user.getCreatedAt()));
    }
}
