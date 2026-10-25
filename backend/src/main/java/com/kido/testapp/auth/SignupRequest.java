package com.kido.testapp.auth;

import com.kido.testapp.common.MaxLength;
import com.kido.testapp.common.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** design.md §7/§8. username and bio are optional; a blank username becomes the email's local part. */
public record SignupRequest(
        @NotBlank @Size(max = 254)
        @Email(regexp = ".+@.+\\..+", message = "must be a valid email address") // also require a dot in the domain
        String email,
        @ValidPassword String password,
        @NotBlank String confirmPassword,
        @MaxLength(50) String username,
        @MaxLength(500) String bio) {

    @Override
    public String toString() {
        return "SignupRequest{email='" + email + "'}"; // never log passwords
    }
}
