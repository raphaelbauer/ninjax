package org.ninjax.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Result {

    private static final Logger logger = LoggerFactory.getLogger(Result.class);

    // /////////////////////////////////////////////////////////////////////////
    // HTTP Status codes (for convenience)
    // /////////////////////////////////////////////////////////////////////////
    public static final int SC_101_SWITCHING_PROTOCOLS = 101;
    public static final int SC_200_OK = 200;
    public static final int SC_201_CREATED = 201;
    public static final int SC_204_NO_CONTENT = 204;

    // for redirects:
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
    public static final String CACHE_CONTROL_DEFAULT_NOCACHE_VALUE =
            "no-cache, no-store, max-age=0, must-revalidate";

    public static final String DATE = "Date";
    public static final String EXPIRES = "Expires";

    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    // ------------------------------------------------------------------------
    // Immutable state
    private final int status;
    private final String contentType;
    private final Optional<OutputStreamRenderer> outputStreamRenderer;
    private final List<NinjaCookie> cookies;                 // unmodifiable
    private final Map<String, List<String>> headers;          // deep unmodifiable
    private final NinjaSessionState ninjaSessionState;

    private Result(Builder b) {
        this.status = b.status;
        this.contentType = b.contentType;
        this.outputStreamRenderer = Optional.ofNullable(b.outputStreamRenderer);

        // defensive copies + freeze
        this.cookies = List.copyOf(b.cookies);

        Map<String, List<String>> tmp = new LinkedHashMap<>();
        for (var e : b.headers.entrySet()) {
            tmp.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.headers = Collections.unmodifiableMap(tmp);

        this.ninjaSessionState = b.ninjaSessionState;
    }

    // ------------------------------------------------------------------------
    // Factories
//    public static Result ok() {
//        return builder().status(SC_200_OK).build();
//    }
//
//    public static Result notFound() {
//        return builder().status(SC_404_NOT_FOUND).build(); // fixed (was 200)
//    }
//
//    public static Result badRequest() {
//        return builder().status(SC_400_BAD_REQUEST).build();
//    }
//
//    public static Result internalServerError() {
//        return builder().status(SC_500_INTERNAL_SERVER_ERROR).build();
//    }
//
//    public static Result redirect(String url) {
//        return builder()
//                .status(SC_303_SEE_OTHER)
//                .addHeader(LOCATION, url)
//                .build();
//    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }
    
    public static Builder ok() {
        return builder().status(SC_200_OK);
    }

    public static Builder notFound() {
        return builder().status(SC_404_NOT_FOUND);
    }

    public static Builder badRequest() {
        return builder().status(SC_400_BAD_REQUEST);
    }

    public static Builder internalServerError() {
        return builder().status(SC_500_INTERNAL_SERVER_ERROR);
    }

    public static Builder redirect(String url) {
        return builder()
                .status(SC_303_SEE_OTHER)
                .addHeader(LOCATION, url);
    }
        
        

    // ------------------------------------------------------------------------
    // "Fluent" immutable modifiers (return new Result)
//    public Result addCookie(NinjaCookie cookie) {
//        return toBuilder().addCookie(cookie).build();
//    }
//
//    public Result addHeader(String key, String value) {
//        return toBuilder().addHeader(key, value).build();
//    }
//
//    public Result withNinjaSession(NinjaSession ninjaSession) {
//        return toBuilder().withNinjaSession(ninjaSession).build();
//    }
//
//    public Result deleteNinjaSession() {
//        return toBuilder().deleteNinjaSession().build();
//    }
//
//    public Result html(String content) {
//        return toBuilder().html(content).build();
//    }
//
//    public Result json(Object objectToRenderAsJson) {
//        return toBuilder().json(objectToRenderAsJson).build();
//    }
//
//    public Result contentType(String contentType) {
//        return toBuilder().contentType(contentType).build();
//    }
//
//    public Result status(int status) {
//        return toBuilder().status(status).build();
//    }
//
//    public Result text(String content) {
//        return toBuilder().text(content).build();
//    }
//
//    public Result stream(OutputStreamRenderer outputStreamRenderer) {
//        return toBuilder().stream(outputStreamRenderer).build();
//    }

    // ------------------------------------------------------------------------
    // Getters (since fields are now private final)
    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public Optional<OutputStreamRenderer> getOutputStreamRenderer() {
        return outputStreamRenderer;
    }

    public List<NinjaCookie> getCookies() {
        return cookies;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public NinjaSessionState getNinjaSessionState() {
        return ninjaSessionState;
    }

    // ------------------------------------------------------------------------
    public interface OutputStreamRenderer {
        void streamTo(OutputStream outputStream);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Make sure we can't mess up the Ninja Session State.
    // Sealed classes to the rescue!
    public sealed interface NinjaSessionState permits Exists, Remove, UnknownButDontTouch {}

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
    // end
    // //////////////////////////////////////////////////////////////////////////

    // ------------------------------------------------------------------------
    // Builder
    public static final class Builder {
        private int status = SC_200_OK;
        private String contentType = TEXT_PLAIN;
        private OutputStreamRenderer outputStreamRenderer = null;
        private final List<NinjaCookie> cookies = new ArrayList<>();
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private NinjaSessionState ninjaSessionState = new UnknownButDontTouch();

        public Builder() {}

        private Builder(Result r) {
            this.status = r.status;
            this.contentType = r.contentType;
            this.outputStreamRenderer = r.outputStreamRenderer.orElse(null);
            this.cookies.addAll(r.cookies);
            for (var e : r.headers.entrySet()) {
                this.headers.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            this.ninjaSessionState = r.ninjaSessionState;
        }

        public Result build() {
            return new Result(this);
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

        public Builder json(Object objectToRenderAsJson) {
            this.contentType = APPLICATION_JSON;
            this.outputStreamRenderer = outputStream -> {
                try {
                    // still static, per your original code
                    Json.objectMapper.writeValue(outputStream, objectToRenderAsJson);
                } catch (IOException e) {
                    logger.error("Rendering went wrong. Ouch! ", e);
                }
            };
            return this;
        }

        public Builder stream(OutputStreamRenderer outputStreamRenderer) {
            this.outputStreamRenderer = Objects.requireNonNull(outputStreamRenderer, "outputStreamRenderer");
            return this;
        }
    }
}
