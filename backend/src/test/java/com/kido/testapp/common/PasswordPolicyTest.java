package com.kido.testapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"abcdefg1", "Passw0rd!", "mật khẩu 2024", "Passw0rd\n", "line1\nline2"})
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

    @Test
    void enforcesTheBcrypt72ByteLimitNotACharLimit() {
        assertThat(PasswordPolicy.violation("a1" + "x".repeat(70))).isNull();       // 72 bytes
        assertThat(PasswordPolicy.violation("a1" + "x".repeat(71))).isNotNull();    // 73 bytes
        assertThat(PasswordPolicy.violation("a1" + "ậ".repeat(24))).isNotNull();    // 26 chars, 74 bytes
    }

    @Test
    void countsCharactersByCodePoint() {
        assertThat(PasswordPolicy.violation("a1😀😀😀")).isNotNull();             // 5 code points (8 UTF-16 units)
    }

    record Form(@ValidPassword String password) {
    }

    @Test
    void beanValidationUsesTheSameRule() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        String multibyte = "a1" + "ậ".repeat(30); // 32 chars — would pass @Size(max = 72) — but 92 bytes

        assertThat(validator.validate(new Form(multibyte))).singleElement()
                .satisfies(v -> assertThat(v.getMessage()).isEqualTo(PasswordPolicy.MESSAGE));
        assertThat(validator.validate(new Form("Passw0rd!"))).isEmpty();
    }
}
