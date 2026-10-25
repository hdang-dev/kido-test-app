package com.kido.testapp.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Server-side revocation of a user's sessions (design.md D2): the reason sessions live in Postgres.
 * Sessions are indexed by principal name, which is the normalised email.
 */
@Component
public class UserSessions {

    private static final Logger log = LoggerFactory.getLogger(UserSessions.class);

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** E.g. after a role change: every device must log in again to pick up the new authorities. */
    public int expireAll(String email) {
        return expireAllExcept(email, null);
    }

    /** E.g. after a password change: other devices are logged out, the current one stays. */
    public int expireAllExcept(String email, String keepSessionId) {
        var ids = sessions.findByPrincipalName(email).keySet().stream()
                .filter(id -> !id.equals(keepSessionId))
                .toList();
        ids.forEach(sessions::deleteById);
        if (!ids.isEmpty()) {
            log.info("Expired {} session(s) of {}", ids.size(), email);
        }
        return ids.size();
    }
}
