package com.kido.testapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import com.kido.testapp.IntegrationTest;
import com.kido.testapp.SpaCsrf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Filter-chain rules from design.md D1–D3 and §5, independent of any controller. */
@IntegrationTest
class SecurityConfigTest {

    @Autowired MockMvc mvc;

    @Test
    void anonymousApiCallGetsJson401NotRedirect() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void anonymousRequestDoesNotCreateASession() throws Exception {
        var response = mvc.perform(get("/api/me")).andReturn().getResponse();
        assertThat(response.getCookie("SESSION")).isNull();
    }

    @Test
    void anyResponseIssuesReadableCsrfCookie() throws Exception {
        Cookie cookie = mvc.perform(get("/api/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isFalse(); // the SPA must be able to read it
    }

    @Test
    @WithMockUser
    void mutatingRequestWithoutCsrfTokenIs403() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void spaFlowCookieEchoedInHeaderIsAccepted() throws Exception {
        mvc.perform(post("/api/auth/logout").with(SpaCsrf.token(mvc)))
                .andExpect(status().isNoContent());
    }

    @Test
    void spaFlowWrongHeaderValueIsRejected() throws Exception {
        Cookie token = mvc.perform(get("/api/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/auth/logout").cookie(token).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userIsForbiddenFromAdminApi() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    /** 404 = passed authorization; the endpoint itself arrives in phase 4. */
    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPER_ADMIN"})
    void adminsPassAdminApiAuthorization(String role) throws Exception {
        mvc.perform(get("/api/admin/users").with(user("a@test.local").roles(role)))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginAndSignupArePublic() throws Exception {
        // 400 (missing body), not 401: the endpoints are reachable anonymously
        mvc.perform(post("/api/auth/login").with(SpaCsrf.token(mvc))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/signup").with(SpaCsrf.token(mvc))).andExpect(status().isBadRequest());
    }

    @Test
    void otherEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized()); // only POST is public
        mvc.perform(get("/anything")).andExpect(status().isUnauthorized());
    }
}
