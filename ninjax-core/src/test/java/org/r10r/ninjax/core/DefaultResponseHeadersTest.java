package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultResponseHeadersTest {

    @Test
    void addsNosniff_whenResultDoesNotSetIt() {
        // given
        Map<String, List<String>> resultHeaders = Map.of("Location", List.of("/"));

        // when
        Map<String, String> missing = DefaultResponseHeaders.missingIn(resultHeaders);

        // then
        assertThat(missing).containsExactly("X-Content-Type-Options", "nosniff");
    }

    @Test
    void keepsValueOfResult_evenIfNameDiffersInCase() {
        // given
        Map<String, List<String>> resultHeaders = Map.of("x-content-type-options", List.of("custom"));

        // when
        Map<String, String> missing = DefaultResponseHeaders.missingIn(resultHeaders);

        // then
        assertThat(missing).isEmpty();
    }

    @Test
    void htmlAndText_declareUtf8() {
        // when
        Result html = Result.builder().html("<p>ä</p>").build();
        Result text = Result.builder().text("ä").build();

        // then
        assertThat(html.contentType()).isEqualTo("text/html; charset=utf-8");
        assertThat(text.contentType()).isEqualTo("text/plain; charset=utf-8");
    }
}
