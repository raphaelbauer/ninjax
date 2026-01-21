package org.ninjax.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Request(
        String requestPath,
        InputStreamGetter inputStreamGetter,
        FileItemGetter fileItemGetter,
        FileItemsGetter fileItemsGetter,
        List<NinjaCookie> ninjaCookies,
        Payload payload,
        Map<String, List<String>> headers,
        Map<String, String[]> parameters,
        Optional<NinjaSession> ninjaSession,
        Locale language,
        Map<String, String> pathParameters
        ) {

    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    public interface InputStreamGetter {

        InputStream get();
    }

    public interface FileItemGetter {

        Optional<FileItem> getFileItem(String fieldName);
    }

    public interface FileItemsGetter {

        List<FileItem> getFileItems(String fieldName);
    }

    /**
     * Canonical constructor to enforce defensive copies.
     */
    public Request(
            String requestPath,
            InputStreamGetter inputStreamGetter,
            FileItemGetter fileItemGetter,
            FileItemsGetter fileItemsGetter,
            List<NinjaCookie> ninjaCookies,
            Payload payload,
            Map<String, List<String>> headers,
            Map<String, String[]> parameters,
            Optional<NinjaSession> ninjaSession,
            Locale language,
            Map<String, String> pathParameters
    ) {
        // Apply defensive copies directly
        this.requestPath = requestPath;
        this.inputStreamGetter = inputStreamGetter;
        this.fileItemGetter = fileItemGetter;
        this.fileItemsGetter = fileItemsGetter;
        this.ninjaCookies = ImmutableList.copyOf(ninjaCookies);
        this.payload = payload;
        this.headers = ImmutableMap.copyOf(headers);
        this.parameters = ImmutableMap.copyOf(parameters);
        this.ninjaSession = ninjaSession;
        this.language = language;
        this.pathParameters = ImmutableMap.copyOf(pathParameters);
    }

    // ----- Original methods, adapted to the record style -----
    public List<NinjaCookie> getNinjaCookies() {
        return ninjaCookies;
    }

    public <A> Optional<A> getJsonBody() {
        try (var inputStream = inputStreamGetter.get()) {
            return Optional.of(Json.objectMapper.readValue(inputStream, new TypeReference<A>() {
            }));
        } catch (IOException ex) {
            logger.error("Opsi", ex);
            return Optional.empty();
        }
    }

    public Optional<NinjaSession> getNinjaSession() {
        return ninjaSession;
    }

    /**
     * Content of this raw path parameter.
     *
     * All urlencoded Strings will be decoded. For instance "my%20name" will
     * become "my name".
     */
    public Optional<String> getPathParameter(String pathParameterName) {
        return Optional.ofNullable(pathParameters.get(pathParameterName))
                .map(p -> URLDecoder.decode(p, StandardCharsets.UTF_8));
    }

    public ImmutableList<String> getParameter(String parameterName) {
        var value = parameters.get(parameterName);

        if (value == null || value.length == 0) {
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(value);
    }

    public List<FileItem> getFiles(String fieldName) {
        return fileItemsGetter.getFileItems(fieldName);
    }

    public Optional<FileItem> getFile(String fieldName) {
        return fileItemGetter.getFileItem(fieldName);
    }

    public Locale getLocale() {
        return language;
    }

    // Convenience alias to match the old accessor
    public String getRequestPath() {
        return requestPath;
    }

    // ----- Builder -----
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .requestPath(this.requestPath)
                .inputStreamGetter(this.inputStreamGetter)
                .fileItemGetter(this.fileItemGetter)
                .fileItemsGetter(this.fileItemsGetter)
                .ninjaCookies(this.ninjaCookies)
                .payload(this.payload)
                .headers(this.headers)
                .parameters(this.parameters)
                .ninjaSession(this.ninjaSession)
                .language(this.language)
                .pathParameters(this.pathParameters);
    }

    public static final class Builder {

        private String requestPath;
        private InputStreamGetter inputStreamGetter;
        private FileItemGetter fileItemGetter;
        private FileItemsGetter fileItemsGetter;
        private List<NinjaCookie> ninjaCookies = List.of();
        private Payload payload;
        private Map<String, List<String>> headers = Map.of();
        private Map<String, String[]> parameters = Map.of();
        private Optional<NinjaSession> ninjaSession = Optional.empty();
        private Locale language = Locale.getDefault();
        private Map<String, String> pathParameters = Map.of();

        private Builder() {
        }

        public Builder requestPath(String requestPath) {
            this.requestPath = requestPath;
            return this;
        }

        public Builder inputStreamGetter(InputStreamGetter inputStreamGetter) {
            this.inputStreamGetter = inputStreamGetter;
            return this;
        }

        public Builder fileItemGetter(FileItemGetter fileItemGetter) {
            this.fileItemGetter = fileItemGetter;
            return this;
        }

        public Builder fileItemsGetter(FileItemsGetter fileItemsGetter) {
            this.fileItemsGetter = fileItemsGetter;
            return this;
        }

        public Builder ninjaCookies(List<NinjaCookie> ninjaCookies) {
            this.ninjaCookies = ninjaCookies;
            return this;
        }

        public Builder payload(Payload payload) {
            this.payload = payload;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        public Builder parameters(Map<String, String[]> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder ninjaSession(Optional<NinjaSession> ninjaSession) {
            this.ninjaSession = ninjaSession;
            return this;
        }

        public Builder language(Locale language) {
            this.language = language;
            return this;
        }

        public Builder pathParameters(Map<String, String> pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        public Request build() {
            Objects.requireNonNull(requestPath, "requestPath must not be null");
            Objects.requireNonNull(inputStreamGetter, "inputStreamGetter must not be null");
            Objects.requireNonNull(fileItemGetter, "fileItemGetter must not be null");
            Objects.requireNonNull(fileItemsGetter, "fileItemsGetter must not be null");
            Objects.requireNonNull(ninjaCookies, "ninjaCookies must not be null");
            Objects.requireNonNull(headers, "headers must not be null");
            Objects.requireNonNull(parameters, "parameters must not be null");
            Objects.requireNonNull(ninjaSession, "ninjaSession must not be null");
            Objects.requireNonNull(language, "language must not be null");
            Objects.requireNonNull(pathParameters, "pathParameters must not be null");

            return new Request(
                    requestPath,
                    inputStreamGetter,
                    fileItemGetter,
                    fileItemsGetter,
                    ninjaCookies,
                    payload,
                    headers,
                    parameters,
                    ninjaSession,
                    language,
                    pathParameters
            );
        }
    }

    public final static class Payload {

        private final Map<String, Object> delegate;

        public Payload(Map<String, Object> delegate) {
            this.delegate = Map.copyOf(delegate);
        }

        public <U> Optional<U> get(String key, Class<U> clazz) {
            Object object = delegate.get(key);
            return clazz.isInstance(object)
                    ? Optional.of(clazz.cast(object))
                    : Optional.empty();
        }

        public Optional<String> getString(String key) {
            Object object = delegate.get(key);
            return object instanceof String s ? Optional.of(s) : Optional.empty();
        }

    }
}
