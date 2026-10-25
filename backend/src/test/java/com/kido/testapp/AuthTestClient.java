package com.kido.testapp;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Drives the real API like the SPA does: JSON bodies, the SESSION cookie, and a SpaCsrf token.
 * Tests create their own users with unique emails, so they never depend on each other's data.
 */
public final class AuthTestClient {

    public static final String PASSWORD = "Passw0rd!";

    private final MockMvc mvc;
    private final ObjectMapper json;

    public AuthTestClient(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    public static String uniqueEmail() {
        return "u-" + UUID.randomUUID() + "@test.local";
    }

    /** Signs up a fresh USER (which logs them in) and returns the SESSION cookie. */
    public Cookie signup(String email) throws Exception {
        return post("/api/auth/signup", Map.of("email", email, "password", PASSWORD, "confirmPassword", PASSWORD), null)
                .andReturn().getResponse().getCookie("SESSION");
    }

    /** Logs in and returns the SESSION cookie (null if the login failed). */
    public Cookie login(String email, String password) throws Exception {
        return post("/api/auth/login", Map.of("email", email, "password", password, "rememberMe", false), null)
                .andReturn().getResponse().getCookie("SESSION");
    }

    public ResultActions getAs(Cookie session, String path) throws Exception {
        return mvc.perform(withSession(MockMvcRequestBuilders.get(path), session));
    }

    public ResultActions put(String path, Object body, Cookie session) throws Exception {
        return send(MockMvcRequestBuilders.put(path), body, session);
    }

    public ResultActions post(String path, Object body, Cookie session) throws Exception {
        return send(MockMvcRequestBuilders.post(path), body, session);
    }

    /** A mutating request that carries no CSRF token at all. */
    public ResultActions putWithoutCsrf(String path, Object body, Cookie session) throws Exception {
        return mvc.perform(withSession(json(MockMvcRequestBuilders.put(path), body), session));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Object body, Cookie session) throws Exception {
        return mvc.perform(withSession(json(request, body), session).with(SpaCsrf.token(mvc)));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }

    private static MockHttpServletRequestBuilder withSession(MockHttpServletRequestBuilder request, Cookie session) {
        return session == null ? request : request.cookie(session);
    }
}
