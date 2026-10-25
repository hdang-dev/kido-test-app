package com.kido.testapp.user;

import java.util.Locale;

/** Email is the identity (design.md D5); every lookup and insert goes through {@link #normalise}. */
public final class Emails {

    private static final int MAX_USERNAME_CODE_POINTS = 50;

    private Emails() {
    }

    public static String normalise(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Default display name: the local part (design.md §6), capped to the column size.
     * Splits at the last '@' (a quoted local part may contain '@') and truncates by code point so an
     * emoji is never cut in half.
     */
    public static String defaultUsername(String normalisedEmail) {
        String local = normalisedEmail.substring(0, normalisedEmail.lastIndexOf('@'));
        if (local.codePointCount(0, local.length()) <= MAX_USERNAME_CODE_POINTS) {
            return local;
        }
        return local.substring(0, local.offsetByCodePoints(0, MAX_USERNAME_CODE_POINTS));
    }
}
