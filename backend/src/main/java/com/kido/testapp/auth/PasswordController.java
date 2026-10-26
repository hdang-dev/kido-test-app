package com.kido.testapp.auth;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.kido.testapp.user.ChangePasswordRequest;
import com.kido.testapp.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service password change (design.md §6). Lives in auth/ next to SessionLogin because, besides the
 * user update, it re-establishes the caller's session.
 */
@RestController
public class PasswordController {

    private final UserService userService;
    private final SessionLogin sessionLogin;

    public PasswordController(UserService userService, SessionLogin sessionLogin) {
        this.userService = userService;
        this.sessionLogin = sessionLogin;
    }

    /** Other sessions of this user are revoked; this one is rotated and stays signed in. */
    @PutMapping("/api/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body, Authentication authentication,
                               HttpServletRequest request, HttpServletResponse response) {
        Instant newCredentialVersion = userService.changePassword(authentication.getName(), body.currentPassword(),
                body.newPassword(), body.confirmPassword(), request.getSession().getId());
        sessionLogin.afterPasswordChange(authentication, newCredentialVersion, request, response);
    }
}
