package com.meridian.governance;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ClassificationPolicy {

    private static final Set<String> SENSITIVE = Set.of("CONFIDENTIAL", "RESTRICTED");

    public boolean canView(String classification, String role) {
        if (!SENSITIVE.contains(classification)) return true;
        return "ADMIN".equals(role) || "FACULTY_MENTOR".equals(role);
    }

    public boolean canModify(String role) {
        return "ADMIN".equals(role) || "FACULTY_MENTOR".equals(role);
    }
}
