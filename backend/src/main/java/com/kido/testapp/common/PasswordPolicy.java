package com.kido.testapp.common;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Single source of the password rule (design.md §8): 8–72 chars, at least one letter and one digit.
 * 72 is BCrypt's limit — it silently ignores bytes beyond it, so the byte length is checked too.
 * Request DTOs use {@link #REGEX}/{@link #MIN}/{@link #MAX} in Bean Validation; other code calls {@link #violation}.
 */
public final class PasswordPolicy {

    public static final int MIN = 8;
    public static final int MAX = 72;
    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\d).+$";
    public static final String MESSAGE = "must be 8-72 characters with at least one letter and one digit";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private PasswordPolicy() {
    }

    /** @return null if the password is acceptable, otherwise a message (never containing the password). */
    public static String violation(String password) {
        if (password == null
                || password.length() < MIN
                || password.getBytes(StandardCharsets.UTF_8).length > MAX
                || !PATTERN.matcher(password).matches()) {
            return MESSAGE;
        }
        return null;
    }
}
