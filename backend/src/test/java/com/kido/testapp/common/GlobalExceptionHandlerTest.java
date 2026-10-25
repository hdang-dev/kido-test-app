package com.kido.testapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void springMvcClientErrorsKeepTheir4xxStatus() throws Exception {
        var response = handler.handleException(
                new HttpRequestMethodNotSupportedException("PUT", List.of(HttpMethod.POST.name())),
                new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isEqualTo(ErrorResponse.of("Method Not Allowed"));
    }

    @Test
    void methodSecurityDenialIs401ForAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(handler.accessDenied().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void methodSecurityDenialIs403ForAuthenticatedUsers() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "u@test.local", null, AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThat(handler.accessDenied().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
