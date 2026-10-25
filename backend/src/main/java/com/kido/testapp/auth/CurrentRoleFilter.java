package com.kido.testapp.auth;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The guarantee behind "a role change takes effect immediately" (design.md §5): every authenticated
 * request compares the role stored in the session with the role in the database. On a mismatch — or if
 * the account no longer exists — the session is invalidated and the request continues as anonymous,
 * so protected endpoints answer 401 and the user logs in again with fresh authorities.
 *
 * <p>Session sweeps on role change are only a courtesy; this filter closes every race they leave
 * (e.g. a login that loaded the old role just before the change committed). Cost: one indexed query
 * per authenticated request. Not a Spring bean on purpose — Boot would also register it as a servlet filter.
 */
public class CurrentRoleFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CurrentRoleFilter.class);

    private final AppUserRepository repository;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public CurrentRoleFilter(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !trustResolver.isAnonymous(authentication)) {
            Optional<Role> current = repository.findRoleByEmail(authentication.getName());
            boolean stale = current.isEmpty() || authentication.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_" + current.get().name()));
            if (stale) {
                log.info("Session of {} no longer matches the stored role; signing it out", authentication.getName());
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}
