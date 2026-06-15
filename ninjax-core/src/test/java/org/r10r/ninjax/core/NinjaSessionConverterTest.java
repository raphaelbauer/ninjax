package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.properties.NinjaProperties;

public class NinjaSessionConverterTest {

    /**
     * Test double for NinjaProperties that serves a fixed map of properties instead of
     * loading from conf/application.conf.
     */
    private static class FixedNinjaProperties extends NinjaProperties {
        private final Map<String, String> values;

        FixedNinjaProperties(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public Optional<String> get(String propertyName) {
            return Optional.ofNullable(values.get(propertyName));
        }
    }

    private static String base64SecretOfLength(int numberOfBytes) {
        return Base64.getEncoder().encodeToString(new byte[numberOfBytes]);
    }

    @Test
    public void shouldRejectSecretShorterThan32Bytes() {
        // given
        NinjaProperties properties = new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(31)));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new NinjaSessionConverter(properties));

        // then
        assertThat(exception.getMessage()).contains("too weak");
    }

    @Test
    public void shouldRejectWeakChangemeDemoDefault() {
        // given
        NinjaProperties properties = new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, "changeme"));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new NinjaSessionConverter(properties));

        // then
        assertThat(exception.getMessage()).contains("too weak");
    }

    @Test
    public void shouldAcceptSecretOfAtLeast32Bytes() {
        // given
        NinjaProperties properties = new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(32)));

        // when
        NinjaSessionConverter converter = new NinjaSessionConverter(properties);

        // then
        assertThat(converter).isNotNull();
    }

    @Test
    public void shouldFailWhenSecretIsMissing() {
        // given
        NinjaProperties properties = new FixedNinjaProperties(Map.of());

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new NinjaSessionConverter(properties));

        // then
        assertThat(exception.getMessage()).contains("Missing key");
    }
}
