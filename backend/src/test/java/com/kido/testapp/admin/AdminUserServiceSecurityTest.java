package com.kido.testapp.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kido.testapp.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

/** The second layer: @PreAuthorize on the service holds even if a URL rule were ever wrong. */
@IntegrationTest
class AdminUserServiceSecurityTest {

    @Autowired AdminUserService service;

    @Test
    @WithMockUser(roles = "USER")
    void usersAreDeniedAtTheServiceLayer() {
        assertThatThrownBy(() -> service.list(0, 10)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void theRoleHierarchyAppliesToMethodSecurity() {
        assertThatCode(() -> service.list(0, 10)).doesNotThrowAnyException();
    }
}
