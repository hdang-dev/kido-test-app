package com.kido.testapp.auth;

import static com.kido.testapp.AuthTestClient.PASSWORD;
import static com.kido.testapp.AuthTestClient.uniqueEmail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kido.testapp.AuthTestClient;
import com.kido.testapp.IntegrationTest;
import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Changes that bypass every session sweep (written straight to the DB, like a race where a session was
 * created with the old role or the old password) must still take effect on the very next request.
 */
@IntegrationTest
class SessionValidityFilterTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository repository;
    AuthTestClient client;

    @BeforeEach
    void setUp() {
        client = new AuthTestClient(mvc, json);
    }

    private void setRoleDirectly(String email, Role role) {
        var user = repository.findByEmail(email).orElseThrow();
        user.setRole(role);
        repository.saveAndFlush(user);
    }

    @Test
    void aSessionWithAStaleRoleIsSignedOutOnTheNextRequest() throws Exception {
        String email = uniqueEmail();
        client.signup(email);
        setRoleDirectly(email, Role.ADMIN);
        Cookie adminSession = client.login(email, PASSWORD);
        client.getAs(adminSession, "/api/admin/users").andExpect(status().isOk());

        setRoleDirectly(email, Role.USER); // no sweep ran

        client.getAs(adminSession, "/api/admin/users").andExpect(status().isUnauthorized());
        client.getAs(adminSession, "/api/me").andExpect(status().isUnauthorized()); // session is gone for good
    }

    @Test
    void aSessionOlderThanTheLastPasswordChangeIsSignedOut() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);
        client.getAs(session, "/api/me").andExpect(status().isOk());

        // e.g. a login that verified the OLD password just before the change committed: no sweep removes it
        var user = repository.findByEmail(email).orElseThrow();
        user.changePasswordHash(user.getPasswordHash());
        repository.saveAndFlush(user);

        client.getAs(session, "/api/me").andExpect(status().isUnauthorized());
    }

    @Test
    void aSessionOfADeletedAccountIsSignedOut() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);

        repository.delete(repository.findByEmail(email).orElseThrow());

        client.getAs(session, "/api/me").andExpect(status().isUnauthorized());
    }

    @Test
    void anUnchangedRoleKeepsTheSession() throws Exception {
        Cookie session = client.signup(uniqueEmail());

        client.getAs(session, "/api/me").andExpect(status().isOk());
        client.getAs(session, "/api/me").andExpect(status().isOk());
    }
}
