package com.kido.testapp.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account. Identity always comes from the session, never from the request. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final UserSessions userSessions;

    public MeController(UserService userService, UserSessions userSessions) {
        this.userService = userService;
        this.userSessions = userSessions;
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

    /** On success: every other session of this user is expired, and this one gets a new ID. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserDetails principal,
                               @Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        String email = principal.getUsername();
        userService.changePassword(email, body.currentPassword(), body.newPassword(), body.confirmPassword());
        userSessions.expireAllExcept(email, request.getSession().getId());
        request.changeSessionId();
    }
}
