package org.r10r.ninjax.core;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class Request {

    private static final Logger logger = Logger.getLogger(Request.class.getName());

    private final String requestPath;
    private final InputStreamGetter inputStreamGetter;
    private final FileItemsGetter fileItemsGetter;
    private final List<NinjaCookie> ninjaCookies;
    private final Headers headers;
    private final Parameters parameters;
    private final Optional<NinjaSession> ninjaSession;
    private final Locale locale;
    private final Map<String, String> pathParameters;

    public interface InputStreamGetter {
        InputStream get();
    }

    public interface FileItemsGetter {
        List<FileItem> getFileItems(String fieldName);
    }

    /**
     * Constructor enforcing non-null values and defensive copies.
     */
    public Request(
            String requestPath,
            InputStreamGetter inputStreamGetter,
            FileItemsGetter fileItemsGetter,
            List<NinjaCookie> ninjaCookies,
            Headers headers,
            Parameters parameters,
            Optional<NinjaSession> ninjaSession,
            Locale locale,
            Map<String, String> pathParameters
    ) {
        this.requestPath = Objects.requireNonNull(requestPath, "requestPath must not be null");
        this.inputStreamGetter = Objects.requireNonNull(inputStreamGetter, "inputStreamGetter must not be null");
        this.fileItemsGetter = Objects.requireNonNull(fileItemsGetter, "fileItemsGetter must not be null");
        this.ninjaCookies = List.copyOf(Objects.requireNonNull(ninjaCookies, "ninjaCookies must not be null"));
        this.headers = Objects.requireNonNull(headers, "headers must not be null");
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
        this.ninjaSession = Objects.requireNonNull(ninjaSession, "ninjaSession must not be null");
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
        this.pathParameters = Map.copyOf(Objects.requireNonNull(pathParameters, "pathParameters must not be null"));
    }

    // ---- getters (get... style) ----

    public String getRequestPath() {
        return requestPath;
    }

    public InputStreamGetter getInputStreamGetter() {
        return inputStreamGetter;
    }

    public FileItemsGetter getFileItemsGetter() {
        return fileItemsGetter;
    }

    public List<NinjaCookie> getNinjaCookies() {
        return ninjaCookies;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public Optional<NinjaSession> getNinjaSession() {
        return ninjaSession;
    }

    public Locale getLocale() {
        return locale;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
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

    public List<FileItem> getFiles(String fieldName) {
        return fileItemsGetter.getFileItems(fieldName);
    }

    /**
     * The first file uploaded for this field name (if any).
     */
    public Optional<FileItem> getFile(String fieldName) {
        return getFiles(fieldName).stream().findFirst();
    }

    // ---- Builder ----

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .requestPath(this.requestPath)
                .inputStreamGetter(this.inputStreamGetter)
                .fileItemsGetter(this.fileItemsGetter)
                .ninjaCookies(this.ninjaCookies)
                .headers(this.headers)
                .parameters(this.parameters)
                .ninjaSession(this.ninjaSession)
                .locale(this.locale)
                .pathParameters(this.pathParameters);
    }

    public static final class Builder {

        private String requestPath;
        private InputStreamGetter inputStreamGetter;
        private FileItemsGetter fileItemsGetter;
        private List<NinjaCookie> ninjaCookies = List.of();
        private Headers headers;
        private Parameters parameters;
        private Optional<NinjaSession> ninjaSession = Optional.empty();
        private Locale locale = Locale.getDefault();
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

        public Builder fileItemsGetter(FileItemsGetter fileItemsGetter) {
            this.fileItemsGetter = fileItemsGetter;
            return this;
        }

        public Builder ninjaCookies(List<NinjaCookie> ninjaCookies) {
            this.ninjaCookies = ninjaCookies;
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Builder parameters(Parameters parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder ninjaSession(Optional<NinjaSession> ninjaSession) {
            this.ninjaSession = ninjaSession;
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public Builder pathParameters(Map<String, String> pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        public Request build() {
            return new Request(
                    requestPath,
                    inputStreamGetter,
                    fileItemsGetter,
                    ninjaCookies,
                    headers,
                    parameters,
                    ninjaSession,
                    locale,
                    pathParameters
            );
        }
    }

    // ---- Nested value types ----

    public static final class Parameters {

        private final Map<String, String[]> parameters;

        public Parameters() {
            this.parameters = Map.of();
        }

        public Parameters(Map<String, String[]> parameters) {
            this.parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
        }

        public Optional<String> get(String parameterName) {
            var list = parameters.get(parameterName);
            return (list == null || list.length == 0) ? Optional.empty() : Optional.of(list[0]);
        }

        public List<String> getAll(String parameterName) {
            var arr = parameters.get(parameterName);
            return (arr == null || arr.length == 0) ? List.of() : List.of(arr);
        }

        public Map<String, String[]> getParameters() {
            return parameters;
        }
    }

    public static final class Headers {

        private final Map<String, List<String>> headers;

        public Headers() {
            this.headers = Map.of();
        }

        public Headers(Map<String, List<String>> headers) {
            this.headers = Map.copyOf(Objects.requireNonNull(headers, "headers must not be null"));
        }

        public Optional<String> get(String headerName) {
            var list = headers.get(headerName);
            return (list == null || list.isEmpty()) ? Optional.empty() : Optional.of(list.get(0));
        }

        public List<String> getAll(String headerName) {
            return headers.getOrDefault(headerName, List.of());
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }
}
