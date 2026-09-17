package org.r10r.ninjax.core.server;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.core.Router;
import org.r10r.ninjax.core.properties.NinjaProperties;

/**
 * Starts a real NinjaHttpServer on an ephemeral port and talks raw HTTP to it, because a regular
 * HTTP client silently drops any body sent in a HEAD response.
 */
class NinjaHttpServerHeadRequestTest {

    private static final AtomicBoolean bodyWasRendered = new AtomicBoolean(false);
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        Router router = new Router();
        router.GET("/hello").with(request -> Result.builder()
                .status(Result.SC_200_OK)
                .addHeader("X-Route", "get")
                .stream(outputStream -> {
                    bodyWasRendered.set(true);
                    try {
                        outputStream.write("hello".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build());

        port = findFreePort();
        NinjaProperties ninjaProperties = propertiesWith(Map.of(
                "ninja.port", String.valueOf(port),
                "application.secret", Base64.getEncoder().encodeToString(new byte[32])));

        // The constructor blocks until the server stops, so it runs in a daemon thread.
        Thread serverThread = new Thread(() -> new NinjaHttpServer(router, ninjaProperties));
        serverThread.setDaemon(true);
        serverThread.start();

        waitUntilListening(port);
    }

    @BeforeEach
    void resetRenderFlag() {
        bodyWasRendered.set(false);
    }

    @Test
    void headRequest_withoutHeadRoute_usesGetRouteButSendsNoBody() throws Exception {
        // given a router with only a GET /hello route

        // when
        String response = sendRawRequest("HEAD", "/hello");

        // then status and headers come from the GET route, but there is no body
        assertThat(response).startsWith("HTTP/1.1 200");
        assertThat(response).containsMatch("(?i)x-route: get");
        assertThat(bodyOf(response)).isEmpty();
        assertThat(bodyWasRendered.get()).isFalse();
    }

    @Test
    void headRequest_unknownPath_returns404WithoutBody() throws Exception {
        // given no route for /unknown

        // when
        String response = sendRawRequest("HEAD", "/unknown");

        // then
        assertThat(response).startsWith("HTTP/1.1 404");
        assertThat(bodyOf(response)).isEmpty();
    }

    @Test
    void getRequest_stillSendsBody() throws Exception {
        // given a router with a GET /hello route

        // when
        String response = sendRawRequest("GET", "/hello");

        // then
        assertThat(response).startsWith("HTTP/1.1 200");
        assertThat(bodyOf(response)).contains("hello");
        assertThat(bodyWasRendered.get()).isTrue();
    }

    private static NinjaProperties propertiesWith(Map<String, String> properties) {
        return new NinjaProperties() {
            @Override
            public Optional<String> get(String propertyName) {
                return Optional.ofNullable(properties.get(propertyName));
            }
        };
    }

    private static String sendRawRequest(String method, String path) throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(5000);
            OutputStream out = socket.getOutputStream();
            out.write((method + " " + path + " HTTP/1.1\r\n"
                    + "Host: localhost\r\n"
                    + "Connection: close\r\n"
                    + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
            out.flush();

            // Connection: close makes the server close the socket after the response, so this reads exactly one response.
            InputStream in = socket.getInputStream();
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static String bodyOf(String rawResponse) {
        int endOfHeaders = rawResponse.indexOf("\r\n\r\n");
        assertThat(endOfHeaders).isAtLeast(0);
        return rawResponse.substring(endOfHeaders + 4);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitUntilListening(int port) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port), 200);
                return;
            } catch (IOException notYetListening) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Server did not start on port " + port);
    }
}
