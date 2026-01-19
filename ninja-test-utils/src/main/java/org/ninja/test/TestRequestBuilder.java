package org.ninja.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.ninja.core.Request;
import org.ninja.core.Result;
import org.ninja.core.Router;

/**
 * Fluent builder for creating test Request objects.
 * Extends the pattern from RequestTestHelper with more convenience methods.
 *
 * Example usage:
 * <pre>
 * Request request = TestRequestBuilder
 *     .create(router, "/tasks", "POST")
 *     .parameter("title", "Buy milk")
 *     .header("Content-Type", "application/x-www-form-urlencoded")
 *     .build();
 * </pre>
 */
public class TestRequestBuilder {

    private final Router router;
    private final String routePath;
    private final String httpMethod;
    private String requestPath;
    private final Map<String, String[]> parameters = new LinkedHashMap<>();
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private String body = "";
    private Locale language = Locale.ENGLISH;
    private Optional<org.ninja.core.NinjaSession> ninjaSession = Optional.empty();
    private List<org.ninja.core.NinjaCookie> ninjaCookies = new ArrayList<>();

    
    private TestRequestBuilder(Router router, String routePath, String httpMethod) {
        this.router = Objects.requireNonNull(router, "router");
        this.routePath = Objects.requireNonNull(routePath, "routePath");
        this.httpMethod = Objects.requireNonNull(httpMethod, "httpMethod");
        this.requestPath = routePath; // Default to route path
    }
    /**
     * Create a new TestRequestBuilder.
     *
     * @param router The router instance
     * @param routePath The route path (e.g., "/tasks")
     * @param httpMethod The HTTP method (e.g., "GET", "POST")
     * @return A new builder instance
     */
    public static TestRequestBuilder create(Router router, String routePath, String httpMethod) {
        return new TestRequestBuilder(router, routePath, httpMethod);
    }

    /**
     * Set the actual request path (useful when it differs from route path).
     *
     * @param requestPath The request path
     * @return This builder
     */
    public TestRequestBuilder requestPath(String requestPath) {
        this.requestPath = Objects.requireNonNull(requestPath, "requestPath");
        return this;
    }

    /**
     * Add a single parameter (form or query parameter).
     *
     * @param name Parameter name
     * @param value Parameter value
     * @return This builder
     */
    public TestRequestBuilder parameter(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        this.parameters.put(name, new String[]{value});
        return this;
    }

    /**
     * Add a multi-value parameter.
     *
     * @param name Parameter name
     * @param values Parameter values
     * @return This builder
     */
    public TestRequestBuilder parameter(String name, String... values) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(values, "values");
        this.parameters.put(name, values);
        return this;
    }

    /**
     * Add all parameters from a map.
     *
     * @param parameters Parameters to add
     * @return This builder
     */
    public TestRequestBuilder parameters(Map<String, String[]> parameters) {
        Objects.requireNonNull(parameters, "parameters");
        this.parameters.putAll(parameters);
        return this;
    }

    /**
     * Add a header.
     *
     * @param name Header name
     * @param value Header value
     * @return This builder
     */
    public TestRequestBuilder header(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Add all headers from a map.
     *
     * @param headers Headers to add
     * @return This builder
     */
    public TestRequestBuilder headers(Map<String, List<String>> headers) {
        Objects.requireNonNull(headers, "headers");
        for (var entry : headers.entrySet()) {
            for (var value : entry.getValue()) {
                header(entry.getKey(), value);
            }
        }
        return this;
    }

    /**
     * Set the request body.
     *
     * @param body Request body
     * @return This builder
     */
    public TestRequestBuilder body(String body) {
        this.body = Objects.requireNonNull(body, "body");
        return this;
    }

    /**
     * Set the language.
     *
     * @param language Language
     * @return This builder
     */
    public TestRequestBuilder language(Locale language) {
        this.language = Objects.requireNonNull(language, "language");
        return this;
    }

    /**
     * Set the ninja session.
     *
     * @param ninjaSession Session
     * @return This builder
     */
    public TestRequestBuilder session(org.ninja.core.NinjaSession ninjaSession) {
        this.ninjaSession = Optional.of(Objects.requireNonNull(ninjaSession, "ninjaSession"));
        return this;
    }

    /**
     * Add a cookie.
     *
     * @param cookie Cookie to add
     * @return This builder
     */
    public TestRequestBuilder cookie(org.ninja.core.NinjaCookie cookie) {
        this.ninjaCookies.add(Objects.requireNonNull(cookie, "cookie"));
        return this;
    }

    /**
     * Build the Request.
     *
     * @return The constructed Request
     */
    public Request build() {
        // Create the route
        Router.Route route = router.new Route(
                httpMethod,
                routePath,
                req -> Result.ok(), // dummy controller method
                List.of()
        );

        // Create input stream from body
        Request.InputStreamGetter inputStreamGetter =
                () -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));

        // File handling (not used in basic tests)
        Request.FileItemGetter fileItemGetter = fieldName -> Optional.empty();
        Request.FileItemsGetter fileItemsGetter = fieldName -> List.of();

        // Create payload from parameters
        var payload = new Request.Payload(Collections.unmodifiableMap(parameters));

        return Request.builder()
                .route(route)
                .requestPath(requestPath)
                .inputStreamGetter(inputStreamGetter)
                .fileItemGetter(fileItemGetter)
                .fileItemsGetter(fileItemsGetter)
                .ninjaCookies(List.copyOf(ninjaCookies))
                .payload(payload)
                .headers(Collections.unmodifiableMap(headers))
                .parameters(Collections.unmodifiableMap(parameters))
                .ninjaSession(ninjaSession)
                .language(language)
                .build();
    }
}
