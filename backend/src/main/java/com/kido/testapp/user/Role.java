package com.kido.testapp.user;

/** Hierarchy SUPER_ADMIN > ADMIN > USER is configured in SecurityConfig (design.md §5). */
public enum Role {
    USER,
    ADMIN,
    SUPER_ADMIN
}
