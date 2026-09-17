package org.r10r.ninjax.core.server;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.core.Router;
import org.r10r.ninjax.core.properties.NinjaProperties;

/**
 * Runs a real NinjaHttpServer, because the JDK HttpServer decides whether the path is decoded.
 */
class NinjaHttpServerPathDecodingTest {

    private static int port;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Test double for NinjaProperties that serves a fixed map instead of conf/application.conf.
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

    @BeforeAll
    static void startServer() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        var properties = new FixedNinjaProperties(Map.of(
                "ninja.port", String.valueOf(port),
                "application.secret", "RHwx0fWz73AlJx1fulfkrYKL5Yo7t8F1H8xUajByE28="));

        var router = new Router();
        router.GET("/p/{v}").with(request -> Result.ok(
                request.getPathParameter("v").orElse("<invalid>") + "|" + request.getRequestPath()));

        // NinjaHttpServer blocks in its constructor until it is stopped.
        Thread serverThread = new Thread(() -> new NinjaHttpServer(router, properties));
        serverThread.setDaemon(true);
        serverThread.start();

        for (int i = 0; i < 100; i++) {
            try (Socket ignored = new Socket("localhost", port)) {
                return;
            } catch (IOException notYetStarted) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("NinjaHttpServer did not start on port " + port);
    }

    @Test
    void pathParameter_isDecodedExactlyOnce() throws Exception {
        // when
        HttpResponse<String> response = get("/p/100%2525");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("100%25|/p/100%2525");
    }

    @Test
    void plusInPath_staysPlus() throws Exception {
        // when
        HttpResponse<String> response = get("/p/a+b");

        // then
        assertThat(response.body()).isEqualTo("a+b|/p/a+b");
    }

    @Test
    void encodedSlash_doesNotSplitTheSegment() throws Exception {
        // when
        HttpResponse<String> response = get("/p/a%2Fb");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("a/b|/p/a%2Fb");
    }

    @Test
    void encodedPercent_isNotAServerError() throws Exception {
        // when
        HttpResponse<String> response = get("/p/100%25");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("100%|/p/100%25");
    }

    private HttpResponse<String> get(String rawPath) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPath)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
