package org.ninjax.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

public class Context {

    private final Router.Route route;

    private final Map<String, String> pathParameters;

    public Context(Router.Route route) {
        this.route = route;
        this.pathParameters = getPathParametersEncoded(route.path());
    }
    
    public Router.Route route() {
        return this.route;
    }

    public Optional<String> getPathParam(String parameterName) {
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
    private Map<String, String> getPathParametersEncoded(String path) {
        Map<String, String> pathParamNameAndValue = new HashMap<>();

        Matcher matcher = this.route.pathRegex().matcher(path);

        if (matcher.matches()) {
            var namedGroupsWithIndex = matcher.namedGroups();
            for (var k : namedGroupsWithIndex.entrySet()) {
                pathParamNameAndValue.put(k.getKey(), matcher.group(k.getValue()));
            }
        }

        return pathParamNameAndValue;
    }

}
