package com.kido.testapp.user;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Email arguments must be normalised ({@link Emails#normalise}); the column stores them that way. */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    /** What a session must still match, checked on every request (auth/SessionValidityFilter). */
    @Query("select new com.kido.testapp.user.AuthState(u.role, u.passwordChangedAt) from AppUser u where u.email = :email")
    Optional<AuthState> findAuthStateByEmail(@Param("email") String email);

    /** FOR UPDATE: concurrent password changes of one account are serialised (the second re-checks the hash). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForUpdate(@Param("email") String email);

    /** FOR SHARE: the actor's role can't change until the role-change transaction commits. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForShare(@Param("email") String email);

    /** FOR UPDATE: concurrent role changes of the same target are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdForUpdate(@Param("id") UUID id);
}
