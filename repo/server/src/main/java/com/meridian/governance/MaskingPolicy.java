package com.meridian.governance;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default-on masking for employee/contact identifiers.
 *
 * Unmasking is permitted only when the viewer has an elevated role
 * (ADMIN, FACULTY_MENTOR) or is looking up their own record, or a
 * corporate mentor querying within their own organization scope.
 */
@Component
public class MaskingPolicy {

    public boolean canUnmask(String viewerRole, UUID viewerId, UUID viewerOrgId,
                             UUID targetUserId, UUID targetOrgId) {
        if ("ADMIN".equals(viewerRole) || "FACULTY_MENTOR".equals(viewerRole)) return true;
        if (viewerId != null && viewerId.equals(targetUserId)) return true;
        if ("CORPORATE_MENTOR".equals(viewerRole) && viewerOrgId != null && viewerOrgId.equals(targetOrgId)) {
            return true;
        }
        return false;
    }

    public String maskUsername(String username) {
        if (username == null || username.isBlank()) return username;
        int len = username.length();
        if (len <= 2) return "*".repeat(len);
        return username.charAt(0) + "***" + username.charAt(len - 1);
    }

    public String maskDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return displayName;
        String[] parts = displayName.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(maskWord(parts[i]));
        }
        return sb.toString();
    }

    public String maskEmail(String email) {
        if (email == null || email.isBlank()) return email;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        char first = local.charAt(0);
        return first + "***" + domain;
    }

    public String apply(String value, boolean canUnmask) {
        return canUnmask ? value : maskUsername(value);
    }

    public String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.length() <= 4) return "***";
        return digits.substring(0, 2) + "***" + digits.substring(digits.length() - 2);
    }

    /**
     * Applies the appropriate masking strategy based on the column/field name.
     * Centralises the field-level masking matrix so all export/serialization
     * paths stay consistent.
     */
    public String maskField(String fieldName, String value) {
        if (value == null) return null;
        return switch (fieldName.toLowerCase()) {
            case "email" -> maskEmail(value);
            case "display_name", "displayname", "full_name", "fullname" -> maskDisplayName(value);
            case "phone", "phone_number", "phonenumber", "mobile" -> maskPhone(value);
            default -> maskUsername(value);
        };
    }

    private String maskWord(String word) {
        if (word.length() <= 1) return word;
        return word.charAt(0) + "***";
    }
}
