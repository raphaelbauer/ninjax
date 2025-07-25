package org.ninjax.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {
    private final List<Route> routes = new ArrayList();
    
    public RouteTmp GET(String path) {
        return new RouteTmp("GET", path);
    }

    public class RouteTmp {
        private final String httpMethod;
        private final String path;

        public RouteTmp(String httpMethod, String path) {
            this.httpMethod = httpMethod;
            this.path = path;
        }

        public void with(ControllerMethod controllerMethod) {
            routes.add(new Route(httpMethod, path, controllerMethod));
        }
        
    }
    
    //public record Route(String httpMethod, String path, ControllerMethod controllerMethod) {}
    
    protected List<Route> getRoutes() {
        return routes;
    }
    
    public static interface ControllerMethod {
        Result executeMethod(Context context);
    }
    
    
    public class Route {
        //Matches: {id} AND {id: .*?}
        // group(1) extracts the name of the group (in that case "id").
        // group(3) extracts the regex if defined

        private final static Pattern PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE
                = Pattern.compile("\\{(.*?)(:\\s(.*?))?\\}");

        /**
         * This regex matches everything in between path slashes.
         */
        private final static String VARIABLE_ROUTES_DEFAULT_REGEX = "([^/]*)";

        
        private final String httpMethod;
        private final String path;
        private final ControllerMethod controllerMethod;
        
        private final Pattern pathRegex;

        public Route(String httpMethod, String path, ControllerMethod controllerMethod) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.controllerMethod = controllerMethod;
            this.pathRegex = Pattern.compile(convertRawUriToRegex(path));
        }

        public Pattern pathRegex() {
            return pathRegex;
        }
        
        public ControllerMethod controllerMethod() {
            return controllerMethod;
        }
        
        public String path() {
            return path;
        }
        
        public String httpMethod() {
            return httpMethod;
        }


        private static String convertRawUriToRegex(String rawUri) {

            // convert capturing groups in route regex to non-capturing groups
            // this is to avoid count mismatch of path params and groups in uri regex
            Matcher groupMatcher = Pattern.compile("\\(([^?].*)\\)").matcher(rawUri);
            String converted = groupMatcher.replaceAll("\\(?:$1\\)");

            Matcher matcher = PATTERN_FOR_VARIABLE_PARTS_OF_ROUTE.matcher(converted);

            StringBuffer stringBuffer = new StringBuffer();

            while (matcher.find()) {

                // By convention group 3 is the regex if provided by the user.
                // If it is not provided by the user the group 3 is null.
                String namedVariablePartOfRoute = matcher.group(3);
                String namedVariablePartOfORouteReplacedWithRegex;

                if (namedVariablePartOfRoute != null) {
                    // we convert that into a regex matcher group itself
                    namedVariablePartOfORouteReplacedWithRegex
                            = "(" + Matcher.quoteReplacement(namedVariablePartOfRoute) + ")";
                } else {
                    // we convert that into the default namedVariablePartOfRoute regex group
                    namedVariablePartOfORouteReplacedWithRegex
                            = VARIABLE_ROUTES_DEFAULT_REGEX;
                }
                // we replace the current namedVariablePartOfRoute group
                matcher.appendReplacement(stringBuffer, namedVariablePartOfORouteReplacedWithRegex);

            }

            // .. and we append the tail to complete the stringBuffer
            matcher.appendTail(stringBuffer);

            return stringBuffer.toString();
        }

    }

}
