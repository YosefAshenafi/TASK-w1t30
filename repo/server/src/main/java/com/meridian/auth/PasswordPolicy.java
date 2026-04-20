package com.meridian.auth;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SYMBOL = Pattern.compile("[^A-Za-z0-9]");

    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) return false;
        return HAS_DIGIT.matcher(password).find() && HAS_SYMBOL.matcher(password).find();
    }

    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new PasswordPolicyException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            throw new PasswordPolicyException("Password must contain at least one digit");
        }
        if (!HAS_SYMBOL.matcher(password).find()) {
            throw new PasswordPolicyException("Password must contain at least one symbol");
        }
    }

    public static class PasswordPolicyException extends IllegalArgumentException {
        public PasswordPolicyException(String message) { super(message); }
    }
}
