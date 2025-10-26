package org.ninjax.core;

// FIXME NOT IMMUTABLE
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Result {

    private static Logger logger = LoggerFactory.getLogger(Result.class);

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
    public static final String CACHE_CONTROL_DEFAULT_NOCACHE_VALUE = "no-cache, no-store, max-age=0, must-revalidate";

    public static final String DATE = "Date";
    public static final String EXPIRES = "Expires";

    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    // FIXME... THIS IS ALL NOT FINAL!
    int status = 200;
    String contentType = TEXT_PLAIN;

    Optional<OutputStreamRenderer> outputStreamRenderer = Optional.empty();
    List<NinjaCookie> cookies = new ArrayList();

    Map<String, List<String>> headers = new HashMap();

    NinjaSessionState ninjaSessionState;

    public Result() {
        ninjaSessionState = new UnknownButDontTouch();
    }

    public static Result ok() {
        return new Result().status(200);
    }

    public static Result notFound() {
        return new Result().status(200);
    }

    public static Result badRequest() {
        return new Result().status(400);
    }
    
    public static Result internalServerError() {
        return new Result().status(500);
    }

    public static Result redirect(String url) {
        Result result = new Result().status(303);
        result.addHeader(LOCATION, url);

        return result;
    }

    public Result addCookie(NinjaCookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public Result addHeader(String key, String value) {
        headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        return this;
    }

    public Result withNinjaSession(NinjaSession ninjaSession) {
        this.ninjaSessionState = new Exists(ninjaSession);
        return this;
    }
    
    public Result deleteNinjaSession() {
        this.ninjaSessionState = new Remove();
        return this;
    }

    public Result html(String content) {
        this.contentType = TEXT_HTML;

        OutputStreamRenderer outputStreamRenderer = outputStream -> {
            try {
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("Rendering went wrong. Ouch! ", e);
            }
        };
        this.outputStreamRenderer = Optional.of(outputStreamRenderer);

        return this;
    }

    public Result json(Object objectToRenderAsJson) {
        this.contentType = APPLICATION_JSON;

        OutputStreamRenderer outputStreamRenderer = outputStream -> {
            try {
                // NOT ideal as Json is statc and not injected...
                Json.objectMapper.writeValue(outputStream, objectToRenderAsJson);
            } catch (IOException e) {
                logger.error("Rendering went wrong. Ouch! ", e);
            }
        };
        this.outputStreamRenderer = Optional.of(outputStreamRenderer);

        return this;
    }

    public Result contentType(String contentType) {
        this.contentType = contentType;
        //FIXME not immutable
        return this;
    }

    public Result status(int status) {
        this.status = status;
        return this;
    }

    public Result text(String content) {
        this.contentType = TEXT_PLAIN;

        OutputStreamRenderer outputStreamRenderer = outputStream -> {
            try {
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("Rendering went wrong. Ouch! ", e);
            }
        };
        this.outputStreamRenderer = Optional.of(outputStreamRenderer);

        return this;
    }

    public Result stream(OutputStreamRenderer outputStreamRenderer) {
        this.outputStreamRenderer = Optional.of(outputStreamRenderer);
        return this;
    }

    public interface OutputStreamRenderer {

        void streamTo(OutputStream outputStream);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Make sure we can't mess up the Ninja Session State.
    // Sealed classes to the rescue!
    public sealed interface NinjaSessionState permits Exists, Remove, UnknownButDontTouch {
    }

    public final class Exists implements NinjaSessionState {

        private final NinjaSession session;

        public Exists(NinjaSession session) {
            this.session = session;
        }

        public NinjaSession getSession() {
            return session;
        }
    }

    public final class Remove implements NinjaSessionState {
        // No session field!
    }

    public final class UnknownButDontTouch implements NinjaSessionState {
        // No session field!
    }
    // end 
    ////////////////////////////////////////////////////////////////////////////

}
