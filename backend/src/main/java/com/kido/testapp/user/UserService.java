package com.kido.testapp.user;

import com.kido.testapp.common.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    static final String EMAIL_TAKEN = "Email is already registered";

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessions userSessions;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder, UserSessions userSessions) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.userSessions = userSessions;
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
        String displayName = StringUtils.hasText(username) ? username.strip() : Emails.defaultUsername(normalisedEmail);
        var user = new AppUser(normalisedEmail, passwordEncoder.encode(password), displayName,
                StringUtils.hasText(bio) ? bio.strip() : null, Role.USER);
        try {
            return repository.saveAndFlush(user); // flush now so a concurrent duplicate surfaces here, not at commit
        } catch (DataIntegrityViolationException race) {
            throw ApiException.field(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
        }
    }

    /** Profile edit (design.md §6): only username and bio; email and role are not editable here. */
    @Transactional
    public AppUser updateProfile(String email, String username, String bio) {
        AppUser user = getByEmail(email);
        user.setUsername(StringUtils.hasText(username) ? username.strip() : Emails.defaultUsername(user.getEmail()));
        user.setBio(StringUtils.hasText(bio) ? bio.strip() : null);
        return user; // dirty checking writes it on commit
    }

    /**
     * Self-service password change (design.md §6). After the checks pass, every other session of the
     * user is revoked <em>before</em> the new hash is written. Spring Session commits deletions on its
     * own, so the failure modes are both safe: revocation fails → password unchanged; the hash write
     * fails → sessions already revoked. Never "password changed, but a stolen session still works".
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword, String confirmPassword,
                               String keepSessionId) {
        AppUser user = getByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "currentPassword", "Current password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "confirmPassword", "Passwords do not match");
        }
        if (newPassword.equals(currentPassword)) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "newPassword", "New password must differ from the current one");
        }
        userSessions.expireAllExcept(user.getEmail(), keepSessionId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    /** For the signed-in principal. A missing row means the account vanished mid-session → treat as logged out. */
    @Transactional(readOnly = true)
    public AppUser getByEmail(String email) {
        return repository.findByEmail(Emails.normalise(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
