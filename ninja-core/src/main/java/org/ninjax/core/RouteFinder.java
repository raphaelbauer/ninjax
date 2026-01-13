package org.ninjax.core;

import java.util.Optional;
import java.util.regex.Matcher;

public class RouteFinder {

    public final Router router;

    public RouteFinder(Router router) {
        this.router = router;
    }

    public Optional<Router.Route> getRouteFor(String httpMethod, String path) {
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
