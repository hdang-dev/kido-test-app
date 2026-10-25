package com.kido.testapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.http.Cookie;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Real SPA CSRF handshake for MockMvc: fetch the XSRF-TOKEN cookie, then send it back as cookie + header,
 * exactly like frontend/src/api/client.ts.
 *
 * <p>Use this instead of spring-security-test's {@code csrf()}: that post-processor swaps the token
 * repository inside the shared CsrfFilter, which stops real XSRF-TOKEN cookies from being issued for
 * every later test in the same (cached) context.
 */
public final class SpaCsrf {

    private SpaCsrf() {
    }

    public static RequestPostProcessor token(MockMvc mvc) throws Exception {
        Cookie cookie = mvc.perform(get("/api/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new IllegalStateException("No XSRF-TOKEN cookie issued — was csrf() used somewhere?");
        }
        return request -> {
            request.setCookies(cookie);
            request.addHeader("X-XSRF-TOKEN", cookie.getValue());
            return request;
        };
    }
}
