package com.kido.testapp.user;

import java.time.Instant;

import com.kido.testapp.common.ApiException;
import com.kido.testapp.common.Text;
import org.hibernate.exception.ConstraintViolationException;
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
        String displayName = Text.isBlank(username) ? Emails.defaultUsername(normalisedEmail) : Text.trim(username);
        var user = new AppUser(normalisedEmail, passwordEncoder.encode(password), displayName,
                Text.isBlank(bio) ? null : Text.trim(bio), Role.USER);
        try {
            return repository.saveAndFlush(user); // flush now so a concurrent duplicate surfaces here, not at commit
        } catch (DataIntegrityViolationException e) {
            if (violates(e, "uq_app_user_email_ci")) { // a concurrent sign-up with the same email won the race
                throw ApiException.field(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
            }
            throw e; // anything else is a real error, not "email taken"
        }
    }

    /** Profile edit (design.md §6): only username and bio; email and role are not editable here. */
    @Transactional
    public AppUser updateProfile(String email, String username, String bio) {
        AppUser user = getByEmail(email);
        user.setUsername(Text.isBlank(username) ? Emails.defaultUsername(user.getEmail()) : Text.trim(username));
        user.setBio(Text.isBlank(bio) ? null : Text.trim(bio));
        return user; // dirty checking writes it on commit
    }

    /**
     * Self-service password change (design.md §6). The row is locked FOR UPDATE, so two concurrent changes
     * are serialised and the second one re-checks the current password against the first one's result.
     * Writing the new hash also bumps the credential version: SessionValidityFilter then signs out every
     * session created before it, whatever the timing. The other sessions are also deleted right away.
     *
     * @return the new credential version, to stamp on the caller's (rotated) session
     */
    @Transactional
    public Instant changePassword(String email, String currentPassword, String newPassword, String confirmPassword,
                                  String keepSessionId) {
        AppUser user = repository.findByEmailForUpdate(Emails.normalise(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "currentPassword", "Current password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "confirmPassword", "Passwords do not match");
        }
        if (newPassword.equals(currentPassword)) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "newPassword", "New password must differ from the current one");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userSessions.expireAllExcept(user.getEmail(), keepSessionId);
        return user.getPasswordChangedAt();
    }

    /** For the signed-in principal. A missing row means the account vanished mid-session → treat as logged out. */
    @Transactional(readOnly = true)
    public AppUser getByEmail(String email) {
        return repository.findByEmail(Emails.normalise(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private static boolean violates(DataIntegrityViolationException e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve && constraint.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
