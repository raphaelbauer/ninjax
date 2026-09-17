package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class NinjaCookieTest {

    @Test
    void builder_minimal_setsRequiredFields_andDefaults() {
        NinjaCookie cookie = NinjaCookie.builder("sid", "abc").build();

        assertThat(cookie.name()).isEqualTo("sid");
        assertThat(cookie.value()).isEqualTo("abc");

        // optionals default to empty
        assertThat(cookie.domain()).isEmpty();
        assertThat(cookie.path()).isEmpty();

        // primitive / enum defaults
        assertThat(cookie.maxAge()).isEqualTo(-1);
        assertThat(cookie.secure()).isEqualTo(Secure.No);
        assertThat(cookie.httpOnly()).isEqualTo(HttpOnly.No);
        assertThat(cookie.sameSite()).isEmpty();
    }

    @Test
    void builder_full_setsAllFields() {
        NinjaCookie cookie = NinjaCookie.builder("sid", "xyz")
                .domain("example.test")
                .maxAge(3600)
                .path("/app")
                .secure(true)
                .httpOnly(true)
                .sameSite(SameSite.Strict)
                .build();

        assertThat(cookie.name()).isEqualTo("sid");
        assertThat(cookie.value()).isEqualTo("xyz");
        assertThat(cookie.domain()).isEqualTo(Optional.of("example.test"));
        assertThat(cookie.maxAge()).isEqualTo(3600);
        assertThat(cookie.path()).isEqualTo(Optional.of("/app"));
        assertThat(cookie.secure()).isEqualTo(Secure.Yes);
        assertThat(cookie.httpOnly()).isEqualTo(HttpOnly.Yes);
        assertThat(cookie.sameSite()).isEqualTo(Optional.of(SameSite.Strict));
    }

    @Test
    void builder_overloadedSecure_andHttpOnly_useEnumDirectly() {
        NinjaCookie cookie = NinjaCookie.builder("sid", "xyz")
                .secure(Secure.Yes)
                .httpOnly(HttpOnly.No)
                .build();

        assertThat(cookie.secure()).isEqualTo(Secure.Yes);
        assertThat(cookie.httpOnly()).isEqualTo(HttpOnly.No);
    }

    @Test
    void secureEnum_ofBoolean_and_toBoolean_roundTrip() {
        assertThat(Secure.ofBoolean(true)).isEqualTo(Secure.Yes);
        assertThat(Secure.ofBoolean(false)).isEqualTo(Secure.No);

        assertThat(Secure.Yes.toBoolean()).isTrue();
        assertThat(Secure.No.toBoolean()).isFalse();
    }

    @Test
    void httpOnlyEnum_ofBoolean_and_toBoolean_roundTrip() {
        assertThat(HttpOnly.ofBoolean(true)).isEqualTo(HttpOnly.Yes);
        assertThat(HttpOnly.ofBoolean(false)).isEqualTo(HttpOnly.No);

        assertThat(HttpOnly.Yes.toBoolean()).isTrue();
        assertThat(HttpOnly.No.toBoolean()).isFalse();
    }

    @Test
    void sameSiteEnum_ofString_isCaseInsensitive_andEmptyForUnknownValues() {
        assertThat(SameSite.ofString("Strict")).isEqualTo(Optional.of(SameSite.Strict));
        assertThat(SameSite.ofString("lax")).isEqualTo(Optional.of(SameSite.Lax));
        assertThat(SameSite.ofString(" NONE ")).isEqualTo(Optional.of(SameSite.None));

        assertThat(SameSite.ofString("sometimes")).isEmpty();
        assertThat(SameSite.ofString("")).isEmpty();
    }

    @Test
    void builder_rejectsValueThatWouldInjectCookieAttributes() {
        // given
        String userInput = "x; Domain=evil.example";

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> NinjaCookie.builder("pref", userInput).build());

        // then
        assertThat(exception.getMessage()).contains("pref");
    }

    @Test
    void builder_rejectsControlCharactersAndLineBreaks() {
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "x\r\nSet-Cookie: b=c").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "x").path("/\r\nX: y").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "x").domain("a.test; Secure").build());
    }

    @Test
    void builder_rejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("", "x").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a=b", "x").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a b", "x").build());
    }

    @Test
    void builder_rejectsSpacesCommasQuotesAndBackslashesInValue() {
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "two words").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "1,2").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "say\"hi").build());
        assertThrows(IllegalArgumentException.class, () -> NinjaCookie.builder("a", "back\\slash").build());
    }

    @Test
    void builder_acceptsTypicalValues() {
        // given
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiYm9iIn0.abc-_DEF";

        // when / then
        assertThat(NinjaCookie.builder("NINJA_SESSION", jwt).path("/").domain("example.com").build().value())
                .isEqualTo(jwt);
        assertThat(NinjaCookie.builder("empty", "").build().value()).isEmpty();
        assertThat(NinjaCookie.builder("quoted", "\"abc\"").build().value()).isEqualTo("\"abc\"");
        assertThat(NinjaCookie.builder("encoded", "a%20b%3Bc").build().value()).isEqualTo("a%20b%3Bc");
    }

    @Test
    void canonicalConstructor_acceptsAnyIncomingValue_butResponseValidationRejectsIt() {
        // given: incoming cookies from browsers are not validated
        NinjaCookie incoming = new NinjaCookie("a", "x; Domain=evil.example", Optional.empty(), -1,
                Optional.empty(), Secure.No, HttpOnly.No, Optional.empty());

        // when / then
        assertThrows(IllegalArgumentException.class, incoming::requireValidForResponse);
    }
}
