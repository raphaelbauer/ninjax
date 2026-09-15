package org.r10r.ninjax.core.conf;


import org.r10r.ninjax.core.properties.NinjaProperties;
import static com.google.common.truth.Truth.*;
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

}
