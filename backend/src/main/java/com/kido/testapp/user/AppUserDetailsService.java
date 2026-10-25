package com.kido.testapp.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Spring Security's "username" is our email (design.md D5). The principal name is therefore the email. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    public AppUserDetailsService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser user = repository.findByEmail(Emails.normalise(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found")); // generic: no email echo
        return toUserDetails(user);
    }

    /** The principal stored in the session. Callers that skip authentication must erase credentials. */
    public static User toUserDetails(AppUser user) {
        return (User) User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}
