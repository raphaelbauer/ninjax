package org.r10r.ninjax.core;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

public class RouteFinder {

    public final Router router;

    /**
     * The route that matched a request, together with the path parameters extracted by that same match.
     */
    public record RouteMatch(Router.Route route, Map<String, String> pathParameters) {
    }

    public RouteFinder(Router router) {
        this.router = router;
    }

    /**
     * Finds the first route matching method and path. The route's regex runs only once per request:
     * the matcher that selected the route also provides the path parameters.
     *
     * A HEAD request without an explicit HEAD route falls back to the GET route (as required by
     * HTTP). The servers are responsible for not sending a body for HEAD requests.
     */
    public Optional<RouteMatch> getRouteFor(String httpMethod, String path) {
        Optional<RouteMatch> routeMatch = findRoute(httpMethod, path);

        if (routeMatch.isEmpty() && "HEAD".equalsIgnoreCase(httpMethod)) {
            return findRoute("GET", path);
        }

        return routeMatch;
    }

    private Optional<RouteMatch> findRoute(String httpMethod, String path) {
        for (var route : router.getRoutes()) {
            if (route.httpMethod().equalsIgnoreCase(httpMethod)) {
                Matcher matcher = route.pathRegex().matcher(path);
                if (matcher.matches()) {
                    var pathParameters = PathParameterExtractor.extractPathParameters(matcher, route.parameterNames);
                    return Optional.of(new RouteMatch(route, pathParameters));
                }
            }
        }

        return Optional.empty();
    }

}
