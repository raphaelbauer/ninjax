package org.r10r.ninjax.jetty;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.core.Router;
import org.r10r.ninjax.core.properties.NinjaProperties;

class NinjaJettyUploadLimitTest {

    private static final int MAX_UPLOAD_BYTES = 1024;
    private static final String BOUNDARY = "boundary123";

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
    static void startJetty() throws Exception {
        port = findFreePort();
        var properties = new FixedNinjaProperties(Map.of(
                "ninja.port", String.valueOf(port),
                "ninja.http.maxUploadBytes", String.valueOf(MAX_UPLOAD_BYTES),
                "application.secret", "RHwx0fWz73AlJx1fulfkrYKL5Yo7t8F1H8xUajByE28="));

        var router = new Router();
        router.POST("/upload").with(request -> Result.ok("files=" + request.getFiles("file").size()));
        router.POST("/form").with(request -> Result.ok("title=" + request.getParameters().get("title").orElse("")));

        // NinjaJetty blocks in its constructor until the server stops.
        Thread serverThread = new Thread(() -> new NinjaJetty(router, properties));
        serverThread.setDaemon(true);
        serverThread.start();
        waitForServer();
    }

    @Test
    void multipartUploadWithinLimit_isAccepted() throws Exception {
        // given
        byte[] body = multipartBody(100);

        // when
        HttpResponse<String> response = post("/upload", "multipart/form-data; boundary=" + BOUNDARY,
                HttpRequest.BodyPublishers.ofByteArray(body));

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body().trim()).isEqualTo("files=1");
    }

    @Test
    void multipartUploadWithContentLengthAboveLimit_isRejectedWith413() throws Exception {
        // given
        byte[] body = multipartBody(MAX_UPLOAD_BYTES * 4);

        // when
        HttpResponse<String> response = post("/upload", "multipart/form-data; boundary=" + BOUNDARY,
                HttpRequest.BodyPublishers.ofByteArray(body));

        // then
        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void chunkedMultipartUploadAboveLimit_isRejectedAsBadRequest() throws Exception {
        // given
        byte[] body = multipartBody(MAX_UPLOAD_BYTES * 4);
        // ofInputStream has an unknown length, so the client sends the body chunked without Content-Length
        var chunked = HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(body));

        // when
        HttpResponse<String> response = post("/upload", "multipart/form-data; boundary=" + BOUNDARY, chunked);

        // then
        // Without Content-Length the limit is only hit while Jetty parses the parts. Jetty reports that
        // as "400: bad multipart", which is passed on instead of turning it into a 500.
        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void urlEncodedFormAboveLimit_isRejectedWith413() throws Exception {
        // given
        String body = "title=" + "a".repeat(MAX_UPLOAD_BYTES * 4);

        // when
        HttpResponse<String> response = post("/form", "application/x-www-form-urlencoded",
                HttpRequest.BodyPublishers.ofString(body));

        // then
        assertThat(response.statusCode()).isEqualTo(413);
    }

    private HttpResponse<String> post(String path, String contentType, HttpRequest.BodyPublisher body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", contentType)
                .timeout(Duration.ofSeconds(10))
                .POST(body)
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static byte[] multipartBody(int fileSize) {
        String head = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"a.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String tail = "\r\n--" + BOUNDARY + "--\r\n";
        return (head + "x".repeat(fileSize) + tail).getBytes(StandardCharsets.ISO_8859_1);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForServer() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try (var socket = new java.net.Socket("localhost", port)) {
                return;
            } catch (IOException notYetStarted) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Jetty did not start on port " + port);
    }
}
