package org.ninjax.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Optional;
import org.ninjax.core.Request;
import org.ninjax.core.Result;
import static org.ninjax.core.Result.APPLICATION_JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simple and basic Json rendering.
 *
 * You may want to create your own class if you have different requirements
 */
public class Json {
    
    private static final Logger logger = LoggerFactory.getLogger(Json.class);

    private final static ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());

    
    /**
     * This method returns an Optional of your class.
     * 
     * Optional<List<TestRecord>> result = json.getJsonBody(request, new TypeReference<List<TestRecord>>() {});
     * The TypeReference allows to use Generics freely in your deserializations.
     * 
     * If there's an error during serialization you will get an empty Optional.
     * 
     * @param <A> The class you want to deserialize
     * @param request The request that contains a link to the input stream that then will be deserialized to Json
     * @param typeRef The TypeReference that allows you to deserialize more complex generics.
     * @return s The class that was created from the Json.
     */
    public <A> Optional<A> getJsonBody(Request request, TypeReference<A> typeRef) {
        try (var inputStream = request.inputStreamGetter().get()) {
            return Optional.of(objectMapper.readValue(inputStream, typeRef));
        } catch (IOException ex) {
            logger.error("Opsi", ex);
            return Optional.empty();
        }
    }
    
    public org.ninjax.core.Result.OutputStreamRenderer json(Object objectToRenderAsJson) {
            org.ninjax.core.Result.OutputStreamRenderer outputStreamRenderer = outputStream -> {
                try {
                    Json.objectMapper.writeValue(outputStream, objectToRenderAsJson);
                } catch (IOException e) {
                    logger.error("Rendering went wrong. Ouch! ", e);
                }
            };
            return outputStreamRenderer;
        }
}
