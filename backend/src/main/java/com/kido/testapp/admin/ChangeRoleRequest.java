package com.kido.testapp.admin;

import com.kido.testapp.user.Role;
import jakarta.validation.constraints.NotNull;

/** {"role": "ADMIN" | "USER"}. SUPER_ADMIN deserialises but is rejected by AdminUserService (design.md §5). */
public record ChangeRoleRequest(@NotNull Role role) {
}
