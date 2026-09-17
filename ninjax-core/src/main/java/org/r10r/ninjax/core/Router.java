package org.r10r.ninjax.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {

    private final List<Route> routes = new ArrayList();

    public RouteTmp GET(String path) {
        return new RouteTmp("GET", path);
    }

    public RouteTmp POST(String path) {
        return new RouteTmp("POST", path);
    }

    public RouteTmp PUT(String path) {
        return new RouteTmp("PUT", path);
    }

    public RouteTmp PATCH(String path) {
        return new RouteTmp("PATCH", path);
    }

    public RouteTmp DELETE(String path) {
        return new RouteTmp("DELETE", path);
    }

    /**
     * Only needed if HEAD should behave differently from GET. Without an explicit HEAD route,
     * a HEAD request is answered by the matching GET route (the servers then send no body).
     */
    public RouteTmp HEAD(String path) {
        return new RouteTmp("HEAD", path);
    }

    public class RouteTmp {

        private final String httpMethod;
        private final String path;

        private final List<NinjaFilter> filters = new ArrayList();

        public RouteTmp(String httpMethod, String path) {
            this.httpMethod = httpMethod;
            this.path = path;
        }

        public RouteTmp filter(NinjaFilter ninjaFilter) {
            filters.add(ninjaFilter);
            return this;
        }

        public void with(ControllerMethod controllerMethod) {
            routes.add(new Route(httpMethod, path, controllerMethod, List.copyOf(filters)));
        }

    }

    //public record Route(String httpMethod, String path, ControllerMethod controllerMethod) {}
    protected List<Route> getRoutes() {
        return routes;
    }

    public static interface ControllerMethod {

        Result executeMethod(Request request);
    }

    public class Route {

        //Matches: {id} AND {id: .*?}
        // group(1) extracts the name of the group (in that case "id").
        // group(3) extracts the regex if defined
        public final static Pattern PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE
                = Pattern.compile("\\{(.*?)(:\\s(.*?))?\\}");

        /**
         * This regex matches everything in between path slashes.
         */
        private final static String VARIABLE_ROUTES_DEFAULT_REGEX = "[^/]*";

        private final String httpMethod;
        private final String path;
        private final ControllerMethod controllerMethod;

        private final Pattern pathRegex;

        /**
         * Names of the path parameters in the order they appear, e.g. ["id", "slug"] for
         * "/users/{id: [0-9]+}/posts/{slug}".
         */
        public final List<String> parameterNames;

        public final List<NinjaFilter> filters;

        public Route(String httpMethod, String path, ControllerMethod controllerMethod, List<NinjaFilter> filters) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.controllerMethod = controllerMethod;
            this.pathRegex = Pattern.compile(convertRawUriToRegex(path));
            this.parameterNames = parseParameterNames(path);
            this.filters = filters;
        }

        public Pattern pathRegex() {
            return pathRegex;
        }

        public ControllerMethod controllerMethod() {
            return controllerMethod;
        }

        /**
         * @return The path as given in the router. For instance: /home/{id}
         */
        public String path() {
            return path;
        }

        public String httpMethod() {
            return httpMethod;
        }

        private static List<String> parseParameterNames(String path) {
            List<String> names = new ArrayList<>();
            Matcher matcher = PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE.matcher(path);
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
            return List.copyOf(names);
        }

        private static String convertRawUriToRegex(String rawUri) {

            Matcher matcher = PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE.matcher(rawUri);

            StringBuffer stringBuffer = new StringBuffer();

            // Each path variable becomes a synthetically named capturing group ("p0", "p1", ...).
            // Looking values up by these synthetic names (rather than by group position) makes
            // extraction independent of any capturing groups inside a user-supplied regex: those
            // inner groups simply become harmless anonymous captures we never read. Synthetic
            // names also avoid relying on the parameter name being a valid Java group identifier.
            int groupIndex = 0;

            while (matcher.find()) {

                // By convention group 3 is the regex if provided by the user.
                // If it is not provided by the user the group 3 is null.
                String namedVariablePartOfRoute = matcher.group(3);
                String innerRegex = (namedVariablePartOfRoute != null)
                        ? namedVariablePartOfRoute
                        : VARIABLE_ROUTES_DEFAULT_REGEX;

                String namedVariablePartOfORouteReplacedWithRegex
                        = "(?<p" + groupIndex + ">" + innerRegex + ")";
                groupIndex++;

                // quoteReplacement so any $ or \ in the user regex are treated literally
                // by appendReplacement rather than as replacement back-references.
                matcher.appendReplacement(stringBuffer,
                        Matcher.quoteReplacement(namedVariablePartOfORouteReplacedWithRegex));

            }

            // .. and we append the tail to complete the stringBuffer
            matcher.appendTail(stringBuffer);

            return "^" + stringBuffer + "$";
        }

    }

}
