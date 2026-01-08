package org.ninjax.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Request(
        Router.Route route,
        String requestPath,
        InputStreamGetter inputStreamGetter,
        FileItemGetter fileItemGetter,
        FileItemsGetter fileItemsGetter,
        List<NinjaCookie> ninjaCookies,
        Map<String, Object> payload,
        Map<String, List<String>> headers,
        Map<String, String[]> parameters,
        Optional<NinjaSession> ninjaSession,
        Locale language,
        Map<String, String> pathParameters // derived from route + requestPath
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
     * Canonical constructor wrapper to enforce defensive copies and derive
     * pathParameters.
     */
    public Request(
            Router.Route route,
            String requestPath,
            InputStreamGetter inputStreamGetter,
            FileItemGetter fileItemGetter,
            FileItemsGetter fileItemsGetter,
            List<NinjaCookie> ninjaCookies,
            Map<String, Object> payload,
            Map<String, List<String>> headers,
            Map<String, String[]> parameters,
            Optional<NinjaSession> ninjaSession,
            Locale language
    ) {
        this(
                route,
                requestPath,
                inputStreamGetter,
                fileItemGetter,
                fileItemsGetter,
                // defensive copies
                ImmutableList.copyOf(ninjaCookies),
                ImmutableMap.copyOf(payload),
                ImmutableMap.copyOf(headers),
                ImmutableMap.copyOf(parameters),
                ninjaSession,
                language,
                getPathParametersEncodedStatic(route, requestPath)
        );
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

    public Router.Route route() {
        return route;
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

    // Static helper because records can’t have “custom” instance-init easily
    private static Map<String, String> getPathParametersEncodedStatic(Router.Route route, String uri) {
        Map<String, String> map = new HashMap<>();

        Matcher m = route.pathRegex().matcher(uri);

        if (m.matches()) {
            Iterator<String> it = route.parameters.keySet().iterator();
            for (int i = 1; i < m.groupCount() + 1; i++) {
                String parameterName = it.next();
                map.put(parameterName, m.group(i));
            }
        }

        return Map.copyOf(map); // unmodifiable
    }

    public <U> Optional<U> getPayload(String key, Class<U> clazz) {
        Object object = payload.get(key);
        if (clazz.isInstance(object)) {
            return Optional.of(clazz.cast(object));
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> getPayload(String key) {
        Object object = payload.get(key);
        if (object instanceof String s) {
            return Optional.of(s);
        } else {
            return Optional.empty();
        }
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

    public static final class Builder {

        private Router.Route route;
        private String requestPath;
        private InputStreamGetter inputStreamGetter;
        private FileItemGetter fileItemGetter;
        private FileItemsGetter fileItemsGetter;
        private List<NinjaCookie> ninjaCookies = List.of();
        private Map<String, Object> payload = Map.of();
        private Map<String, List<String>> headers = Map.of();
        private Map<String, String[]> parameters = Map.of();
        private Optional<NinjaSession> ninjaSession = Optional.empty();
        private Locale language = Locale.getDefault();

        private Builder() {
        }

        public Builder route(Router.Route route) {
            this.route = route;
            return this;
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

        public Builder payload(Map<String, Object> payload) {
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

        public Request build() {
            Objects.requireNonNull(route, "route must not be null");
            Objects.requireNonNull(requestPath, "requestPath must not be null");
            Objects.requireNonNull(inputStreamGetter, "inputStreamGetter must not be null");
            Objects.requireNonNull(fileItemGetter, "fileItemGetter must not be null");
            Objects.requireNonNull(fileItemsGetter, "fileItemsGetter must not be null");
            Objects.requireNonNull(ninjaCookies, "ninjaCookies must not be null");
            Objects.requireNonNull(payload, "payload must not be null");
            Objects.requireNonNull(headers, "headers must not be null");
            Objects.requireNonNull(parameters, "parameters must not be null");
            Objects.requireNonNull(ninjaSession, "ninjaSession must not be null");
            Objects.requireNonNull(language, "language must not be null");

            return new Request(
                    route,
                    requestPath,
                    inputStreamGetter,
                    fileItemGetter,
                    fileItemsGetter,
                    ninjaCookies,
                    payload,
                    headers,
                    parameters,
                    ninjaSession,
                    language
            );
        }
    }
}
