package org.ninja.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simple HTTP client for integration testing.
 *
 * Example usage:
 * <pre>
 * HttpTestClient client = HttpTestClient.localhost(8080);
 * HttpTestResponse response = client.get("/tasks");
 * HttpTestResponse postResponse = client.post("/tasks", Map.of("title", "Buy milk"));
 * </pre>
 */
public class HttpTestClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a new HTTP test client.
     *
     * @param baseUrl The base URL (e.g., "http://localhost:8080")
     */
    public HttpTestClient(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Create a client for localhost with the given port.
     *
     * @param port The port number
     * @return A new client instance
     */
    public static HttpTestClient localhost(int port) {
        return new HttpTestClient("http://localhost:" + port);
    }

    /**
     * Perform a GET request.
     *
     * @param path The path (e.g., "/tasks")
     * @return The response
     * @throws IOException If an I/O error occurs
     */
    public HttpTestResponse get(String path) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return executeRequest(request);
    }

    /**
     * Perform a POST request with form data.
     *
     * @param path The path (e.g., "/tasks")
     * @param formData The form data
     * @return The response
     * @throws IOException If an I/O error occurs
     */
    public HttpTestResponse post(String path, Map<String, String> formData) throws IOException {
        String body = formData.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return executeRequest(request);
    }

    /**
     * Perform a POST request with JSON body.
     *
     * @param path The path (e.g., "/api/tasks")
     * @param jsonObject The object to serialize as JSON
     * @return The response
     * @throws IOException If an I/O error occurs
     */
    public HttpTestResponse postJson(String path, Object jsonObject) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(jsonObject);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return executeRequest(request);
    }

    /**
     * Perform a POST request with no body.
     *
     * @param path The path
     * @return The response
     * @throws IOException If an I/O error occurs
     */
    public HttpTestResponse post(String path) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return executeRequest(request);
    }

    /**
     * Perform a DELETE request.
     *
     * @param path The path
     * @return The response
     * @throws IOException If an I/O error occurs
     */
    public HttpTestResponse delete(String path) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .DELETE()
                .build();
        return executeRequest(request);
    }

    private HttpTestResponse executeRequest(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpTestResponse(
                    response.statusCode(),
                    response.body(),
                    response.headers().map()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Response from an HTTP test request.
     */
    public static class HttpTestResponse {
        private final int statusCode;
        private final String body;
        private final Map<String, java.util.List<String>> headers;

        HttpTestResponse(int statusCode, String body, Map<String, java.util.List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }

        public int statusCode() {
            return statusCode;
        }

        public String body() {
            return body;
        }

        public Map<String, java.util.List<String>> headers() {
            return headers;
        }

        /**
         * Parse the response body as JSON.
         *
         * @param clazz The class to deserialize to
         * @param <T> The type
         * @return The deserialized object
         * @throws IOException If parsing fails
         */
        public <T> T bodyAsJson(Class<T> clazz) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(body, clazz);
        }
    }
}
