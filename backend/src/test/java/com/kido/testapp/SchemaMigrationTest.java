package com.kido.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Role;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.annotation.Transactional;

/** The DB itself enforces design.md §4/§5 rules, independent of application code. */
@IntegrationTest
@Transactional // every insert is rolled back
class SchemaMigrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository repository;
    @Autowired JdbcIndexedSessionRepository sessions;

    private void insertUser(String email, String role) {
        jdbc.update("INSERT INTO app_user (email, password_hash, username, role) VALUES (?, 'x', 'u', ?)", email, role);
    }

    @Test
    void generatesUuidIdAndDefaults() {
        insertUser("new@test.local", "USER");
        var row = jdbc.queryForMap("SELECT id, role, created_at FROM app_user WHERE email = 'new@test.local'");
        assertThat(row.get("id")).isInstanceOf(java.util.UUID.class);
        assertThat(row.get("role")).isEqualTo("USER");
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void rejectsDuplicateEmail() {
        insertUser("dup@test.local", "USER");
        assertThatThrownBy(() -> insertUser("dup@test.local", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // One failing statement per test: Postgres aborts the transaction after the first error.
    @Test
    void rejectsDuplicateEmailThatDiffersOnlyInCase() {
        insertUser("case@test.local", "USER");
        assertThatThrownBy(() -> insertUser("Case@Test.LOCAL", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** UserService.register relies on this name to tell "email taken" apart from other integrity errors. */
    @Test
    void duplicateEmailViolationCarriesTheIndexName() {
        repository.saveAndFlush(new AppUser("named@test.local", "x", "u", null, Role.USER));

        assertThatThrownBy(() -> repository.saveAndFlush(new AppUser("NAMED@test.local", "x", "u", null, Role.USER)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(ConstraintViolationException.class,
                        cve -> assertThat(cve.getConstraintName()).isEqualTo("uq_app_user_email_ci"));
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() -> insertUser("role@test.local", "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsAtMostOneSuperAdmin() {
        jdbc.update("DELETE FROM app_user WHERE role = 'SUPER_ADMIN'"); // independent of seeding; rolled back
        insertUser("root1@test.local", "SUPER_ADMIN");
        assertThatThrownBy(() -> insertUser("root2@test.local", "SUPER_ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sessionTablesStoreLongEmailPrincipals() {
        String longEmail = "a".repeat(240) + "@test.local"; // > 100, the upstream column size
        assertRoundTrip(sessions, longEmail);
    }

    private <S extends Session> void assertRoundTrip(FindByIndexNameSessionRepository<S> repo, String principal) {
        S session = repo.createSession();
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, principal);
        session.setAttribute("probe", "value");
        repo.save(session);

        assertThat(repo.findByPrincipalName(principal)).containsKey(session.getId());
        repo.deleteById(session.getId());
    }
}
