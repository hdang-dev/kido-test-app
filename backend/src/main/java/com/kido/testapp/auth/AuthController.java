package com.kido.testapp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.MeResponse;
import com.kido.testapp.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign-up and login (design.md §6, §7). Logout is handled by Spring Security's LogoutFilter. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final SessionLogin sessionLogin;

    public AuthController(UserService userService, SessionLogin sessionLogin) {
        this.userService = userService;
        this.sessionLogin = sessionLogin;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse signup(@Valid @RequestBody SignupRequest body,
                             HttpServletRequest request, HttpServletResponse response) {
        AppUser user = userService.register(body.email(), body.password(), body.confirmPassword(),
                body.username(), body.bio());
        sessionLogin.withoutPassword(user, request, response);
        return MeResponse.from(user);
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest body,
                            HttpServletRequest request, HttpServletResponse response) {
        String email = sessionLogin.withPassword(body.email(), body.password(), body.rememberMe(), request, response);
        return MeResponse.from(userService.getByEmail(email));
    }
}
