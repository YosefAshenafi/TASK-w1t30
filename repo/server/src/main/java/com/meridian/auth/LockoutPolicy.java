package com.meridian.auth;

import com.meridian.auth.entity.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class LockoutPolicy {

    public static final int MAX_FAILURES = 5;
    public static final int LOCKOUT_MINUTES = 15;

    private LockoutPolicy() {}

    public static boolean isLockedOut(User user) {
        if (!"LOCKED".equals(user.getStatus())) return false;
        if (user.getLockedUntil() == null) return true;
        return Instant.now().isBefore(user.getLockedUntil());
    }

    public static void clearExpiredLock(User user) {
        if ("LOCKED".equals(user.getStatus()) && user.getLockedUntil() != null
                && Instant.now().isAfter(user.getLockedUntil())) {
            user.setStatus("ACTIVE");
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }
    }

    public static void recordFailure(User user) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= MAX_FAILURES) {
            user.setStatus("LOCKED");
            user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }
    }

    public static void resetOnSuccess(User user) {
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if ("LOCKED".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
        }
    }
}
