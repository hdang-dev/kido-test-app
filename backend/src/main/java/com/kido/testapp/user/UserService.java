package com.kido.testapp.user;

import com.kido.testapp.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    static final String EMAIL_TAKEN = "Email is already registered";

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Sign-up (design.md §6). Inputs are already Bean-Validated; this enforces the cross-field and
     * uniqueness rules. "Email already used" is an accepted enumeration trade-off (design.md §10).
     */
    @Transactional
    public AppUser register(String email, String password, String confirmPassword, String username, String bio) {
        if (!password.equals(confirmPassword)) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "confirmPassword", "Passwords do not match");
        }
        String normalisedEmail = Emails.normalise(email);
        if (repository.existsByEmail(normalisedEmail)) {
            throw ApiException.field(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
        }
        String displayName = isBlank(username) ? Emails.defaultUsername(normalisedEmail) : username.strip();
        var user = new AppUser(normalisedEmail, passwordEncoder.encode(password), displayName,
                isBlank(bio) ? null : bio.strip(), Role.USER);
        try {
            return repository.saveAndFlush(user); // flush now so a concurrent duplicate surfaces here, not at commit
        } catch (DataIntegrityViolationException race) {
            throw ApiException.field(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
        }
    }

    /** For the signed-in principal. A missing row means the account vanished mid-session → treat as logged out. */
    @Transactional(readOnly = true)
    public AppUser getByEmail(String email) {
        return repository.findByEmail(Emails.normalise(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
