package com.kido.testapp.user;

import java.util.Locale;

/** Email is the identity (design.md D5); every lookup and insert goes through {@link #normalise}. */
public final class Emails {

    private Emails() {
    }

    public static String normalise(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Default display name: the part before '@' (design.md §6), capped to the column size. */
    public static String defaultUsername(String normalisedEmail) {
        String local = normalisedEmail.substring(0, normalisedEmail.indexOf('@'));
        return local.length() > 50 ? local.substring(0, 50) : local;
    }
}
