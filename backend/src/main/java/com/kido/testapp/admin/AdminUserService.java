package com.kido.testapp.admin;

import java.util.UUID;

import com.kido.testapp.common.ApiException;
import com.kido.testapp.user.AppUser;
import com.kido.testapp.user.AppUserRepository;
import com.kido.testapp.user.Emails;
import com.kido.testapp.user.Role;
import com.kido.testapp.user.UserSessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * User management (design.md §5). Every method is ADMIN+ by @PreAuthorize — a second layer behind the
 * /api/admin/** URL rule. The actor's role is read from the database, not from session authorities.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final AppUserRepository repository;
    private final UserSessions userSessions;

    public AdminUserService(AppUserRepository repository, UserSessions userSessions) {
        this.repository = repository;
        this.userSessions = userSessions;
    }

    @Transactional(readOnly = true)
    public Page<AppUser> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))));
    }

    /**
     * Grant (USER → ADMIN): any ADMIN+. Revoke (ADMIN → USER): SUPER_ADMIN only. Nobody changes their
     * own role, the SUPER_ADMIN is never a target, and SUPER_ADMIN can never be granted. A real change
     * expires all of the target's sessions so stale authorities can't outlive it.
     */
    @Transactional
    public AppUser changeRole(String actorEmail, UUID targetId, Role newRole) {
        if (newRole == Role.SUPER_ADMIN) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "role", "Role must be ADMIN or USER");
        }
        AppUser actor = repository.findByEmail(Emails.normalise(actorEmail))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        AppUser target = repository.findById(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot change your own role");
        }
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "The super admin's role cannot be changed");
        }
        if (target.getRole() == newRole) {
            return target; // idempotent: nothing changes, sessions untouched
        }
        if (target.getRole() == Role.ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the super admin can revoke admin rights");
        }

        Role previous = target.getRole();
        target.setRole(newRole);
        expireSessionsNowAndAfterCommit(target.getEmail());
        log.info("{} changed role of {} from {} to {}", actor.getEmail(), target.getEmail(), previous, newRole);
        return target;
    }

    /**
     * Now: if this fails, the role change rolls back (never "demoted, but the admin session still works").
     * After commit: also kills a session the target created during this transaction with the old role.
     */
    private void expireSessionsNowAndAfterCommit(String email) {
        userSessions.expireAll(email);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userSessions.expireAll(email);
            }
        });
    }
}
