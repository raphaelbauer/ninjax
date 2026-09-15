package org.r10r.ninjax.core.server;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the concurrency cap and the JDK request timeouts against a real JDK HttpServer on an ephemeral port.
 * The timeout check runs in a child JVM (see SlowClientTimeoutCheck), so it does not depend on other tests.
 */
class NinjaHttpServerLimitsTest {

    private static final String LOOPBACK = "127.0.0.1";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void concurrencyLimitHandler_whenSaturated_answers503() throws Exception {
        // given a limit of 1 and a controller that blocks until we release it
        CountDownLatch controllerEntered = new CountDownLatch(1);
        CountDownLatch releaseController = new CountDownLatch(1);
        HttpHandler blockingController = exchange -> {
            controllerEntered.countDown();
            awaitQuietly(releaseController);
            sendText(exchange, 200, "done");
        };
        startServer(new NinjaHttpServer.ConcurrencyLimitHandler(1, blockingController));

        CompletableFuture<HttpResponse<String>> firstResponse
                = httpClient.sendAsync(get("/"), HttpResponse.BodyHandlers.ofString());
        assertThat(controllerEntered.await(5, TimeUnit.SECONDS)).isTrue();

        // when a second request arrives while the first one still holds the only permit
        HttpResponse<String> secondResponse = httpClient.send(get("/"), HttpResponse.BodyHandlers.ofString());
        releaseController.countDown();

        // then the second one is rejected right away and the first one completes normally
        assertThat(secondResponse.statusCode()).isEqualTo(503);
        assertThat(secondResponse.body()).contains("Too many requests");
        assertThat(firstResponse.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
    }

    @Test
    void concurrencyLimitHandler_whenSaturated_answersHeadRequestWithoutBody() throws Exception {
        // given a limit of 1 and a controller that blocks until we release it
        CountDownLatch controllerEntered = new CountDownLatch(1);
        CountDownLatch releaseController = new CountDownLatch(1);
        HttpHandler blockingController = exchange -> {
            controllerEntered.countDown();
            awaitQuietly(releaseController);
            sendText(exchange, 200, "done");
        };
        startServer(new NinjaHttpServer.ConcurrencyLimitHandler(1, blockingController));

        CompletableFuture<HttpResponse<String>> firstResponse
                = httpClient.sendAsync(get("/"), HttpResponse.BodyHandlers.ofString());
        assertThat(controllerEntered.await(5, TimeUnit.SECONDS)).isTrue();

        // when a HEAD request arrives while the first one still holds the only permit
        HttpRequest headRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://" + LOOPBACK + ":" + server.getAddress().getPort() + "/"))
                .timeout(Duration.ofSeconds(5))
                .HEAD()
                .build();
        HttpResponse<String> headResponse = httpClient.send(headRequest, HttpResponse.BodyHandlers.ofString());
        releaseController.countDown();

        // then it is rejected with 503 and no body, which the JDK server would otherwise refuse to send
        assertThat(headResponse.statusCode()).isEqualTo(503);
        assertThat(headResponse.body()).isEmpty();
        assertThat(firstResponse.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
    }

    @Test
    void concurrencyLimitHandler_releasesPermitWhenControllerThrows() throws Exception {
        // given a limit of 1 and a controller that throws on its first call
        AtomicInteger calls = new AtomicInteger();
        HttpHandler failingOnceController = exchange -> {
            if (calls.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }
            sendText(exchange, 200, "ok");
        };
        startServer(new NinjaHttpServer.ConcurrencyLimitHandler(1, failingOnceController));

        // when the first request fails (the JDK server just closes the connection).
        // POST, because HttpClient silently retries a failed GET, which would hide the failure.
        HttpRequest failingRequest = HttpRequest.newBuilder(get("/").uri())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            httpClient.send(failingRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException expected) {
            // no response for the failed request
        }
        HttpResponse<String> secondResponse = httpClient.send(get("/"), HttpResponse.BodyHandlers.ofString());

        // then the permit was given back and the next request is served
        assertThat(calls.get()).isEqualTo(2);
        assertThat(secondResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void jdkServer_closesConnectionOfClientThatSendsHeadersTooSlowly() throws Exception {
        // given a fresh JVM, because the JDK reads its timeout properties only once per JVM
        String javaBinary = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process child = new ProcessBuilder(javaBinary, "-cp", System.getProperty("java.class.path"),
                SlowClientTimeoutCheck.class.getName())
                .redirectErrorStream(true)
                .start();

        // when the child starts a server with maxReqTime=1s and a slowloris client talks to it
        boolean finished = child.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            child.destroyForcibly();
        }
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // then the server dropped the connection instead of waiting forever
        assertWithMessage(output).that(finished).isTrue();
        assertWithMessage(output).that(child.exitValue()).isEqualTo(SlowClientTimeoutCheck.CONNECTION_CLOSED_BY_SERVER);
    }

    @Test
    void setSystemPropertyIfAbsent_setsValueWhenMissing() {
        // given
        String key = "ninjax.test." + UUID.randomUUID();

        try {
            // when
            NinjaHttpServer.setSystemPropertyIfAbsent(key, "60");

            // then
            assertThat(System.getProperty(key)).isEqualTo("60");
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    void setSystemPropertyIfAbsent_keepsValueSetByUser() {
        // given a value the user passed via -D
        String key = "ninjax.test." + UUID.randomUUID();
        System.setProperty(key, "5");

        try {
            // when
            NinjaHttpServer.setSystemPropertyIfAbsent(key, "60");

            // then
            assertThat(System.getProperty(key)).isEqualTo("5");
        } finally {
            System.clearProperty(key);
        }
    }

    private void startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        server.createContext("/", handler);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://" + LOOPBACK + ":" + server.getAddress().getPort() + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
