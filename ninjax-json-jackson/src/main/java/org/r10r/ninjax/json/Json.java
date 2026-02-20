package org.r10r.ninjax.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Optional;
import org.r10r.ninjax.core.Request;
import org.r10r.ninjax.core.Result;
import static org.r10r.ninjax.core.Result.APPLICATION_JSON;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Very simple and basic Json rendering.
 *
 * You may want to create your own class if you have different requirements
 */
public class Json {
    
    private static final Logger logger = Logger.getLogger(Json.class.getName());

    private final static ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    
    /**
     * This method returns an Optional of your class.
     * 
     * Optional&lt;List&lt;TestRecord&gt;&gt; result = json.getJsonBody(request, new TypeReference&lt;List&lt;TestRecord&gt;&gt;() {});
     * The TypeReference allows to use Generics freely in your deserializations.
     * 
     * If there&#39;s an error during serialization you will get an empty Optional.
     * 
     * @param <A> The class you want to deserialize
     * @param request The request that contains a link to the input stream that then will be deserialized to Json
     * @param typeRef The TypeReference that allows you to deserialize more complex generics.
     * @return s The class that was created from the Json.
     */
    public <A> Optional<A> getJsonBody(Request request, TypeReference<A> typeRef) {
        try (var inputStream = request.getInputStreamGetter().get()) {
            return Optional.of(objectMapper.readValue(inputStream, typeRef));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Opsi", ex);
            return Optional.empty();
        }
    }
    
    public org.r10r.ninjax.core.Result.OutputStreamRenderer json(Object objectToRenderAsJson) {
            org.r10r.ninjax.core.Result.OutputStreamRenderer outputStreamRenderer = outputStream -> {
                try {
                    Json.objectMapper.writeValue(outputStream, objectToRenderAsJson);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Rendering went wrong. Ouch! ", e);
                }
            };
            return outputStreamRenderer;
        }
}
