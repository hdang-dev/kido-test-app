package com.kido.testapp.auth;

import jakarta.validation.constraints.NotBlank;

/** No format/policy checks here: any mismatch is just "Invalid email or password". */
public record LoginRequest(@NotBlank String email, @NotBlank String password, boolean rememberMe) {

    @Override
    public String toString() {
        return "LoginRequest{email='" + email + "', rememberMe=" + rememberMe + "}";
    }
}
