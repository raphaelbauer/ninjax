package org.r10r.ninjax.core;

import java.util.Optional;
import java.util.regex.Matcher;

public class RouteFinder {

    public final Router router;

    public RouteFinder(Router router) {
        this.router = router;
    }

    /**
     * Finds the first route matching method and path.
     *
     * A HEAD request without an explicit HEAD route falls back to the GET route (as required by
     * HTTP). The servers are responsible for not sending a body for HEAD requests.
     */
    public Optional<Router.Route> getRouteFor(String httpMethod, String path) {
        Optional<Router.Route> route = findRoute(httpMethod, path);

        if (route.isEmpty() && "HEAD".equalsIgnoreCase(httpMethod)) {
            return findRoute("GET", path);
        }

        return route;
    }

    private Optional<Router.Route> findRoute(String httpMethod, String path) {
        for (var route : router.getRoutes()) {
            if (route.httpMethod().equalsIgnoreCase(httpMethod)) {
                Matcher matcher = route.pathRegex().matcher(path);
                if (matcher.matches()) {
                    return Optional.of(route);
                }
            }
        }

        return Optional.empty();
    }

}
