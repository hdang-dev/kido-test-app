package com.kido.testapp.common;

import java.util.regex.Pattern;

/**
 * Normalisation for user-entered display text (username, bio). String.strip() only removes
 * Character.isWhitespace, which excludes no-break spaces (U+00A0, U+202F, ...) — so "  " made of
 * NBSPs would count as text. Here every Unicode space separator (\p{Z}) counts as blank.
 */
public final class Text {

    private static final Pattern EDGE_SPACE = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    private Text() {
    }

    /** Trims ASCII and Unicode spaces from both ends. Null stays null. */
    public static String trim(String value) {
        return value == null ? null : EDGE_SPACE.matcher(value).replaceAll("");
    }

    /** True if null, empty, or only (Unicode) spaces. */
    public static boolean isBlank(String value) {
        return value == null || trim(value).isEmpty();
    }

    /**
     * Control characters (NUL, ESC, ...) are never valid display text, and Postgres rejects NUL outright.
     * Tab and line breaks are allowed only where the field is multi-line (bio).
     */
    public static boolean hasControlChars(String value, boolean allowLineBreaks) {
        return value != null && value.codePoints().anyMatch(c -> Character.getType(c) == Character.CONTROL
                && !(allowLineBreaks && (c == '\n' || c == '\r' || c == '\t')));
    }
}
