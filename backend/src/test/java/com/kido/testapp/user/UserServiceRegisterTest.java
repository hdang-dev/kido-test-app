package com.kido.testapp.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import com.kido.testapp.common.ApiException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/** Only the email unique index means "email taken"; other DB errors must not be disguised as a 409. */
class UserServiceRegisterTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final UserService service =
            new UserService(repository, NoOpPasswordEncoder.getInstance(), mock(UserSessions.class));

    private void saveFailsOn(String constraint) {
        when(repository.existsByEmail(any())).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("insert failed",
                new ConstraintViolationException("violation", new SQLException(), constraint)));
    }

    @Test
    void aConcurrentDuplicateEmailIsA409() {
        saveFailsOn("uq_app_user_email_ci");

        assertThatThrownBy(() -> service.register("a@test.local", "Passw0rd!", "Passw0rd!", null, null))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getFieldErrors()).containsKey("email"));
    }

    @Test
    void anyOtherIntegrityErrorIsNotReportedAsEmailTaken() {
        saveFailsOn("ck_app_user_role");

        assertThatThrownBy(() -> service.register("a@test.local", "Passw0rd!", "Passw0rd!", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
