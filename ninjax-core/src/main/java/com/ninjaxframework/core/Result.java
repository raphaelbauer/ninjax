package com.ninjaxframework.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Result(
        int status,
        String contentType,
        Optional<Result.OutputStreamRenderer> outputStreamRenderer,
        List<NinjaCookie> cookies,
        Map<String, List<String>> headers,
        Result.NinjaSessionState ninjaSessionState
) {

    private static final Logger logger = LoggerFactory.getLogger(Result.class);

    // /////////////////////////////////////////////////////////////////////////
    // HTTP Status codes (for convenience)
    // /////////////////////////////////////////////////////////////////////////
    public static final int SC_101_SWITCHING_PROTOCOLS = 101;
    public static final int SC_200_OK = 200;
    public static final int SC_201_CREATED = 201;
    public static final int SC_204_NO_CONTENT = 204;

    public static final int SC_300_MULTIPLE_CHOICES = 300;
    public static final int SC_301_MOVED_PERMANENTLY = 301;
    public static final int SC_302_FOUND = 302;
    public static final int SC_303_SEE_OTHER = 303;
    public static final int SC_304_NOT_MODIFIED = 304;
    public static final int SC_307_TEMPORARY_REDIRECT = 307;

    public static final int SC_400_BAD_REQUEST = 400;
    public static final int SC_401_UNAUTHORIZED = 401;
    public static final int SC_403_FORBIDDEN = 403;
    public static final int SC_404_NOT_FOUND = 404;

    public static final int SC_500_INTERNAL_SERVER_ERROR = 500;
    public static final int SC_501_NOT_IMPLEMENTED = 501;

    // /////////////////////////////////////////////////////////////////////////
    // Some MIME types (for convenience)
    // /////////////////////////////////////////////////////////////////////////
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_JSONP = "application/javascript";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    public static final String LOCATION = "Location";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CACHE_CONTROL_DEFAULT_NOCACHE_VALUE
            = "no-cache, no-store, max-age=0, must-revalidate";

    public static final String DATE = "Date";
    public static final String EXPIRES = "Expires";

    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    // Canonical constructor – enforce defensive copies and non‑nulls
    public Result {
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(outputStreamRenderer, "outputStreamRenderer");
        Objects.requireNonNull(cookies, "cookies");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(ninjaSessionState, "ninjaSessionState");

        // freeze cookies
        cookies = List.copyOf(cookies);

        // deep‑freeze headers
        Map<String, List<String>> tmp = new LinkedHashMap<>();
        for (var e : headers.entrySet()) {
            tmp.put(e.getKey(), List.copyOf(e.getValue()));
        }
        headers = Collections.unmodifiableMap(tmp);
    }

    // ------------------------------------------------------------------------
    // Convenience factory methods

    public static Result redirect(String route) {
        return Result.builder()
                .redirect(route)
                .build();
    }

    public static Result ok() {
        return Result.builder()
                .status(SC_200_OK)
                .build();
    }

    public static Result ok(String body) {
        return Result.builder()
                .status(SC_200_OK)
                .text(body)
                .build();
    }

    public static Result notFound() {
        return Result.builder()
                .status(SC_404_NOT_FOUND)
                .build();
    }
    
    public static Result badRequest() {
        return Result.builder()
                .badRequest()
                .build();
    }
    
    public static Result badRequest(String body) {
        return Result.builder()
                .badRequest()
                .text(body)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------------
    public interface OutputStreamRenderer {
        void streamTo(OutputStream outputStream);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Sealed session state types
    // //////////////////////////////////////////////////////////////////////////
    public sealed interface NinjaSessionState permits Exists, Remove, UnknownButDontTouch {
    }

    public static final class Exists implements NinjaSessionState {
        private final NinjaSession session;

        public Exists(NinjaSession session) {
            this.session = Objects.requireNonNull(session, "session");
        }

        public NinjaSession getSession() {
            return session;
        }
    }

    public static final class Remove implements NinjaSessionState {
        // No session field!
    }

    public static final class UnknownButDontTouch implements NinjaSessionState {
        // No session field!
    }

    // ------------------------------------------------------------------------
    // Builder (unchanged public API, but builds the record)
    public static final class Builder {

        private int status = SC_200_OK;
        private String contentType = TEXT_PLAIN;
        private OutputStreamRenderer outputStreamRenderer = null;
        private final List<NinjaCookie> cookies = new ArrayList<>();
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private NinjaSessionState ninjaSessionState = new UnknownButDontTouch();

        public Builder() {
        }

        public Result build() {
            return new Result(
                    status,
                    contentType,
                    Optional.ofNullable(outputStreamRenderer),
                    cookies,
                    headers,
                    ninjaSessionState
            );
        }

        public Builder ok() {
            return builder().status(SC_200_OK);
        }

        public Builder notFound() {
            return builder().status(SC_404_NOT_FOUND);
        }

        public Builder badRequest() {
            return builder().status(SC_400_BAD_REQUEST);
        }

        public Builder internalServerError() {
            return builder().status(SC_500_INTERNAL_SERVER_ERROR);
        }

        public Builder redirect(String url) {
            return builder()
                    .status(SC_303_SEE_OTHER)
                    .addHeader(LOCATION, url);
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = Objects.requireNonNull(contentType, "contentType");
            return this;
        }

        public Builder addCookie(NinjaCookie cookie) {
            this.cookies.add(Objects.requireNonNull(cookie, "cookie"));
            return this;
        }

        public Builder addHeader(String key, String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder withNinjaSession(NinjaSession ninjaSession) {
            this.ninjaSessionState = new Exists(ninjaSession);
            return this;
        }

        public Builder deleteNinjaSession() {
            this.ninjaSessionState = new Remove();
            return this;
        }

        public Builder html(String content) {
            this.contentType = TEXT_HTML;
            this.outputStreamRenderer = outputStream -> {
                try {
                    outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error("Rendering went wrong. Ouch! ", e);
                }
            };
            return this;
        }

        public Builder text(String content) {
            this.contentType = TEXT_PLAIN;
            this.outputStreamRenderer = outputStream -> {
                try {
                    outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error("Rendering went wrong. Ouch! ", e);
                }
            };
            return this;
        }

        public Builder json(OutputStreamRenderer outputStreamRenderer) {
            this.contentType = APPLICATION_JSON;
            this.outputStreamRenderer = outputStreamRenderer;
            return this;
        }

        public Builder stream(OutputStreamRenderer outputStreamRenderer) {
            this.outputStreamRenderer = Objects.requireNonNull(outputStreamRenderer, "outputStreamRenderer");
            return this;
        }
    }
}
