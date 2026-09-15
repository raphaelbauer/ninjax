package org.r10r.ninjax.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.r10r.ninjax.core.Request;
import org.r10r.ninjax.core.server.NinjaHttpServer.NinjaHttpServerHelper;
import org.r10r.ninjax.core.Result;
import static org.r10r.ninjax.core.Result.APPLICATION_JSON;
import java.util.logging.Level;
import java.util.logging.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Very simple and basic Json rendering.
 *
 * You may want to create your own class if you have different requirements
 */
public class Json {
    
    private static final Logger logger = Logger.getLogger(Json.class.getName());

    // Jackson 3 supports java.time and Optional out of the box - no extra modules needed.
    private final static JsonMapper objectMapper = JsonMapper.builder().build();

    
    /**
     * This method returns an Optional of your class.
     * 
     * Optional&lt;List&lt;TestRecord&gt;&gt; result = json.getJsonBody(request, new TypeReference&lt;List&lt;TestRecord&gt;&gt;() {});
     * The TypeReference allows to use Generics freely in your deserializations.
     * 
     * If there&#39;s an error during serialization you will get an empty Optional.
     *
     * Exception: if the request body exceeds the server&#39;s maxUploadBytes limit an
     * UncheckedIOException is thrown, so the server can answer with 413 Payload Too Large
     * instead of treating the request like invalid JSON.
     *
     * @param <A> The class you want to deserialize
     * @param request The request that contains a link to the input stream that then will be deserialized to Json
     * @param typeRef The TypeReference that allows you to deserialize more complex generics.
     * @return s The class that was created from the Json.
     */
    public <A> Optional<A> getJsonBody(Request request, TypeReference<A> typeRef) {
        try (var inputStream = request.getInputStreamGetter().get()) {
            return Optional.of(objectMapper.readValue(inputStream, typeRef));
        } catch (IOException | JacksonException ex) {
            if (NinjaHttpServerHelper.isCausedByPayloadTooLarge(ex)) {
                throw new UncheckedIOException(new IOException("Request body exceeds maxUploadBytes", ex));
            }
            logger.log(Level.SEVERE, "Opsi", ex);
            return Optional.empty();
        }
    }
    
    public org.r10r.ninjax.core.Result.OutputStreamRenderer json(Object objectToRenderAsJson) {
            org.r10r.ninjax.core.Result.OutputStreamRenderer outputStreamRenderer = outputStream -> {
                try {
                    Json.objectMapper.writeValue(outputStream, objectToRenderAsJson);
                } catch (JacksonException e) {
                    logger.log(Level.SEVERE, "Rendering went wrong. Ouch! ", e);
                }
            };
            return outputStreamRenderer;
        }
}
