package org.ninjax.core;

// FIXME NOT IMMUTABLE
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Result {

    private static Logger logger = LoggerFactory.getLogger(Result.class);

    public final static String CONTENT_TYPE_TEXT_HTML = "text/html";
    public final static String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
    public final static String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    // FIXME... THIS IS ALL NOT FINAL!
    int status = 200;
    String contentType = CONTENT_TYPE_TEXT_PLAIN;

    Optional<OutputStreamRenderer> outputStreamRenderer = Optional.empty();
    List<NinjaCookie> cookies = new ArrayList();
    
    
    public Result() {
        //this.outputStream = outputStream;
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
    
    public Result addCookie(NinjaCookie cookie) {
        cookies.add(cookie);
        return this;
    }

    public Result html(String content) {
        this.contentType = CONTENT_TYPE_TEXT_HTML;

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
        this.contentType = CONTENT_TYPE_APPLICATION_JSON;

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
        this.contentType = CONTENT_TYPE_TEXT_PLAIN;
     
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

}
