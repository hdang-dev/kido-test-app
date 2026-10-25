package com.kido.testapp.user;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
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
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SpringSessionRememberMeServices rememberMeServices;
    private final Duration defaultIdleTimeout;

    public MeController(UserService userService, SessionAuthenticationStrategy sessionAuthenticationStrategy,
                        SpringSessionRememberMeServices rememberMeServices,
                        @Value("${spring.session.timeout}") Duration defaultIdleTimeout) {
        this.userService = userService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.rememberMeServices = rememberMeServices;
        this.defaultIdleTimeout = defaultIdleTimeout;
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

    /**
     * On success every other session of this user is revoked (in the service transaction), then this
     * session is rotated exactly like at login — new session ID and new CSRF token — keeping "remember me".
     */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserDetails principal,
                               @Valid @RequestBody ChangePasswordRequest body, Authentication authentication,
                               HttpServletRequest request, HttpServletResponse response) {
        userService.changePassword(principal.getUsername(), body.currentPassword(), body.newPassword(),
                body.confirmPassword(), request.getSession().getId());

        boolean remembered = request.getSession().getMaxInactiveInterval() > defaultIdleTimeout.toSeconds();
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        if (remembered) {
            rememberMeServices.loginSuccess(request, response, authentication); // keep the persistent cookie
        }
    }
}
