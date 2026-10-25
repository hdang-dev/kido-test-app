package com.kido.testapp.admin;

import static com.kido.testapp.AuthTestClient.PASSWORD;
import static com.kido.testapp.AuthTestClient.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kido.testapp.AuthTestClient;
import com.kido.testapp.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** User management and the role rules of design.md §5, over real sessions. */
@IntegrationTest
class AdminControllerTest {

    // Seeded by SuperAdminSeeder from src/test/resources/application-test.yml
    private static final String ROOT_EMAIL = "root@test.local";
    private static final String ROOT_PASSWORD = "RootPass123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    AuthTestClient client;
    Cookie root;

    @BeforeEach
    void setUp() throws Exception {
        client = new AuthTestClient(mvc, json);
        root = client.login(ROOT_EMAIL, ROOT_PASSWORD);
    }

    // ---------- helpers

    private record Account(String email, String id, Cookie session) {
    }

    private Account newUser() throws Exception {
        String email = uniqueEmail();
        Cookie session = client.signup(email);
        String id = json.readTree(client.getAs(session, "/api/me").andReturn().getResponse().getContentAsString())
                .get("id").asText();
        return new Account(email, id, session);
    }

    /** A USER promoted by the super admin, logged in again so their session carries ROLE_ADMIN. */
    private Account newAdmin() throws Exception {
        Account user = newUser();
        setRole(root, user.id(), "ADMIN").andExpect(status().isOk());
        return new Account(user.email(), user.id(), client.login(user.email(), PASSWORD));
    }

    private ResultActions setRole(Cookie actor, String targetId, String role) throws Exception {
        return client.put("/api/admin/users/" + targetId + "/role", Map.of("role", role), actor);
    }

    // ---------- access to the API

    @Test
    void listNeedsAnAdmin() throws Exception {
        client.getAs(null, "/api/admin/users").andExpect(status().isUnauthorized());
        client.getAs(newUser().session(), "/api/admin/users").andExpect(status().isForbidden());
        client.getAs(newAdmin().session(), "/api/admin/users").andExpect(status().isOk());
        client.getAs(root, "/api/admin/users").andExpect(status().isOk());
    }

    @Test
    void listIsPagedAndNeverExposesHashes() throws Exception {
        newUser();
        newUser();

        client.getAs(root, "/api/admin/users?page=0&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content[0].email").exists())
                .andExpect(jsonPath("$.content[0].role").exists())
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void rejectsOutOfRangePaging() throws Exception {
        client.getAs(root, "/api/admin/users?size=101").andExpect(status().isBadRequest());
        client.getAs(root, "/api/admin/users?page=-1").andExpect(status().isBadRequest());
        client.getAs(root, "/api/admin/users?page=abc").andExpect(status().isBadRequest());
    }

    @Test
    void roleChangeNeedsAnAdminAndCsrf() throws Exception {
        Account user = newUser();
        Account other = newUser();

        setRole(user.session(), other.id(), "ADMIN").andExpect(status().isForbidden());
        client.putWithoutCsrf("/api/admin/users/" + other.id() + "/role", Map.of("role", "ADMIN"), root)
                .andExpect(status().isForbidden());
        setRole(null, other.id(), "ADMIN").andExpect(status().isUnauthorized());
    }

    // ---------- grant

    @Test
    void anAdminCanGrantAdminAndTheTargetIsForcedToLogInAgain() throws Exception {
        Account admin = newAdmin();
        Account user = newUser();

        setRole(admin.session(), user.id(), "ADMIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        client.getAs(user.session(), "/api/me").andExpect(status().isUnauthorized()); // old session gone
        Cookie fresh = client.login(user.email(), PASSWORD);
        client.getAs(fresh, "/api/admin/users").andExpect(status().isOk());         // new authorities
    }

    // ---------- revoke

    @Test
    void aRegularAdminCannotRevokeAnotherAdmin() throws Exception {
        Account admin = newAdmin();
        Account otherAdmin = newAdmin();

        setRole(admin.session(), otherAdmin.id(), "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the super admin can revoke admin rights"));
        client.getAs(otherAdmin.session(), "/api/admin/users").andExpect(status().isOk()); // untouched
    }

    @Test
    void theSuperAdminCanRevokeAndTheDemotedAdminLosesAccessImmediately() throws Exception {
        Account admin = newAdmin();

        setRole(root, admin.id(), "USER").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));

        client.getAs(admin.session(), "/api/admin/users").andExpect(status().isUnauthorized()); // session killed
        Cookie fresh = client.login(admin.email(), PASSWORD);
        client.getAs(fresh, "/api/admin/users").andExpect(status().isForbidden());              // now a USER
    }

    // ---------- guard rails

    @Test
    void nobodyCanChangeTheirOwnRole() throws Exception {
        Account admin = newAdmin();

        setRole(admin.session(), admin.id(), "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot change your own role"));
    }

    @Test
    void theSuperAdminIsNeverATarget() throws Exception {
        String rootId = json.readTree(client.getAs(root, "/api/me").andReturn().getResponse().getContentAsString())
                .get("id").asText();
        Account admin = newAdmin();

        setRole(admin.session(), rootId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("The super admin's role cannot be changed"));
    }

    @Test
    void superAdminCanNeverBeGranted() throws Exception {
        Account user = newUser();

        setRole(root, user.id(), "SUPER_ADMIN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void rejectsUnknownRolesAndIds() throws Exception {
        Account user = newUser();

        setRole(root, user.id(), "OWNER").andExpect(status().isBadRequest());
        setRole(root, UUID.randomUUID().toString(), "ADMIN").andExpect(status().isNotFound());
        setRole(root, "not-a-uuid", "ADMIN").andExpect(status().isBadRequest());
    }

    @Test
    void settingTheCurrentRoleIsANoOpThatKeepsSessions() throws Exception {
        Account user = newUser();

        setRole(root, user.id(), "USER").andExpect(status().isOk());

        client.getAs(user.session(), "/api/me").andExpect(status().isOk());
        assertThat(user.session()).isNotNull();
    }
}
