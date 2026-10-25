package com.kido.testapp.admin;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User management API (design.md §7). Access: /api/admin/** URL rule + @PreAuthorize on the service. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<UserSummary> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(adminUserService.list(page, size), UserSummary::from);
    }

    @PutMapping("/{id}/role")
    public UserSummary changeRole(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
                                  @Valid @RequestBody ChangeRoleRequest body) {
        return UserSummary.from(adminUserService.changeRole(principal.getUsername(), id, body.role()));
    }
}
