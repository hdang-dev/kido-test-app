package com.kido.testapp.user;

import jakarta.validation.constraints.Size;

/** design.md §7. A blank username resets it to the default (email local part), as at sign-up. */
public record UpdateProfileRequest(@Size(max = 50) String username, @Size(max = 500) String bio) {
}
