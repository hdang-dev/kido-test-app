package com.kido.testapp.user;

import com.kido.testapp.common.MaxLength;
import com.kido.testapp.common.PlainText;
import jakarta.validation.constraints.NotNull;

/**
 * design.md §7. Full replacement: both fields are required so a partial body can't silently wipe the
 * other one. A blank username resets it to the default (email local part); a blank bio clears it.
 */
public record UpdateProfileRequest(@NotNull @MaxLength(50) @PlainText String username,
                                   @NotNull @MaxLength(500) @PlainText(multiline = true) String bio) {
}
