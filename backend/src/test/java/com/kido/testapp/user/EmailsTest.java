package com.kido.testapp.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailsTest {

    @Test
    void normalisesCaseAndWhitespace() {
        assertThat(Emails.normalise("  John.Doe@Example.COM ")).isEqualTo("john.doe@example.com");
        assertThat(Emails.normalise(null)).isNull();
    }

    @Test
    void defaultUsernameIsTheLocalPart() {
        assertThat(Emails.defaultUsername("john@a.com")).isEqualTo("john");
    }

    @Test
    void defaultUsernameSplitsAtTheLastAt() {
        assertThat(Emails.defaultUsername("\"a@b\"@example.com")).isEqualTo("\"a@b\"");
    }

    @Test
    void defaultUsernameIsCappedToColumnSize() {
        assertThat(Emails.defaultUsername("x".repeat(64) + "@a.com")).hasSize(50);
    }

    @Test
    void defaultUsernameNeverSplitsASurrogatePair() {
        String local = "x".repeat(49) + "😀" + "tail"; // emoji is code point #50
        String username = Emails.defaultUsername(local + "@a.com");

        assertThat(username).endsWith("😀");
        assertThat(username.codePointCount(0, username.length())).isEqualTo(50);
    }
}
