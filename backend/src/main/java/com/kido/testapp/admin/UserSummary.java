package com.kido.testapp.admin;

import java.time.Instant;
import java.util.UUID;

import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.Role;

/** A row of the user-management list (design.md §7). No bio, never the hash. */
public record UserSummary(UUID id, String email, String username, Role role, Instant createdAt) {

    public static UserSummary from(AppUser user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
