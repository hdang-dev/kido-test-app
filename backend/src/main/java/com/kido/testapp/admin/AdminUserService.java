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
 * /api/admin/** URL rule. The actor's role is read from the database (locked), not from session authorities.
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
        // Matches index ix_app_user_created_at (V3)
        return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id"))));
    }

    /**
     * Grant (USER → ADMIN): any ADMIN+. Revoke (ADMIN → USER): SUPER_ADMIN only. Nobody changes their
     * own role, the SUPER_ADMIN is never a target, and SUPER_ADMIN can never be granted.
     *
     * <p>Concurrency: facts that can never change (who is who, who is the SUPER_ADMIN) are checked first
     * without locks. Then both rows are locked — actor FOR SHARE, target FOR UPDATE — always in id order,
     * so two admins acting on each other can't deadlock. Roles are re-checked on the locked rows: an admin
     * revoked meanwhile can't finish a grant, and changes to one user are serialised.
     */
    @Transactional
    public AppUser changeRole(String actorEmail, UUID targetId, Role newRole) {
        if (newRole == Role.SUPER_ADMIN) {
            throw ApiException.field(HttpStatus.BAD_REQUEST, "role", "Role must be ADMIN or USER");
        }
        UUID actorId = repository.findIdByEmail(Emails.normalise(actorEmail))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        Role targetRole = repository.findRoleById(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (targetId.equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot change your own role");
        }
        if (targetRole == Role.SUPER_ADMIN) { // nobody becomes or stops being SUPER_ADMIN, so this can't go stale
            throw new ApiException(HttpStatus.FORBIDDEN, "The super admin's role cannot be changed");
        }

        AppUser actor;
        AppUser target;
        if (actorId.compareTo(targetId) < 0) {
            actor = lockActor(actorId);
            target = lockTarget(targetId);
        } else {
            target = lockTarget(targetId);
            actor = lockActor(actorId);
        }

        if (actor.getRole() == Role.USER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied"); // demoted since the request started
        }
        if (target.getRole() == newRole) {
            return target; // idempotent: nothing changes, sessions untouched
        }
        if (target.getRole() == Role.ADMIN && actor.getRole() != Role.SUPER_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the super admin can revoke admin rights");
        }

        Role previous = target.getRole();
        target.setRole(newRole);
        signOutAfterCommit(target.getEmail());
        log.info("{} changed role of {} from {} to {}", actor.getEmail(), target.getEmail(), previous, newRole);
        return target;
    }

    private AppUser lockActor(UUID id) {
        return repository.findByIdForShare(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    private AppUser lockTarget(UUID id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /**
     * Courtesy sweep so the target is signed out everywhere right away. Correctness doesn't depend on it:
     * SessionValidityFilter drops any session whose role no longer matches the database. So a failure here is
     * logged, never turned into an error for a change that has already committed.
     */
    private void signOutAfterCommit(String email) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    userSessions.expireAll(email);
                } catch (RuntimeException e) {
                    log.warn("Could not expire sessions of {} after role change; SessionValidityFilter will", email, e);
                }
            }
        });
    }
}
