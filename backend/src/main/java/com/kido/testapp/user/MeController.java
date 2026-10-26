package com.kido.testapp.user;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own account. Identity always comes from the session, never from the request.
 * (Password change is auth/PasswordController: it also re-establishes the session.)
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal UserDetails principal) {
        return MeResponse.from(userService.getByEmail(principal.getUsername()));
    }

    @PutMapping
    public MeResponse updateProfile(@AuthenticationPrincipal UserDetails principal,
                                    @Valid @RequestBody UpdateProfileRequest body) {
        return MeResponse.from(userService.updateProfile(principal.getUsername(), body.username(), body.bio()));
    }
}
