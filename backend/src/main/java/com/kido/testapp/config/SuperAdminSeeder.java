package com.kido.testapp.config;

import com.kido.testapp.common.PasswordPolicy;
import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Emails;
import com.kido.testapp.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps the single SUPER_ADMIN (design.md §5): creates it from config only if none exists, and
 * never overwrites an existing one (so a password changed later in the app survives restarts).
 * Fails startup on a weak password or an email already used by a regular account.
 */
@Component
@EnableConfigurationProperties(SuperAdminProperties.class)
public class SuperAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminProperties properties;

    public SuperAdminSeeder(AppUserRepository repository, PasswordEncoder passwordEncoder,
                            SuperAdminProperties properties) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsByRole(Role.SUPER_ADMIN)) {
            log.info("SUPER_ADMIN already exists; seeding skipped");
            return;
        }

        String email = Emails.normalise(properties.email());
        if (repository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "APP_SUPER_ADMIN_EMAIL is already used by a regular account; refusing to promote it implicitly");
        }
        String violation = PasswordPolicy.violation(properties.password());
        if (violation != null) {
            throw new IllegalStateException("APP_SUPER_ADMIN_PASSWORD " + violation);
        }

        repository.save(new AppUser(email, passwordEncoder.encode(properties.password()),
                Emails.defaultUsername(email), null, Role.SUPER_ADMIN));
        log.info("Seeded SUPER_ADMIN {}", email);
    }
}
