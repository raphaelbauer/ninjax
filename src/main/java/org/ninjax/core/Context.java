package org.ninjax.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {
    
        private final static Logger logger = LoggerFactory
            .getLogger(Context.class);

    private final Router.Route route;

    private final Map<String, String> pathParameters;
    
    private final String requestPath;
    
    private final InputStream inputStream;
    
    private final List<NinjaCookie> ninjaCookies;
    
    private final Map<String, List<String>> headers;
    
    private final Map<String, Object> payload;
    
    private final NinjaSession ninjaSession;

    public Context(
            Router.Route route,
            String requestPath,
            InputStream inputStream,
            List<NinjaCookie> ninjaCookies,
            Map<String, List<String>> headers,
            NinjaSession ninjaSession) {
        this.route = route;
        this.requestPath = requestPath;
        // Performance improvement: Only call when we need path params...
        this.pathParameters = getPathParametersEncoded(requestPath);
        this.inputStream = inputStream;
        
        this.ninjaCookies = ninjaCookies;
        
        this.payload = new HashMap<>();
        this.headers = headers;
        this.ninjaSession = ninjaSession;
    }
    
    public String getRequestPath() {
        return this.requestPath;
    }
    
    // Maybe a stupid API. We likely want to get a certain cookue by name...
    public List<NinjaCookie> getNinjaCookies() {
        return ninjaCookies;
    }
    
    public <A> Optional<A> getJsonBody() {
        try {
            return Optional.of(Json.objectMapper.readValue(inputStream, new TypeReference<A>() {}));
        } catch (IOException ex) {
            logger.error("Opsi", ex);
            return Optional.empty();
        }
    }
    
    public NinjaSession getNinjaSession() {
        return this.ninjaSession;
    }
    
    public Router.Route route() {
        return this.route;
    }

    public Optional<String> getPathParameterEncoded(String parameterName) {
        return Optional.ofNullable(pathParameters.get(parameterName));
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

}
