package org.r10r.ninjax.core;

import java.util.HashMap;
import java.util.List;
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
     * @param parameterNames The names of the route's path parameters (in order)
     * @param uri The request URI to extract parameters from
     * @return An immutable map of parameter names to their extracted values (URL-encoded)
     */
    public static Map<String, String> extractPathParameters(
            Pattern pathRegex,
            List<String> parameterNames,
            String uri
    ) {
        Matcher m = pathRegex.matcher(uri);
        return m.matches() ? extractPathParameters(m, parameterNames) : Map.of();
    }

    /**
     * Extracts path parameters from a matcher that already matched the request URI.
     * Use this when the route was just matched, so the regex does not run a second time.
     *
     * @param matchedMatcher A matcher of the route's regex on which matches() returned true
     * @param parameterNames The names of the route's path parameters (in order)
     * @return An immutable map of parameter names to their extracted values (URL-encoded)
     */
    public static Map<String, String> extractPathParameters(
            Matcher matchedMatcher,
            List<String> parameterNames
    ) {
        Map<String, String> map = new HashMap<>();

        // The route regex assigns each path variable a synthetic named group ("p0", "p1",
        // ...) in parameter order (see Router.convertRawUriToRegex). Reading values by these
        // names keeps extraction correct even when a user-supplied regex contains its own
        // capturing groups, which would otherwise shift positional group indices.
        int groupIndex = 0;
        for (String parameterName : parameterNames) {
            map.put(parameterName, matchedMatcher.group("p" + groupIndex));
            groupIndex++;
        }

        return Map.copyOf(map); // unmodifiable
    }
}
