package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.server.NinjaHttpServer.NinjaHttpServerHelper;

class RequestHeadersTest {

    @Test
    void lookupIgnoresCase() {
        // given
        Request.Headers headers = new Request.Headers(Map.of("Content-Type", List.of("application/json")));

        // when / then
        assertThat(headers.get("content-type")).hasValue("application/json");
        assertThat(headers.get("CONTENT-TYPE")).hasValue("application/json");
        assertThat(headers.getAll("Content-type")).containsExactly("application/json");
    }

    @Test
    void namesDifferingOnlyInCase_areMerged() {
        // given
        Request.Headers headers = new Request.Headers(Map.of(
                "X-Forwarded-For", List.of("1.1.1.1"),
                "x-forwarded-for", List.of("2.2.2.2")));

        // when
        List<String> values = headers.getAll("X-FORWARDED-FOR");

        // then
        assertThat(values).containsExactly("1.1.1.1", "2.2.2.2");
    }

    @Test
    void missingHeader_isEmpty() {
        // given
        Request.Headers headers = new Request.Headers();

        // when / then
        assertThat(headers.get("Authorization")).isEmpty();
        assertThat(headers.getAll("Authorization")).isEmpty();
    }

    @Test
    void headersAreImmutable() {
        // given
        Request.Headers headers = new Request.Headers(Map.of("Accept", List.of("text/html")));

        // when / then
        assertThrows(UnsupportedOperationException.class, () -> headers.getHeaders().put("Accept", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> headers.getAll("Accept").add("x"));
    }

    @Test
    void headersFromJdkHttpServer_canBeReadWithTheirUsualSpelling() {
        // given
        // The JDK HttpServer normalizes names: "Content-Type" is stored as "Content-type".
        var jdkHeaders = new com.sun.net.httpserver.Headers();
        jdkHeaders.add("Content-Type", "text/plain");
        jdkHeaders.add("X-CSRF-Token", "abc");

        // when
        Request.Headers headers = NinjaHttpServerHelper.extractHeaders(jdkHeaders);

        // then
        assertThat(headers.get("Content-Type")).hasValue("text/plain");
        assertThat(headers.get("X-CSRF-Token")).hasValue("abc");
    }
}
