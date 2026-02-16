package org.r10r.ninjax.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to extract path parameters from a URI using a route's regex pattern.
 *
 * This class separates the concern of path parameter extraction from the Request class,
 * allowing Request to be a pure data container without dependencies on routing logic.
 */
public class PathParameterExtractor {

    /**
     * Extracts path parameters from a URI by matching it against a route's regex pattern.
     *
     * @param pathRegex The compiled regex pattern from the route
     * @param parameters The map of parameter definitions from the route (ordered)
     * @param uri The request URI to extract parameters from
     * @return An immutable map of parameter names to their extracted values (URL-encoded)
     */
    public static Map<String, String> extractPathParameters(
            Pattern pathRegex,
            Map<String, RouteParameter> parameters,
            String uri
    ) {
        Map<String, String> map = new HashMap<>();

        Matcher m = pathRegex.matcher(uri);

        if (m.matches()) {
            Iterator<String> it = parameters.keySet().iterator();
            for (int i = 1; i < m.groupCount() + 1; i++) {
                String parameterName = it.next();
                map.put(parameterName, m.group(i));
            }
        }

        return Map.copyOf(map); // unmodifiable
    }
}
