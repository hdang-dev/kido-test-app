package com.kido.testapp.auth;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.AuthState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A session is a snapshot taken at login. On every authenticated request this filter checks the snapshot
 * is still true against the database (design.md D9, D10):
 * <ul>
 *   <li><b>role</b> — the role in the session must equal the stored role (role change takes effect at once);</li>
 *   <li><b>credential version</b> — the session must have been created after the last password change
 *       ({@link SessionLogin#PASSWORD_CHANGED_AT}), so no session made with an old password survives.</li>
 * </ul>
 * Account gone or snapshot stale → the session is invalidated and the request continues as anonymous,
 * so protected endpoints answer 401. Session sweeps elsewhere are only a courtesy; this filter is what
 * closes every race (e.g. a login that read the old role or old password hash just before a change committed).
 * Cost: one indexed query per authenticated request. Not a Spring bean on purpose — Boot would also
 * register it as a servlet filter.
 */
public class SessionValidityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionValidityFilter.class);

    private final AppUserRepository repository;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public SessionValidityFilter(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !trustResolver.isAnonymous(authentication)) {
            HttpSession session = request.getSession(false);
            Optional<AuthState> current = repository.findAuthStateByEmail(authentication.getName());
            if (current.isEmpty() || !matches(authentication, session, current.get())) {
                log.info("Session of {} no longer matches the stored account; signing it out", authentication.getName());
                SecurityContextHolder.clearContext();
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean matches(Authentication authentication, HttpSession session, AuthState current) {
        boolean sameRole = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + current.role().name()));
        Object stamp = session == null ? null : session.getAttribute(SessionLogin.PASSWORD_CHANGED_AT);
        boolean sameCredentials = stamp instanceof Instant instant && instant.equals(current.passwordChangedAt());
        return sameRole && sameCredentials;
    }
}
