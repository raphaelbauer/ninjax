package org.ninjax.core;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import static org.ninjax.core.NinjaSessionConverter.NINJA_SESSION_COOKIE_NAME;
import org.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaJetty {

    private static final Logger logger = LoggerFactory.getLogger(NinjaJetty.class);

    public final RouteFinder routeFinder;
    public final NinjaProperties ninjaProperties;

    private final Optional<Long> sessionExpiryTimeInSeconds;
    private final boolean sessionCookieSecure;

    public static final String NINJA_APPLICATION_SECRET_KEY = "application.secret";

    private final int jettyServerPort;

    private final SecretKey secretKeyForSessionEncryption;

    private static final String NINJA_LOGO
                = """
                      _______  .___ _______        ____.  _____  ____  ___
                      \\      \\ |   |\\      \\      |    | /  _  \\ \\   \\/  /
                      /   |   \\|   |/   |   \\     |    |/  /_\\  \\ \\     / 
                     /    |    \\   /    |    \\/\\__|    /    |    \\/     \\ 
                     \\____|__  /___\\____|__  /\\________\\____|__  /___/\\  \\
                             \\/            \\/                  \\/      \\_/
                """;

    public NinjaJetty(Router router, NinjaProperties ninjaProperties) throws RuntimeException {
        this.routeFinder = new RouteFinder(router);
        this.ninjaProperties = ninjaProperties;

        this.jettyServerPort = Integer.parseInt(ninjaProperties.get("ninja.port").orElse("8080"));

        String encodedSecret = ninjaProperties.get(NINJA_APPLICATION_SECRET_KEY).orElseThrow(() -> {
            return new RuntimeException(String.format("Missing key '%s' in 'conf/application.conf'. Can't start without a secret. Please create the secret using e.g. 'mvn ninja:generateSecret'.", NINJA_APPLICATION_SECRET_KEY));
        });

        byte[] decodedKey = Base64.getDecoder().decode(encodedSecret);
        this.secretKeyForSessionEncryption = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");

        this.sessionExpiryTimeInSeconds = ninjaProperties.get("application.session.expire_time_in_seconds").map(v -> Long.valueOf(v));
        this.sessionCookieSecure = ninjaProperties.get("application.session.cookie.secure")
                .map(v -> Boolean.parseBoolean(v))
                .orElse(true); // Default to true (secure) if not specified

        try {
            start();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public final void start() throws Exception {
        System.out.println(NINJA_LOGO);

        QueuedThreadPool threadPool = new QueuedThreadPool(200, 8);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(this.jettyServerPort);
        server.addConnector(connector);

        //GzipHandler gzipHandler = new GzipHandler();
        //gzipHandler.setHandler(request);
        //server.setHandler(gzipHandler);
        // Create a ServletContextHandler with request path
        ServletContextHandler request = new ServletContextHandler(ServletContextHandler.NO_SECURITY);
        request.setContextPath("/");

        // Map servlets to the request handler
        server.setHandler(request);

        // Add a simple servlet to the request
        //request.addServlet(new ServletHolder(new HelloServlet()), "/hello");
        request.addFilter(new FilterHolder(new NinjaServletFilter()), "/*", null);

        // Start the server
        server.start();
        server.join();
    }

    public class NinjaServletFilter implements jakarta.servlet.Filter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, jakarta.servlet.FilterChain fc) throws IOException, ServletException {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            try {

                var httpMethod = httpServletRequest.getMethod();
                var requestURI = httpServletRequest.getRequestURI();
                var routingResult = routeFinder.getRouteFor(httpMethod, requestURI);

                if (routingResult.isPresent()) {

                    var route = routingResult.get();

                    List<NinjaCookie> ninjaCookies = httpServletRequest.getCookies() == null
                            ? List.of()
                            : Arrays.stream(httpServletRequest.getCookies())
                                    .map(c -> NinjaJettyHelper.convertServletCookieToNinjaCookie(c))
                                    .toList();

                    var headers = NinjaJettyHelper.extractHeaders(httpServletRequest);

                    var ninjaSessionCookie = ninjaCookies.stream()
                            .filter(c -> c.name().equals(NINJA_SESSION_COOKIE_NAME))
                            .findFirst();

                    Optional<NinjaSession> ninjaSessionInRequest = ninjaSessionCookie
                            .map(c -> NinjaSessionConverter.extractSessionFromCookie(c, secretKeyForSessionEncryption))
                            .orElseGet(() -> Optional.empty());

                    Request.InputStreamGetter inputStreamGetter = () -> {
                        try {
                            return httpServletRequest.getInputStream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };

                    Request.FileItemGetter fileItemGetter = (String fieldName) -> {
                        try {
                            Part part = httpServletRequest.getPart(fieldName);
                            if (part != null) {
                                return Optional.of(new FileItem(
                                        part.getSubmittedFileName(),
                                        part.getContentType(),
                                        part.getSize(),
                                        part.getInputStream()
                                ));
                            }
                        } catch (Exception e) {
                            logger.error("Opsi", e);
                        }
                        return Optional.empty();
                    };

                    // Config multpart requests... (params, files etc)
                    if (httpServletRequest.getContentType() != null
                            && httpServletRequest.getContentType().startsWith("multipart/")) {
                        httpServletRequest.setAttribute(
                                "org.eclipse.jetty.multipartConfig",
                                new MultipartConfigElement(System.getProperty("java.io.tmpdir"))
                        );
                    }

                    Request.FileItemsGetter fileItemsGetter = (String fieldName) -> {
                        List<FileItem> result = new ArrayList<>();
                        try {
                            Collection<Part> parts = httpServletRequest.getParts();
                            for (Part part : parts) {
                                if (part.getName().equals(fieldName)) {
                                    result.add(new FileItem(
                                            part.getSubmittedFileName(),
                                            part.getContentType(),
                                            part.getSize(),
                                            part.getInputStream()
                                    ));
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Opsi", e);
                        }
                        return result;
                    };


                    var payload = new org.ninjax.core.Request.Payload(Map.of());

                    // Extract path parameters using the utility
                    var pathParams = PathParameterExtractor.extractPathParameters(
                            route.pathRegex(),
                            route.parameters,
                            requestURI
                    );

                    var request = Request.builder()
                            .requestPath(requestURI)
                            .pathParameters(pathParams)
                            .inputStreamGetter(inputStreamGetter)
                            .fileItemGetter(fileItemGetter)
                            .fileItemsGetter(fileItemsGetter)
                            .ninjaCookies(ninjaCookies)
                            .payload(payload)
                            .headers(headers)
                            .parameters(httpServletRequest.getParameterMap())
                            .ninjaSession(ninjaSessionInRequest)
                            .language(httpServletRequest.getLocale())
                            .build();

                    FilterChain chain = new FilterChain(route.filters, 0, routingResult.get().controllerMethod());
                    var result = chain.doFilter(request);

                    var status = result.status();
                    var contentType = result.contentType();

                    httpServletResponse.setContentType(contentType);
                    httpServletResponse.setStatus(status);
                    NinjaJettyHelper.setHeadersOnResponse(httpServletResponse, result.headers());

                    // That's actually not jetty specific logic...
                    // should live likely somewhere else...
                    switch (result.ninjaSessionState()) {
                        case Result.Exists exists -> {
                            NinjaSession ninjaSessionForResponse = exists.getSession();
                            var cookie = NinjaSessionConverter.createCookieWithInformationOfNinjaSession(
                                    ninjaSessionForResponse,
                                    secretKeyForSessionEncryption,
                                    sessionExpiryTimeInSeconds,
                                    sessionCookieSecure);
                            httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookie(cookie));
                        }
                        case Result.Remove remove -> {
                            var cookie = NinjaSessionConverter.createCookieToRemoveNinjaSession(sessionCookieSecure);
                            httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookie(cookie));
                        }
                        case Result.UnknownButDontTouch unknown -> {
                            // Intntionally don't do anything
                        }
                    }

                    for (var ninjaCookie : result.cookies()) {
                        httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookie(ninjaCookie));
                    }

                    if (result.outputStreamRenderer().isPresent()) {
                        result.outputStreamRenderer().get().streamTo(httpServletResponse.getOutputStream());
                    }

                } else {
                    var text = "Opsi. Not found";
                    var status = 404;
                    var contentType = "text/plain";

                    httpServletResponse.setContentType(contentType);
                    httpServletResponse.setStatus(status);
                    httpServletResponse.getWriter().println(text);
                }

            } catch (Throwable t) {
                logger.error("OMG! Something really bad happened. Time to investigate...", t);

                try {
                    // try to return result. it may not be possible...
                    var text = "Wow. Something really bad happened. Ask the owner of this server if error persists...";
                    var status = 500;
                    var contentType = "text/plain";

                    httpServletResponse.setContentType(contentType);
                    httpServletResponse.setStatus(status);
                    httpServletResponse.getWriter().println(text);
                } catch (Throwable e) {
                    logger.debug("I was not able to send a message via http to the user. That may be expected depending on the error", e);
                }
            }
        }
    }

    public static class NinjaJettyHelper {

        public static org.ninjax.core.NinjaCookie convertServletCookieToNinjaCookie(Cookie cookie) {

            return new org.ninjax.core.NinjaCookie(
                    cookie.getName(),
                    cookie.getValue(),
                    Optional.ofNullable(cookie.getComment()),
                    Optional.ofNullable(cookie.getDomain()),
                    cookie.getMaxAge(),
                    Optional.ofNullable(cookie.getPath()),
                    Secure.ofBoolean(cookie.getSecure()),
                    HttpOnly.ofBoolean(cookie.isHttpOnly()));
        }

        public static Cookie convertNinjaCookieToServletCookie(NinjaCookie ninjaCookie) {

            var cookie = new Cookie(ninjaCookie.name(), ninjaCookie.value());

            ninjaCookie.comment().ifPresent(c -> cookie.setComment(c));
            ninjaCookie.domain().ifPresent(d -> cookie.setDomain(d));
            cookie.setMaxAge(ninjaCookie.maxAge());
            ninjaCookie.path().ifPresent(p -> cookie.setPath(p));
            cookie.setSecure(ninjaCookie.secure().toBoolean());
            cookie.setHttpOnly(ninjaCookie.httpOnly().toBoolean());

            return cookie;
        }

        public static Map<String, List<String>> extractHeaders(HttpServletRequest httpServletRequest) {

            Map<String, List<String>> headersMap = new HashMap<>();

            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                List<String> headerValues = new ArrayList<>();
                Enumeration<String> values = httpServletRequest.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    headerValues.add(values.nextElement());
                }
                headersMap.put(headerName, headerValues);
            }

            return headersMap;

        }

        public static void setHeadersOnResponse(HttpServletResponse response, Map<String, List<String>> headers) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                List<String> values = entry.getValue();
                for (String value : values) {
                    response.addHeader(headerName, value); // Use addHeader for multi-value headers
                    // Use response.setHeader(headerName, value); if you only want the last value per header
                }
            }
        }
    }

}
