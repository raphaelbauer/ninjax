package org.r10r.ninjax.core.conf;


import org.r10r.ninjax.core.properties.NinjaProperties;
import static com.google.common.truth.Truth.*;
import java.util.Map;
import java.util.Optional;

public class NinjaPropertiesTest {

    public NinjaPropertiesTest() {

    }


    @org.junit.jupiter.api.Test
    public void testGet() {

        NinjaProperties ninjaProperties = new NinjaProperties();
        assertThat(ninjaProperties.get("one")).isEqualTo(Optional.of("test"));
    }

    @org.junit.jupiter.api.Test
    public void missingApplicationConf_doesNotThrow_andStillReadsSystemProperties() {
        // given a context class loader that cannot find conf/application.conf
        Thread thread = Thread.currentThread();
        ClassLoader originalClassLoader = thread.getContextClassLoader();
        ClassLoader emptyClassLoader = new ClassLoader(null) {};
        System.setProperty("ninjax.test.missingConf", "fromSystemProperty");

        try {
            thread.setContextClassLoader(emptyClassLoader);

            // when
            NinjaProperties ninjaProperties = new NinjaProperties();

            // then properties from application.conf are absent, but -D properties still work
            assertThat(ninjaProperties.get("one")).isEqualTo(Optional.empty());
            assertThat(ninjaProperties.get("ninjax.test.missingConf")).isEqualTo(Optional.of("fromSystemProperty"));
        } finally {
            thread.setContextClassLoader(originalClassLoader);
            System.clearProperty("ninjax.test.missingConf");
        }
    }

    @org.junit.jupiter.api.Test
    public void environmentVariable_overridesApplicationConf() {
        // given application.conf contains one=test
        Map<String, String> environment = Map.of("ONE", "fromEnvironment");

        // when
        NinjaProperties ninjaProperties = new NinjaProperties(environment);

        // then
        assertThat(ninjaProperties.get("one")).isEqualTo(Optional.of("fromEnvironment"));
        assertThat(ninjaProperties.getAllProperties()).containsEntry("one", "fromEnvironment");
    }

    @org.junit.jupiter.api.Test
    public void environmentVariable_isFound_evenIfKeyIsNotInApplicationConf() {
        // given
        Map<String, String> environment = Map.of("APPLICATION_SESSION_EXPIRE_TIME_IN_SECONDS", "60");

        // when
        NinjaProperties ninjaProperties = new NinjaProperties(environment);

        // then
        assertThat(ninjaProperties.get("application.session.expire_time_in_seconds")).isEqualTo(Optional.of("60"));
    }

    @org.junit.jupiter.api.Test
    public void systemProperty_winsOverEnvironmentVariable() {
        // given
        Map<String, String> environment = Map.of("ONE", "fromEnvironment", "NINJAX_TEST_PRECEDENCE", "fromEnvironment");
        System.setProperty("one", "fromSystemProperty");
        System.setProperty("ninjax.test.precedence", "fromSystemProperty");

        try {
            // when
            NinjaProperties ninjaProperties = new NinjaProperties(environment);

            // then
            assertThat(ninjaProperties.get("one")).isEqualTo(Optional.of("fromSystemProperty"));
            assertThat(ninjaProperties.get("ninjax.test.precedence")).isEqualTo(Optional.of("fromSystemProperty"));
        } finally {
            System.clearProperty("one");
            System.clearProperty("ninjax.test.precedence");
        }
    }

    @org.junit.jupiter.api.Test
    public void toEnvironmentVariableName_usesUpperCaseAndUnderscores() {
        // when / then
        assertThat(NinjaProperties.toEnvironmentVariableName("application.datasource.default.url"))
                .isEqualTo("APPLICATION_DATASOURCE_DEFAULT_URL");
        assertThat(NinjaProperties.toEnvironmentVariableName("application.session.cookie.same-site"))
                .isEqualTo("APPLICATION_SESSION_COOKIE_SAME_SITE");
    }

}
