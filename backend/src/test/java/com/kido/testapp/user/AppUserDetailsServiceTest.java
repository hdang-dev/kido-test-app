package com.kido.testapp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kido.testapp.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@IntegrationTest
class AppUserDetailsServiceTest {

    @Autowired AppUserDetailsService service;

    @Test
    void loadsByEmailIgnoringCaseAndWhitespace() {
        var details = service.loadUserByUsername("  ROOT@Test.Local ");

        assertThat(details.getUsername()).isEqualTo("root@test.local"); // principal name = normalised email
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SUPER_ADMIN");
    }

    @Test
    void unknownEmailFailsWithoutEchoingIt() {
        assertThatThrownBy(() -> service.loadUserByUsername("nobody@test.local"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageNotContaining("nobody");
    }
}
