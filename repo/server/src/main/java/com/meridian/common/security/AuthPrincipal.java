package com.meridian.common.security;

import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.UUID;

public record AuthPrincipal(UUID userId, String role, UUID organizationId) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }

    public static AuthPrincipal of(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof AuthPrincipal ap) return ap;
        UUID userId = UUID.fromString(auth.getName());
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
        return new AuthPrincipal(userId, role, null);
    }

    public static UUID userId(Authentication auth) {
        return of(auth).userId();
    }

    public static String role(Authentication auth) {
        return of(auth).role();
    }

    public static UUID orgId(Authentication auth) {
        return of(auth).organizationId();
    }
}
