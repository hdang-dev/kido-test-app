package com.kido.testapp.common;

import java.nio.charset.StandardCharsets;

/**
 * Single source of the password rule (design.md §8): at least 8 characters, at most 72 UTF-8 bytes
 * (BCrypt's hard limit — Spring's encoder throws beyond it), at least one letter and one digit.
 * Request DTOs use {@link ValidPassword}, which delegates here; never re-express the rule with @Size/@Pattern
 * (@Size counts UTF-16 chars, not bytes).
 */
public final class PasswordPolicy {

    public static final int MIN_CHARS = 8;
    public static final int MAX_BYTES = 72;
    public static final String MESSAGE = "must be 8-72 characters with at least one letter and one digit";

    private PasswordPolicy() {
    }

    /** @return null if the password is acceptable, otherwise a message (never containing the password). */
    public static String violation(String password) {
        if (password == null
                || password.codePointCount(0, password.length()) < MIN_CHARS
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES
                || password.codePoints().noneMatch(Character::isLetter)
                || password.codePoints().noneMatch(Character::isDigit)) {
            return MESSAGE;
        }
        return null;
    }
}
