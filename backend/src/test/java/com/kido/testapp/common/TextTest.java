package com.kido.testapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void treatsUnicodeSpacesAsBlank() {
        assertThat(Text.isBlank("  ")).isTrue();       // no-break spaces: String.strip() keeps them
        assertThat(Text.isBlank("  \t")).isTrue();
        assertThat(Text.trim("  Neo  ")).isEqualTo("Neo");
        assertThat(Text.trim(null)).isNull();
    }

    @Test
    void rejectsControlCharactersButAllowsLineBreaksWhenMultiline() {
        assertThat(Text.hasControlChars("a\u0000b", true)).isTrue();   // NUL: Postgres rejects it
        assertThat(Text.hasControlChars("a\u001Bb", true)).isTrue();   // ESC
        assertThat(Text.hasControlChars("line1\nline2\tx", false)).isTrue();
        assertThat(Text.hasControlChars("line1\nline2\tx", true)).isFalse();
        assertThat(Text.hasControlChars("Mật khẩu 😀", false)).isFalse();
    }
}
