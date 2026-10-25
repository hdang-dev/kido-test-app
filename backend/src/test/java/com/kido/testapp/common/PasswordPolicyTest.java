package com.kido.testapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"abcdefg1", "Passw0rd!", "mật khẩu 2024"})
    void accepts(String password) {
        assertThat(PasswordPolicy.violation(password)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "abc1234",        // 7 chars
            "abcdefgh",       // no digit
            "12345678",       // no letter
    })
    void rejects(String password) {
        assertThat(PasswordPolicy.violation(password)).isEqualTo(PasswordPolicy.MESSAGE);
    }

    @org.junit.jupiter.api.Test
    void rejectsMoreThan72Bytes() {
        assertThat(PasswordPolicy.violation("a1" + "x".repeat(70))).isNull();          // exactly 72
        assertThat(PasswordPolicy.violation("a1" + "x".repeat(71))).isNotNull();       // 73 chars
        assertThat(PasswordPolicy.violation("a1" + "ậ".repeat(24))).isNotNull();       // 50 chars, 74 bytes
    }
}
