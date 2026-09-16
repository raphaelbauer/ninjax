package org.r10r.ninjax.jetty;

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
 * Starts a real NinjaJetty, because only the server decides between Content-Length and chunked encoding.
 */
class NinjaJettyContentLengthTest {

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
        router.GET("/html").with(request -> Result.builder().html("<p>ä</p>").build());
        router.GET("/empty").with(request -> Result.builder().text("").build());
        router.GET("/stream").with(request -> Result.builder()
                .stream(outputStream -> {
                    try {
                        outputStream.write("streamed".getBytes());
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                })
                .build());

        // The server blocks in its constructor until it is stopped.
        Thread serverThread = new Thread(() -> new NinjaJetty(router, properties));
        serverThread.setDaemon(true);
        serverThread.start();

        for (int i = 0; i < 100; i++) {
            try (Socket ignored = new Socket("localhost", port)) {
                return;
            } catch (IOException notYetStarted) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Server did not start on port " + port);
    }

    @Test
    void htmlBody_isSentWithContentLength() throws Exception {
        // when
        HttpResponse<String> response = get("/html");

        // then
        assertThat(response.body()).isEqualTo("<p>ä</p>");
        assertThat(response.headers().firstValue("Content-Length")).hasValue("9");
        assertThat(response.headers().firstValue("Transfer-Encoding")).isEmpty();
    }

    @Test
    void emptyTextBody_isSentWithoutChunking() throws Exception {
        // when
        HttpResponse<String> response = get("/empty");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        assertThat(response.headers().firstValue("Transfer-Encoding")).isEmpty();
    }

    @Test
    void streamedBody_stillWorks() throws Exception {
        // when
        HttpResponse<String> response = get("/stream");

        // then
        assertThat(response.body()).isEqualTo("streamed");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}

