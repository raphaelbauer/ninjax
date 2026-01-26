package org.ninjax.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.ninjax.core.Request;

public final class TestRequest {

    private TestRequest() {
        // utility
    }

    /**
     * Create a basic Request with reasonable defaults for tests.
     * Call .toBuilder() on the result to tweak as needed.
     */
    public static Request basic() {
        return Request.builder()
                .requestPath("/")
                .inputStreamGetter(() -> new ByteArrayInputStream(new byte[0]))
                .fileItemGetter(fieldName -> Optional.empty())
                .fileItemsGetter(fieldName -> List.of())
                .ninjaCookies(List.of())
                .payload(null) // or new Request.Payload(Map.of())
                .headers(new org.ninjax.core.Request.Headers())
                .parameters(new org.ninjax.core.Request.Parameters())
                .ninjaSession(Optional.empty())
                .language(Locale.ENGLISH)
                .pathParameters(Map.of())
                .build();
    }

    /**
     * Create a Request with a JSON body string.
     */
    public static Request withJsonBody(String path, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return Request.builder()
                .requestPath(path)
                .inputStreamGetter(() -> new ByteArrayInputStream(bytes))
                .fileItemGetter(fieldName -> Optional.empty())
                .fileItemsGetter(fieldName -> List.of())
                .ninjaCookies(List.of())
                .payload(null)
                .headers(new org.ninjax.core.Request.Headers(Map.of(
                        "Content-Type", List.of("application/json"),
                        "Content-Length", List.of(String.valueOf(bytes.length))
                )))
                .parameters(new org.ninjax.core.Request.Parameters())
                .ninjaSession(Optional.empty())
                .language(Locale.ENGLISH)
                .pathParameters(Map.of())
                .build();
    }

    public static Request.Builder builderFromBasic() {
        return basic().toBuilder();
    }

    public static Request.Builder builderFromJson(String path, String json) {
        return withJsonBody(path, json).toBuilder();
    }
}