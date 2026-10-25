package com.kido.testapp.user;

import com.kido.testapp.common.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/** design.md §6/§7. Cross-field rules (current is correct, confirmation, differs) live in UserService. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @ValidPassword String newPassword,
        @NotBlank String confirmPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[***]"; // never log passwords
    }
}
