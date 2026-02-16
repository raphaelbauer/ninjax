package com.r10r.ninjax.test;

import static com.google.common.truth.Fact.fact;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import java.util.List;
import com.r10r.ninjax.core.Result;

/**
 * Google Truth custom assertions for Result objects.
 *
 * Example usage:
 * <pre>
 * import static com.r10r.ninjax.test.ResultAssertions.assertThat;
 *
 * assertThat(result).isOk();
 * assertThat(result).hasStatus(200);
 * assertThat(result).hasRedirectTo("/");
 * assertThat(result).hasJsonContent();
 * </pre>
 */
public class ResultAssertions extends Subject {

    private final Result actual;

    protected ResultAssertions(FailureMetadata metadata, Result actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    /**
     * Create assertions for a Result.
     *
     * @param result The result to assert on
     * @return Assertions for the result
     */
    public static ResultAssertions assertThat(Result result) {
        return Truth.assertAbout(ResultAssertions::new).that(result);
    }

    /**
     * Assert that the result has the expected status code.
     *
     * @param expectedStatus The expected status code
     */
    public void hasStatus(int expectedStatus) {
        check("status()").that(actual.status()).isEqualTo(expectedStatus);
    }

    /**
     * Assert that the result has a 200 OK status.
     */
    public void isOk() {
        hasStatus(Result.SC_200_OK);
    }

    /**
     * Assert that the result has a 201 Created status.
     */
    public void isCreated() {
        hasStatus(Result.SC_201_CREATED);
    }

    /**
     * Assert that the result has a 204 No Content status.
     */
    public void isNoContent() {
        hasStatus(Result.SC_204_NO_CONTENT);
    }

    /**
     * Assert that the result has a 400 Bad Request status.
     */
    public void isBadRequest() {
        hasStatus(Result.SC_400_BAD_REQUEST);
    }

    /**
     * Assert that the result has a 401 Unauthorized status.
     */
    public void isUnauthorized() {
        hasStatus(Result.SC_401_UNAUTHORIZED);
    }

    /**
     * Assert that the result has a 403 Forbidden status.
     */
    public void isForbidden() {
        hasStatus(Result.SC_403_FORBIDDEN);
    }

    /**
     * Assert that the result has a 404 Not Found status.
     */
    public void isNotFound() {
        hasStatus(Result.SC_404_NOT_FOUND);
    }

    /**
     * Assert that the result has a 500 Internal Server Error status.
     */
    public void isInternalServerError() {
        hasStatus(Result.SC_500_INTERNAL_SERVER_ERROR);
    }

    /**
     * Assert that the result is a redirect (3xx status).
     */
    public void isRedirect() {
        int status = actual.status();
        if (status < 300 || status >= 400) {
            failWithActual(fact("expected", "to be redirect (3xx)"));
        }
    }

    /**
     * Assert that the result has the expected content type.
     *
     * @param expectedContentType The expected content type
     */
    public void hasContentType(String expectedContentType) {
        check("contentType()").that(actual.contentType()).isEqualTo(expectedContentType);
    }

    /**
     * Assert that the result has HTML content type.
     */
    public void hasHtmlContent() {
        hasContentType(Result.TEXT_HTML);
    }

    /**
     * Assert that the result has JSON content type.
     */
    public void hasJsonContent() {
        hasContentType(Result.APPLICATION_JSON);
    }

    /**
     * Assert that the result has plain text content type.
     */
    public void hasTextContent() {
        hasContentType(Result.TEXT_PLAIN);
    }

    /**
     * Assert that the result has a Location header with the expected value.
     *
     * @param expectedLocation The expected redirect location
     */
    public void hasRedirectTo(String expectedLocation) {
        List<String> locationHeaders = actual.headers().get(Result.LOCATION);
        if (locationHeaders == null || locationHeaders.isEmpty()) {
            failWithoutActual(fact("expected to have Location header", expectedLocation));
        } else {
            check("headers().get(\"Location\").get(0)").that(locationHeaders.get(0)).isEqualTo(expectedLocation);
        }
    }

    /**
     * Assert that the result has the expected header.
     *
     * @param headerName The header name
     * @param expectedValue The expected header value
     */
    public void hasHeader(String headerName, String expectedValue) {
        List<String> headerValues = actual.headers().get(headerName);
        if (headerValues == null || headerValues.isEmpty()) {
            failWithoutActual(fact("expected to have header", headerName));
        } else {
            check("headers().get(\"" + headerName + "\").get(0)").that(headerValues.get(0)).isEqualTo(expectedValue);
        }
    }

    /**
     * Assert that the result has a header with the given name.
     *
     * @param headerName The header name
     */
    public void hasHeader(String headerName) {
        List<String> headerValues = actual.headers().get(headerName);
        if (headerValues == null || headerValues.isEmpty()) {
            failWithoutActual(fact("expected to have header", headerName));
        }
    }

    /**
     * Assert that the result has a cookie with the given name.
     *
     * @param cookieName The cookie name
     */
    public void hasCookie(String cookieName) {
        boolean found = actual.cookies().stream()
                .anyMatch(cookie -> cookie.name().equals(cookieName));
        if (!found) {
            failWithoutActual(fact("expected to have cookie", cookieName));
        }
    }

    /**
     * Assert that the result has an output stream renderer (i.e., has content).
     */
    public void hasContent() {
        if (actual.outputStreamRenderer().isEmpty()) {
            failWithoutActual(fact("expected", "to have content"));
        }
    }

    /**
     * Assert that the result has no output stream renderer (i.e., has no content).
     */
    public void hasNoContent() {
        if (actual.outputStreamRenderer().isPresent()) {
            failWithoutActual(fact("expected", "to have no content"));
        }
    }
}
