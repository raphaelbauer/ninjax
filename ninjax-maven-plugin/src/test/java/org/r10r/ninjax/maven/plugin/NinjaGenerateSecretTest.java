package org.r10r.ninjax.maven.plugin;

import static com.google.common.truth.Truth.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;

/**
 * Tests for NinjaGenerateSecret mojo.
 * Given a mojo, when executed, then it should log a generated secret key line.
 */
class NinjaGenerateSecretTest {

    @Test
    void givenSecretGeneration_whenExecute_thenLogsTwoLines() {
        // given
        List<String> captured = new ArrayList<>();
        Log logProxy = (Log) Proxy.newProxyInstance(
            Log.class.getClassLoader(),
            new Class<?>[]{Log.class},
            new InvocationHandler() {
                @Override public Object invoke(Object proxy, Method method, Object[] args) {
                    if (method.getName().equals("info") && args.length >= 1) {
                        captured.add(args[0] != null ? args[0].toString() : "null");
                    }
                    return null;
                }
            }
        );
        NinjaGenerateSecret mojo = new TestNinjaGenerateSecret(logProxy);

        // when
        mojo.execute();

        // then
        assertThat(captured).hasSize(2);
        assertThat(captured.get(0)).startsWith("Generated secret that is useful as '");
        assertThat(captured.get(1)).isNotEmpty();
    }

    /** Test subclass to inject a custom log instance. */
    private static class TestNinjaGenerateSecret extends NinjaGenerateSecret {
        private final Log log;
        TestNinjaGenerateSecret(Log log) { this.log = log; }
        @Override
        public Log getLog() { return log; }
    }
}
