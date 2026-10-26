package com.kido.testapp.user;

import java.time.Instant;

/** The parts of a user that a session snapshot must still match: role and credential version. */
public record AuthState(Role role, Instant passwordChangedAt) {
}
