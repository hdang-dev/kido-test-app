package com.kido.testapp.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.super-admin.*} from APP_SUPER_ADMIN_EMAIL / APP_SUPER_ADMIN_PASSWORD (design.md §5).
 * Validated at startup: an unset env var leaves the literal "${...}" placeholder, which fails @Email.
 */
@Validated
@ConfigurationProperties("app.super-admin")
public record SuperAdminProperties(@NotBlank @Email String email, @NotBlank String password) {

    @Override
    public String toString() {
        return "SuperAdminProperties{email='" + email + "', password=***}";
    }
}
