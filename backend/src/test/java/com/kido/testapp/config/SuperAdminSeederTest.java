package com.kido.testapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kido.testapp.IntegrationTest;
import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
class SuperAdminSeederTest {

    // From src/test/resources/application-test.yml
    private static final String EMAIL = "root@test.local";
    private static final String PASSWORD = "RootPass123";

    @Autowired AppUserRepository repository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SuperAdminSeeder seeder;

    @Test
    void seedsSuperAdminOnStartup() {
        AppUser root = repository.findByEmail(EMAIL).orElseThrow();

        assertThat(root.getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(root.getUsername()).isEqualTo("root");
        assertThat(passwordEncoder.matches(PASSWORD, root.getPasswordHash())).isTrue();
        assertThat(root.getPasswordHash()).startsWith("{bcrypt}");
    }

    @Test
    void rerunNeverOverwritesExistingSuperAdmin() {
        String hashBefore = repository.findByEmail(EMAIL).orElseThrow().getPasswordHash();

        seeder.run(null);

        assertThat(repository.findAll()).filteredOn(u -> u.getRole() == Role.SUPER_ADMIN).hasSize(1);
        assertThat(repository.findByEmail(EMAIL).orElseThrow().getPasswordHash()).isEqualTo(hashBefore);
    }

    @Test
    @Transactional // rolled back: restores the seeded super admin
    void rejectsWeakPassword() {
        repository.deleteAll();
        var weak = new SuperAdminSeeder(repository, passwordEncoder, new SuperAdminProperties(EMAIL, "short"));

        assertThatThrownBy(() -> weak.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("APP_SUPER_ADMIN_PASSWORD")
                .hasMessageNotContaining("short");
    }

    @Test
    @Transactional
    void refusesToPromoteAnExistingRegularAccount() {
        repository.deleteAll();
        repository.save(new AppUser("taken@test.local", "x", "taken", null, Role.USER));
        var seederForTaken = new SuperAdminSeeder(repository, passwordEncoder,
                new SuperAdminProperties("Taken@Test.local", PASSWORD));

        assertThatThrownBy(() -> seederForTaken.run(null)).isInstanceOf(IllegalStateException.class);
        assertThat(repository.existsByRole(Role.SUPER_ADMIN)).isFalse();
    }
}
