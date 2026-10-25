package com.kido.testapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kido.testapp.IntegrationTest;
import com.kido.testapp.SpaCsrf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Sign-up, login, logout and /api/me over real HTTP semantics (design.md §6, §7). */
@IntegrationTest
class AuthControllerTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SessionRepository<? extends Session> sessions;

    // ---------- helpers (MockMvc + Spring Session: the SESSION cookie is the session)

    private static String uniqueEmail() {
        return "u-" + UUID.randomUUID() + "@test.local";
    }

    private ResultActions postJson(String path, Object body, Cookie... cookies) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mvc.perform(request.with(SpaCsrf.token(mvc)));
    }

    private ResultActions signup(String email, String username, String bio) throws Exception {
        return postJson("/api/auth/signup",
                new SignupRequest(email, PASSWORD, PASSWORD, username, bio));
    }

    private MockHttpServletResponse login(String email, String password, boolean rememberMe, Cookie... cookies)
            throws Exception {
        return postJson("/api/auth/login", new LoginRequest(email, password, rememberMe), cookies)
                .andReturn().getResponse();
    }

    private ResultActions me(Cookie session) throws Exception {
        return mvc.perform(get("/api/me").cookie(session));
    }

    // ---------- sign-up

    @Test
    void signupCreatesUserAndLogsThemIn() throws Exception {
        String email = uniqueEmail();
        MockHttpServletResponse response = signup(email.toUpperCase(), null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))                       // normalised
                .andExpect(jsonPath("$.username").value(email.substring(0, email.indexOf('@'))))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse();

        Cookie session = response.getCookie("SESSION");
        assertThat(session).as("auto-login after sign-up").isNotNull();
        me(session).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void signupKeepsProvidedUsernameAndBioTrimmed() throws Exception {
        signup(uniqueEmail(), "  Dang  ", "  hello  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Dang"))
                .andExpect(jsonPath("$.bio").value("hello"));
    }

    @Test
    void signupRejectsEmailAlreadyUsedInAnyCase() throws Exception {
        String email = uniqueEmail();
        signup(email, null, null).andExpect(status().isCreated());

        signup(email.toUpperCase(), null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").value("Email is already registered"));
    }

    @Test
    void signupValidatesFormatPolicyAndConfirmation() throws Exception {
        postJson("/api/auth/signup", new SignupRequest("not-an-email", "short", "short", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        postJson("/api/auth/signup", new SignupRequest("user@nodot", PASSWORD, PASSWORD, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        postJson("/api/auth/signup", new SignupRequest(uniqueEmail(), PASSWORD, "Different1", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword").value("Passwords do not match"));
    }

    @Test
    void signupRejectsPasswordOver72BytesAs400Not500() throws Exception {
        String multibyte = "a1" + "ậ".repeat(30); // 32 chars, 92 bytes
        postJson("/api/auth/signup", new SignupRequest(uniqueEmail(), multibyte, multibyte, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    // ---------- login

    @Test
    void loginReturnsProfileAndAWorkingSession() throws Exception {
        String email = uniqueEmail();
        signup(email, "Neo", null);

        MockHttpServletResponse response = login("  " + email.toUpperCase() + " ", PASSWORD, false);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json.readTree(response.getContentAsString()).get("username").asText()).isEqualTo("Neo");
        me(response.getCookie("SESSION")).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameGeneric401() throws Exception {
        String email = uniqueEmail();
        signup(email, null, null);

        for (var attempt : new String[][] {{email, "WrongPass1"}, {uniqueEmail(), PASSWORD}}) {
            postJson("/api/auth/login", new LoginRequest(attempt[0], attempt[1], false))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Test
    void loginRotatesSessionIdAndCsrfToken() throws Exception {
        String email = uniqueEmail();
        Cookie first = signup(email, null, null).andReturn().getResponse().getCookie("SESSION");

        // Log in again while carrying the existing session: a fixation attacker's planted cookie.
        MockHttpServletResponse response = login(email, PASSWORD, false, first);
        Cookie second = response.getCookie("SESSION");

        assertThat(second).isNotNull();
        assertThat(second.getValue()).as("session id rotated").isNotEqualTo(first.getValue());
        me(first).andExpect(status().isUnauthorized());   // old id is dead
        me(second).andExpect(status().isOk());
        assertThat(response.getCookie("XSRF-TOKEN")).as("CSRF token rotated on login").isNotNull();
    }

    /**
     * Spring Session makes a remembered cookie persistent (Max-Age = Integer.MAX_VALUE) and lets the
     * server decide the lifetime: the session's max inactive interval. The server is the authority.
     */
    @Test
    void rememberMeKeepsTheSessionFor30DaysIdleInsteadOf30Minutes() throws Exception {
        String email = uniqueEmail();
        signup(email, null, null);

        Cookie remembered = login(email, PASSWORD, true).getCookie("SESSION");
        Cookie browserSession = login(email, PASSWORD, false).getCookie("SESSION");

        assertThat(remembered.getMaxAge()).as("survives browser restarts").isPositive();
        assertThat(browserSession.getMaxAge()).as("ends with the browser").isEqualTo(-1);
        assertThat(serverSession(remembered).getMaxInactiveInterval()).isEqualTo(Duration.ofDays(30));
        assertThat(serverSession(browserSession).getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void reLoginWithoutRememberMeDropsAnEarlierRememberedLifetime() throws Exception {
        String email = uniqueEmail();
        signup(email, null, null);
        Cookie remembered = login(email, PASSWORD, true).getCookie("SESSION");

        Cookie relogged = login(email, PASSWORD, false, remembered).getCookie("SESSION");

        assertThat(serverSession(relogged).getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
        assertThat(relogged.getMaxAge()).isEqualTo(-1);
    }

    @Test
    void sessionsStoredInTheDatabaseNeverContainThePasswordHash() throws Exception {
        String email = uniqueEmail();
        Cookie afterSignup = signup(email, null, null).andReturn().getResponse().getCookie("SESSION");
        Cookie afterLogin = login(email, PASSWORD, false).getCookie("SESSION");

        for (Cookie cookie : new Cookie[] {afterSignup, afterLogin}) {
            SecurityContext context = serverSession(cookie)
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            var principal = (UserDetails) context.getAuthentication().getPrincipal();
            assertThat(principal.getUsername()).isEqualTo(email);
            assertThat(principal.getPassword()).as("hash erased before the session is persisted").isNull();
        }
    }

    private Session serverSession(Cookie cookie) {
        String id = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        return Objects.requireNonNull(sessions.findById(id), "session " + id);
    }

    // ---------- logout and /api/me

    @Test
    void logoutKillsTheSession() throws Exception {
        String email = uniqueEmail();
        Cookie session = signup(email, null, null).andReturn().getResponse().getCookie("SESSION");

        mvc.perform(post("/api/auth/logout").cookie(session).with(SpaCsrf.token(mvc)))
                .andExpect(status().isNoContent());

        me(session).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsOnlyTheCallersOwnAccount() throws Exception {
        String email = uniqueEmail();
        Cookie session = signup(email, "Me", "bio").andReturn().getResponse().getCookie("SESSION");

        me(session).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.username").value("Me"))
                .andExpect(jsonPath("$.bio").value("bio"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
