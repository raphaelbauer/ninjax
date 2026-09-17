package org.r10r.ninjax.test;

import static com.google.common.truth.Truth.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the client against a plain JDK HttpServer that echoes the method and request body.
 */
class HttpTestClientTest {

    private HttpServer echoServer;
    private HttpTestClient client;

    @BeforeEach
    void startEchoServer() throws IOException {
        echoServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        echoServer.createContext("/", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = (exchange.getRequestMethod() + " " + requestBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("X-Method", exchange.getRequestMethod());
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(response);
                }
            }
            exchange.close();
        });
        echoServer.start();
        client = HttpTestClient.localhost(echoServer.getAddress().getPort());
    }

    @AfterEach
    void stopEchoServer() {
        echoServer.stop(0);
    }

    @Test
    void putJson_sendsPutWithJsonBody() throws IOException {
        // given
        Map<String, String> task = Map.of("title", "Buy milk");

        // when
        HttpTestClient.HttpTestResponse response = client.putJson("/tasks/1", task);

        // then
        assertThat(response.body()).isEqualTo("PUT {\"title\":\"Buy milk\"}");
    }

    @Test
    void patchJson_sendsPatchWithJsonBody() throws IOException {
        // given
        Map<String, String> task = Map.of("title", "Buy milk");

        // when
        HttpTestClient.HttpTestResponse response = client.patchJson("/tasks/1", task);

        // then
        assertThat(response.body()).isEqualTo("PATCH {\"title\":\"Buy milk\"}");
    }

    @Test
    void head_sendsHeadAndReturnsHeaders() throws IOException {
        // given a running echo server

        // when
        HttpTestClient.HttpTestResponse response = client.head("/tasks");

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().get("x-method")).containsExactly("HEAD");
        assertThat(response.body()).isEmpty();
    }
}
