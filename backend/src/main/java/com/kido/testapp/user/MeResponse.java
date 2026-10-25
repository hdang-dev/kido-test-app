package com.kido.testapp.user;

import java.util.UUID;

/** The signed-in user's own view (design.md §7). Never includes the password hash. */
public record MeResponse(UUID id, String email, String username, String bio, Role role) {

    public static MeResponse from(AppUser user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getUsername(), user.getBio(), user.getRole());
    }
}
