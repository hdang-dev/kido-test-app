package com.kido.testapp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.kido.testapp.common.ApiException;
import com.kido.testapp.user.Emails;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.stereotype.Component;

/**
 * Establishes an authenticated session from a controller, doing explicitly what Spring Security's
 * formLogin filter would (design.md §6), in the same order as AbstractAuthenticationProcessingFilter:
 * <ol>
 *   <li>SessionAuthenticationStrategy — new session ID (anti-fixation) and a fresh CSRF token;</li>
 *   <li>store the SecurityContext in the session (Spring Security 6 no longer does this implicitly);</li>
 *   <li>optionally extend the session for "remember me".</li>
 * </ol>
 */
@Component
public class SessionLogin {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SpringSessionRememberMeServices rememberMeServices;

    public SessionLogin(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                        SessionAuthenticationStrategy sessionAuthenticationStrategy,
                        SecurityContextRepository securityContextRepository,
                        SpringSessionRememberMeServices rememberMeServices) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.rememberMeServices = rememberMeServices;
    }

    /** @return the authenticated principal's name (the normalised email). Wrong email or password → 401, same message. */
    public String withPassword(String email, String password, boolean rememberMe,
                               HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(Emails.normalise(email), password));
        } catch (AuthenticationException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        establish(authentication, rememberMe, request, response);
        return authentication.getName();
    }

    /** Sign-up logs the new user straight in (design.md §6); the password was just set, so no re-check. */
    public void withoutPassword(String email, HttpServletRequest request, HttpServletResponse response) {
        UserDetails user = userDetailsService.loadUserByUsername(email);
        establish(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()),
                false, request, response);
    }

    private void establish(Authentication authentication, boolean rememberMe,
                           HttpServletRequest request, HttpServletResponse response) {
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        if (rememberMe) {
            rememberMeServices.loginSuccess(request, response, authentication);
        }
    }
}
