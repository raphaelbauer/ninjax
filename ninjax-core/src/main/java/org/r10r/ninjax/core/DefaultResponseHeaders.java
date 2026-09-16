package org.r10r.ninjax.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Security headers every response gets, unless the Result already sets them. Both servers use this,
 * so they behave the same.
 */
public final class DefaultResponseHeaders {

    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    // "nosniff" stops browsers from guessing a different content type than the one we send,
    // e.g. running an uploaded text file as HTML or JavaScript.
    private static final Map<String, String> DEFAULTS = Map.of(X_CONTENT_TYPE_OPTIONS, "nosniff");

    private DefaultResponseHeaders() {
    }

    /**
     * @param resultHeaders the headers of the Result
     * @return the default headers that are not already present in resultHeaders (names compared ignoring case)
     */
    public static Map<String, String> missingIn(Map<String, List<String>> resultHeaders) {
        Map<String, String> missing = new LinkedHashMap<>();
        DEFAULTS.forEach((name, value) -> {
            boolean present = resultHeaders.keySet().stream().anyMatch(name::equalsIgnoreCase);
            if (!present) {
                missing.put(name, value);
            }
        });
        return missing;
    }
}
