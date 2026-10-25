package com.kido.testapp.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kido.testapp.common.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Session-cookie auth for the SPA (design.md D1–D3, §5). The API only ever answers in JSON:
 * no login page, no redirects, no HTTP Basic.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // SUPER_ADMIN via the hierarchy
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, objectMapper, HttpStatus.UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, objectMapper, HttpStatus.FORBIDDEN, "Access denied")))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                // No "redirect back after login" for a JSON API — and saving the request would create a
                // session row (Spring Session JDBC) for every anonymous 401.
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout") // POST only, CSRF-protected
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** SUPER_ADMIN > ADMIN > USER — used by URL rules and @PreAuthorize alike. */
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .role("ADMIN").implies("USER")
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt by default
    }

    private static void writeError(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String message)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getOutputStream(), ErrorResponse.of(message));
    }
}
