package com.kido.testapp.user;

import static com.kido.testapp.AuthTestClient.PASSWORD;
import static com.kido.testapp.AuthTestClient.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kido.testapp.AuthTestClient;
import com.kido.testapp.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Profile edit and self-service password change (design.md §6, §7). */
@IntegrationTest
class MeControllerTest {

    private static final String NEW_PASSWORD = "N3wPassword!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    AuthTestClient client;

    @BeforeEach
    void setUp() {
        client = new AuthTestClient(mvc, json);
    }

    private static Map<String, String> passwordChange(String current, String next, String confirm) {
        return Map.of("currentPassword", current, "newPassword", next, "confirmPassword", confirm);
    }

    // ---------- PUT /api/me

    @Test
    void updatesUsernameAndBioTrimmed() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);

        client.put("/api/me", Map.of("username", "  Morpheus ", "bio", "  Captain of the Nebuchadnezzar "), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Morpheus"))
                .andExpect(jsonPath("$.bio").value("Captain of the Nebuchadnezzar"))
                .andExpect(jsonPath("$.email").value(email));

        client.getAs(session, "/api/me").andExpect(jsonPath("$.username").value("Morpheus"));
    }

    @Test
    void blankUsernameResetsToTheDefaultAndBlankBioClearsIt() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);
        client.put("/api/me", Map.of("username", "Temp", "bio", "Temp"), session);

        client.put("/api/me", Map.of("username", " ", "bio", ""), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(email.substring(0, email.indexOf('@'))))
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void rejectsTooLongFields() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.put("/api/me", Map.of("username", "x".repeat(51), "bio", "y".repeat(501)), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.bio").exists());
    }

    @Test
    void requiresBothFieldsSoAPartialBodyCannotWipeTheOther() throws Exception {
        Cookie session = client.signup(uniqueEmail());
        client.put("/api/me", Map.of("username", "Keep", "bio", "Keep"), session);

        client.put("/api/me", Map.of("bio", "only bio"), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
        client.getAs(session, "/api/me").andExpect(jsonPath("$.username").value("Keep"));
    }

    @Test
    void countsLengthsLikeTheDatabaseDoes() throws Exception {
        Cookie session = client.signup(uniqueEmail());
        String fiftyEmoji = "😀".repeat(50);           // 100 UTF-16 units, 50 code points: fits varchar(50)

        client.put("/api/me", Map.of("username", "  " + fiftyEmoji + "  ", "bio", ""), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(fiftyEmoji));
    }

    @Test
    void ignoresAttemptsToChangeEmailOrRole() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);

        client.put("/api/me", Map.of("username", "x", "bio", "", "email", "evil@test.local", "role", "SUPER_ADMIN"), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void profileEndpointsNeedAuthenticationAndCsrf() throws Exception {
        client.put("/api/me", Map.of("username", "x", "bio", ""), null).andExpect(status().isUnauthorized());

        Cookie session = client.signup(uniqueEmail());
        client.putWithoutCsrf("/api/me", Map.of("username", "x", "bio", ""), session).andExpect(status().isForbidden());
        client.putWithoutCsrf("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, NEW_PASSWORD), session)
                .andExpect(status().isForbidden());
    }

    // ---------- PUT /api/me/password

    @Test
    void changesPasswordSoOnlyTheNewOneWorks() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);

        client.put("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, NEW_PASSWORD), session)
                .andExpect(status().isNoContent());

        assertThat(client.login(email, PASSWORD)).as("old password rejected").isNull();
        assertThat(client.login(email, NEW_PASSWORD)).as("new password accepted").isNotNull();
    }

    @Test
    void expiresOtherSessionsAndRotatesTheCurrentOne() throws Exception {
        String email = uniqueEmail();
        Cookie laptop = client.signup(email);
        Cookie phone = client.login(email, PASSWORD);

        Cookie rotated = client.put("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, NEW_PASSWORD), laptop)
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");

        client.getAs(phone, "/api/me").andExpect(status().isUnauthorized());   // other device logged out
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(laptop.getValue());
        client.getAs(laptop, "/api/me").andExpect(status().isUnauthorized());  // old id is dead
        client.getAs(rotated, "/api/me").andExpect(status().isOk());           // this device stays in
    }

    @Test
    void passwordChangeRotatesTheCsrfTokenAndKeepsRememberMe() throws Exception {
        String email = uniqueEmail();
        client.signup(email);
        Cookie remembered = client.post("/api/auth/login",
                        Map.of("email", email, "password", PASSWORD, "rememberMe", true), null)
                .andReturn().getResponse().getCookie("SESSION");

        var response = client.put("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, NEW_PASSWORD), remembered)
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getCookie("XSRF-TOKEN")).as("new CSRF token, like at login").isNotNull();
        assertThat(response.getCookie("SESSION").getMaxAge()).as("still a persistent cookie").isPositive();
    }

    @Test
    void rejectsWrongCurrentPassword() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.put("/api/me/password", passwordChange("WrongPass1", NEW_PASSWORD, NEW_PASSWORD), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").value("Current password is incorrect"));
    }

    @Test
    void rejectsMismatchedConfirmation() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.put("/api/me/password", passwordChange(PASSWORD, NEW_PASSWORD, "Other1234"), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.confirmPassword").value("Passwords do not match"));
    }

    @Test
    void rejectsReusingTheCurrentPassword() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.put("/api/me/password", passwordChange(PASSWORD, PASSWORD, PASSWORD), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void rejectsNewPasswordBreakingThePolicy() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.put("/api/me/password", passwordChange(PASSWORD, "short", "short"), session)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }
}
