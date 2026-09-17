package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Range;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.jwt.Jwt;
import org.r10r.ninjax.core.jwt.JwtTest;
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

    @Test
    public void shouldCreateBrowserSessionCookieWhenNoExpiryIsConfigured() {
        // given
        NinjaSessionConverter converter = new NinjaSessionConverter(new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(32))));

        // when
        NinjaCookie cookie = converter.createCookieWithInformationOfNinjaSession(new NinjaSession(Map.of("user", "bob")));

        // then
        assertThat(cookie.maxAge()).isEqualTo(-1);
        assertThat(converter.extractSessionFromCookie(cookie).orElseThrow().get("user")).hasValue("bob");
    }

    @Test
    public void shouldSetMaxAgeFromConfiguredExpiry() {
        // given
        NinjaSessionConverter converter = new NinjaSessionConverter(new FixedNinjaProperties(Map.of(
                NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(32),
                "application.session.expire_time_in_seconds", "3600")));

        // when
        NinjaCookie cookie = converter.createCookieWithInformationOfNinjaSession(new NinjaSession(Map.of("user", "bob")));

        // then
        assertThat(cookie.maxAge()).isIn(Range.closed(3599, 3600));
    }

    @Test
    public void shouldDeleteCookieWhenSessionExpiryAlreadyPassed() {
        // given
        NinjaSessionConverter converter = new NinjaSessionConverter(new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(32))));
        String expiredOneMinuteAgo = String.valueOf(Instant.now().minusSeconds(60).getEpochSecond());

        // when
        NinjaCookie cookie = converter.createCookieWithInformationOfNinjaSession(
                new NinjaSession(Map.of("user", "bob", "exp", expiredOneMinuteAgo)));

        // then
        assertThat(cookie.maxAge()).isEqualTo(0);
    }

    private static NinjaProperties propertiesWithValidSecretAnd(Map<String, String> additionalValues) {
        Map<String, String> values = new HashMap<>(additionalValues);
        values.put(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, base64SecretOfLength(32));
        return new FixedNinjaProperties(values);
    }

    @Test
    public void shouldDefaultSessionCookieToSameSiteLax() {
        // given
        NinjaSessionConverter converter = new NinjaSessionConverter(propertiesWithValidSecretAnd(Map.of()));

        // when
        NinjaCookie sessionCookie = converter.createCookieWithInformationOfNinjaSession(new NinjaSession(Map.of()));
        NinjaCookie removeSessionCookie = converter.createCookieToRemoveNinjaSession();

        // then
        assertThat(sessionCookie.sameSite()).hasValue(SameSite.Lax);
        assertThat(removeSessionCookie.sameSite()).hasValue(SameSite.Lax);
    }

    @Test
    public void shouldUseConfiguredSameSiteCaseInsensitively() {
        // given
        NinjaProperties properties = propertiesWithValidSecretAnd(
                Map.of("application.session.cookie.same_site", "strict"));
        NinjaSessionConverter converter = new NinjaSessionConverter(properties);

        // when
        NinjaCookie sessionCookie = converter.createCookieWithInformationOfNinjaSession(new NinjaSession(Map.of()));
        NinjaCookie removeSessionCookie = converter.createCookieToRemoveNinjaSession();

        // then
        assertThat(sessionCookie.sameSite()).hasValue(SameSite.Strict);
        assertThat(removeSessionCookie.sameSite()).hasValue(SameSite.Strict);
    }

    @Test
    public void shouldAllowSameSiteNoneWhenCookieIsSecure() {
        // given
        NinjaProperties properties = propertiesWithValidSecretAnd(Map.of(
                "application.session.cookie.same_site", "None",
                "application.session.cookie.secure", "true"));
        NinjaSessionConverter converter = new NinjaSessionConverter(properties);

        // when
        NinjaCookie sessionCookie = converter.createCookieWithInformationOfNinjaSession(new NinjaSession(Map.of()));

        // then
        assertThat(sessionCookie.sameSite()).hasValue(SameSite.None);
        assertThat(sessionCookie.secure()).isEqualTo(Secure.Yes);
    }

    @Test
    public void shouldRejectInvalidSameSiteValue() {
        // given
        NinjaProperties properties = propertiesWithValidSecretAnd(
                Map.of("application.session.cookie.same_site", "sometimes"));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new NinjaSessionConverter(properties));

        // then
        assertThat(exception.getMessage()).contains("Invalid value 'sometimes'");
        assertThat(exception.getMessage()).contains("application.session.cookie.same_site");
    }

    @Test
    public void shouldRejectSameSiteNoneWithoutSecureCookie() {
        // given
        NinjaProperties properties = propertiesWithValidSecretAnd(Map.of(
                "application.session.cookie.same_site", "none",
                "application.session.cookie.secure", "false"));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new NinjaSessionConverter(properties));

        // then
        assertThat(exception.getMessage()).contains("requires 'application.session.cookie.secure=true'");
    }

    private static NinjaSessionConverter converterWithOldImplementationSecret() {
        return new NinjaSessionConverter(new FixedNinjaProperties(
                Map.of(NinjaConstants.NINJA_APPLICATION_SECRET_KEY, JwtTest.OLD_IMPLEMENTATION_SECRET)));
    }

    private static SecretKey oldImplementationKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(JwtTest.OLD_IMPLEMENTATION_SECRET), "HmacSHA256");
    }

    private static NinjaCookie sessionCookie(String value) {
        return NinjaCookie.builder(NinjaSessionConverter.NINJA_SESSION_COOKIE_NAME, value).build();
    }

    @Test
    public void shouldRoundTripSession() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        NinjaSession session = new NinjaSession(Map.of("username", "alice"));

        // when
        NinjaCookie cookie = converter.createCookieWithInformationOfNinjaSession(session);
        Optional<NinjaSession> extracted = converter.extractSessionFromCookie(cookie);

        // then
        assertThat(extracted.isPresent()).isTrue();
        assertThat(extracted.get().get("username")).hasValue("alice");
        assertThat(extracted.get().get("nbf")).isPresent();
        assertThat(extracted.get().get("iat")).isPresent();
    }

    @Test
    public void shouldKeepExpiryOfExistingSessionWhenCreatingCookieAgain() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        NinjaSession session = new NinjaSession(Map.of("username", "alice", "exp", "4102444800"));

        // when
        NinjaCookie cookie = converter.createCookieWithInformationOfNinjaSession(session);

        // then
        Map<String, Object> claims = Jwt.verify(cookie.value(), oldImplementationKey());
        assertThat(claims).containsEntry("exp", 4102444800L);
    }

    @Test
    public void shouldAcceptSessionCookieCreatedByOldImplementation() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(
                sessionCookie(JwtTest.OLD_IMPLEMENTATION_TOKEN));

        // then
        assertThat(session.isPresent()).isTrue();
        assertThat(session.get().get("username")).hasValue("alice");
        assertThat(session.get().get("note")).hasValue("say \"hi\"\nüñ");
        assertThat(session.get().get("exp")).hasValue("4102444800");
    }

    @Test
    public void shouldIgnoreSessionWithInvalidSignature() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        SecretKey otherKey = new SecretKeySpec("another-secret-of-32-bytes-long!".getBytes(), "HmacSHA256");
        String token = Jwt.sign(Map.of("username", "mallory"), otherKey);

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreGarbageCookie() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie("not-a-jwt"));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreExpiredSession() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        long oneMinuteAgo = Instant.now().minusSeconds(60).getEpochSecond();
        String token = Jwt.sign(Map.of("username", "alice", "exp", oneMinuteAgo), oldImplementationKey());

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreSessionThatIsNotValidYet() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        long inOneMinute = Instant.now().plusSeconds(60).getEpochSecond();
        String token = Jwt.sign(Map.of("username", "alice", "nbf", inOneMinute), oldImplementationKey());

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreSessionWithNonNumericExpiry() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        String token = Jwt.sign(Map.of("username", "alice", "exp", "4102444800"), oldImplementationKey());

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreSessionWithNonIntegerExpiry() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        String token = Jwt.sign(Map.of("username", "alice", "exp", 4102444800.5), oldImplementationKey());

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }

    @Test
    public void shouldIgnoreSessionWithNonNumericNotBefore() {
        // given
        NinjaSessionConverter converter = converterWithOldImplementationSecret();
        String token = Jwt.sign(Map.of("username", "alice", "nbf", true), oldImplementationKey());

        // when
        Optional<NinjaSession> session = converter.extractSessionFromCookie(sessionCookie(token));

        // then
        assertThat(session.isPresent()).isFalse();
    }
}
