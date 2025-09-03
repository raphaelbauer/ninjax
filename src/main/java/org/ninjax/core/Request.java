package org.ninjax.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Request {

    private final static Logger logger = LoggerFactory
            .getLogger(Request.class);

    private final Router.Route route;

    private final Map<String, String> pathParameters;

    private final String requestPath;

    private final InputStreamGetter inputStreamGetter;
    
    private final FileItemGetter fileItemGetter;
    
    private final FileItemsGetter fileItemsGetter;
    

    private final List<NinjaCookie> ninjaCookies;

    private final Map<String, List<String>> headers;

    private final Map<String, Object> payload;

    private final Optional<NinjaSession> ninjaSession;

    private final Map<String, String[]> parameters;
    
    private final Locale language;

    public interface InputStreamGetter {

        InputStream get();
    }
    
    public interface FileItemGetter {

        Optional<FileItem> getFileItem(String fieldName);
    }
    
    public interface FileItemsGetter {

        List<FileItem> getFileItems(String fieldName);
    }

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
            Locale language) {
        this.route = route;
        this.requestPath = requestPath;
        // Performance improvement: Only call when we need path params...
        this.pathParameters = getPathParametersEncoded(requestPath);
        this.inputStreamGetter = inputStreamGetter;
        this.fileItemGetter = fileItemGetter;
        this.fileItemsGetter = fileItemsGetter;

        this.ninjaCookies = ImmutableList.copyOf(ninjaCookies);

        this.payload = ImmutableMap.copyOf(payload);
        this.headers = ImmutableMap.copyOf(headers);
        this.parameters = ImmutableMap.copyOf(parameters);
        this.ninjaSession = ninjaSession;
        
        this.language = language;
    }

    public String getRequestPath() {
        return this.requestPath;
    }

    // Maybe a stupid API. We likely want to get a certain cookue by name...
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
        return this.ninjaSession;
    }

    public Router.Route route() {
        return this.route;
    }
    
    /**
     * Content of this paRaw path parameter. 
     * 
     * All urlencoded Strings will be decoded. For instance "my%20name" will become "my name".
     */
    public Optional<String> getPathParameter(String pathParameterName) {
        return Optional.ofNullable(pathParameters.get(pathParameterName))
                .map(p -> URLDecoder.decode(p, StandardCharsets.UTF_8));
    }

    public Optional<String> getParameter(String parameterName) {
        var value = parameters.get(parameterName);

        if (value == null) {
            return Optional.empty();
        }

        if (value.length == 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(value[0]);
    }

    /**
     * This method does not do any decoding / encoding.
     *
     * If you want to decode you have to do it yourself.
     *
     * Most likely with:
     * http://docs.oracle.com/javase/6/docs/api/java/net/URI.html
     *
     * @param path The whole encoded uri.
     * @return A map with all parameters of that uri. Encoded in => encoded out.
     */
    private Map<String, String> getPathParametersEncoded(String uri) {
        Map<String, String> map = Maps.newHashMap();

        Matcher m = route.pathRegex().matcher(uri);

        if (m.matches()) {
            Iterator<String> it = this.route.parameters.keySet().iterator();
            for (int i = 1; i < m.groupCount() + 1; i++) {
                String parameterName = it.next();
                map.put(parameterName, m.group(i));
            }
        }

        return map;
    }

    public void putPayload(String key, Object o) {
        payload.put(key, o);
    }

    // not sure if this is ok with optinal...
    public <U> Optional<U> getPayload(String key, Class<U> clazz) {
        Object object = payload.get(key);
        if (clazz.isInstance(object)) {
            return Optional.of(clazz.cast(object));
        } else {
            return Optional.empty(); // or throw an exception
        }
    }
    
    public Optional<String> getPayload(String key) {
        Object object = payload.get(key);
        if (String.class.isInstance(object)) {
            return Optional.of(String.class.cast(object));
        } else {
            return Optional.empty(); // or throw an exception
        }
    }

    public List<FileItem> getFiles(String fieldName) {
        return this.fileItemsGetter.getFileItems(fieldName); 
        
    }

    public Optional<FileItem> getFile(String fieldName) {
        return this.fileItemGetter.getFileItem(fieldName);
    }
    
    public Locale getLocale() {
        return this.language;
    }

}
