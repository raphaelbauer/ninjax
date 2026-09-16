package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestPathParameterTest {

    private static Request requestWithRawPathParameter(String rawValue) {
        return Request.builder()
                .requestPath("/p/" + rawValue)
                .pathParameters(Map.of("v", rawValue))
                .inputStreamGetter(InputStream::nullInputStream)
                .fileItemsGetter(fieldName -> List.of())
                .headers(new Request.Headers())
                .parameters(new Request.Parameters())
                .build();
    }

    @Test
    void percentEncodedCharacters_areDecoded() {
        // given
        Request request = requestWithRawPathParameter("my%20name%C3%A4");

        // when / then
        assertThat(request.getPathParameter("v")).hasValue("my nameä");
    }

    @Test
    void plus_staysPlus() {
        // given
        Request request = requestWithRawPathParameter("a+b");

        // when / then
        assertThat(request.getPathParameter("v")).hasValue("a+b");
    }

    @Test
    void encodedPercent_isDecodedOnlyOnce() {
        // given
        Request request = requestWithRawPathParameter("100%2525");

        // when / then
        assertThat(request.getPathParameter("v")).hasValue("100%25");
    }

    @Test
    void invalidEncoding_isEmptyInsteadOfThrowing() {
        // given
        Request request = requestWithRawPathParameter("100%");

        // when / then
        assertThat(request.getPathParameter("v")).isEmpty();
    }

    @Test
    void unknownParameter_isEmpty() {
        // given
        Request request = requestWithRawPathParameter("x");

        // when / then
        assertThat(request.getPathParameter("unknown")).isEmpty();
    }
}
