package org.ninjax.core;

import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaJetty {

    private static final Logger logger = LoggerFactory.getLogger(NinjaJetty.class);

    public final RouteFinder routeFinder;
    public final NinjaProperties ninjaProperties;

    private final Optional<Long> sessionExpiryTimeInSeconds;
    //private final Optional<Long> sessionisSecure;
    //private final Optional<Long> sessionisHttpOnly;

    public static final String NINJA_SESSION_COOKIE_NAME = "NINJA_SESSION";
    
    private static final String NINJA_SESSION_PATH = "/";

    private final SecretKey secretKeyForSessionEncryption;

    public NinjaJetty(Router router, NinjaProperties ninjaProperties) throws RuntimeException {
        this.routeFinder = new RouteFinder(router);
        this.ninjaProperties = ninjaProperties;

        String encodedSecret = ninjaProperties.get("application.secret").orElseThrow();
        byte[] decodedKey = Base64.getDecoder().decode(encodedSecret);
        secretKeyForSessionEncryption = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");

        this.sessionExpiryTimeInSeconds = ninjaProperties.get("application.session.expire_time_in_seconds").map(v -> Long.valueOf(v));

        
        
        try {
            start();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public final void start() throws Exception {
        System.out.println(
                """
                     _______  .___ _______        ____.  _____   
                     \\      \\ |   |\\      \\      |    | /  _  \\  
                     /   |   \\|   |/   |   \\     |    |/  /_\\  \\ 
                    /    |    \\   /    |    \\/\\__|    /    |    \\
                    \\____|__  /___\\____|__  /\\________\\____|__  /
                            \\/            \\/                  \\/ 
                """);

        // Create a basic Jetty server object that will listen on port 8080
        QueuedThreadPool threadPool = new QueuedThreadPool(200, 8);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
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

                    Optional<NinjaSession> ninjaSessionInRequest = NinjaJettyHelper.getSession(
                            ninjaCookies, 
                            secretKeyForSessionEncryption);

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
   

                    var request = new Request(
                            route,
                            requestURI,
                            inputStreamGetter,
                            fileItemGetter,
                            fileItemsGetter,
                            ninjaCookies,
                            ImmutableMap.<String, Object>of(),
                            headers,
                            httpServletRequest.getParameterMap(),
                            ninjaSessionInRequest,
                            httpServletRequest.getLocale() /* TODO local can also be set by a lang cookie to override headers of accept... */
                    );  

                    FilterChain chain = new FilterChain(route.filters, 0, routingResult.get().controllerMethod());
                    var result = chain.doFilter(request);

                    var status = result.status;
                    var contentType = result.contentType;

                    httpServletResponse.setContentType(contentType);
                    httpServletResponse.setStatus(status);
                    NinjaJettyHelper.setHeadersOnResponse(httpServletResponse, result.headers);

                    // That's actually not jetty specific logic...
                    // should live likely somewhere else...
                    switch (result.ninjaSessionState) {
                        case Result.Exists exists -> {
                            NinjaSession ninjaSessionForResponse = exists.getSession();
                            var cookie = NinjaJettyHelper.saveSession(
                                    ninjaSessionForResponse,
                                    secretKeyForSessionEncryption,
                                    sessionExpiryTimeInSeconds);
                            httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookue(cookie));
                        }
                        case Result.Remove remove -> {
                            var cookie = NinjaJettyHelper.removeNinjaSession();
                            httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookue(cookie));
                        }
                        case Result.UnknownButDontTouch unknown -> {
                            // Intntionally don't do anything
                        }
                    }
                    

                    for (var ninjaCookie : result.cookies) {
                        httpServletResponse.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookue(ninjaCookie));
                    }
                    
                    
                    
                    

                    if (result.outputStreamRenderer.isPresent()) {
                        result.outputStreamRenderer.get().streamTo(httpServletResponse.getOutputStream());
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
                    cookie.getSecure(),
                    cookie.isHttpOnly());
        }

        public static Cookie convertNinjaCookieToServletCookue(NinjaCookie ninjaCookie) {

            var cookie = new Cookie(ninjaCookie.name(), ninjaCookie.value());

            ninjaCookie.comment().ifPresent(c -> cookie.setComment(c));
            ninjaCookie.domain().ifPresent(d -> cookie.setDomain(d));
            cookie.setMaxAge(ninjaCookie.maxAge());
            ninjaCookie.path().ifPresent(p -> cookie.setPath(p));
            cookie.setSecure(ninjaCookie.secure());
            cookie.setHttpOnly(ninjaCookie.httpOnly());

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

        ////////////////////////////////////////////////////////////////////////////
    ///// Session
    ////////////////////////////////////////////////////////////////////////////
    
    
    public static Optional<NinjaSession> getSession(
                List<NinjaCookie> ninjaCookies,
                SecretKey secretKeyForSessionEncryption) {
            var ninjaCookie = ninjaCookies.stream()
                    .filter(c -> c.name().equals(NINJA_SESSION_COOKIE_NAME))
                    .findFirst();

            if (ninjaCookie.isPresent()) {
                var ninjaSessionCookie = ninjaCookie.get();

                var now = System.currentTimeMillis();

                try {
                    var claims = Jwts.parser()
                            .verifyWith(secretKeyForSessionEncryption)
                            .build()
                            .parseSignedClaims(ninjaSessionCookie.value())
                            .getPayload();

                    if (claims.getNotBefore() != null /* Not our Api. We have to do a null check :( */
                            && now < claims.getNotBefore().getTime()) {
                        return Optional.empty();
                    }

                    if (claims.getExpiration() != null /* Not our Api. We have to do a null check :( */
                            && now > claims.getExpiration().getTime()) {
                        return Optional.empty();
                    }

                    var mapBuilder = ImmutableMap.<String, String>builder();
                    for (var e : claims.entrySet()) {
                        mapBuilder.put(e.getKey(), e.getValue().toString());
                    }
                    var ninjaSession = new NinjaSession(mapBuilder.build());

                    return Optional.of(ninjaSession);
                } catch (Exception e) {
                    logger.debug("Opsi. Error parsing Ninja Session. I am ignoring this session.", e);
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
    
        public static NinjaCookie removeNinjaSession() {
            int REMOVE_SESSION_MAX_AGE = 0;
            var cookie = new NinjaCookie(
                    NINJA_SESSION_COOKIE_NAME, 
                    "",
                    Optional.empty(),
                    Optional.empty(),
                    REMOVE_SESSION_MAX_AGE,
                    Optional.of(NINJA_SESSION_PATH),
                    false,
                    false);
                        
            
            return cookie;
        }

        public static NinjaCookie saveSession(
                NinjaSession ninjaSession,
                SecretKey secretKeyForSessionEncryption,
                Optional<Long> sessionExpiryTimeInSeconds) {

            // some setup
            Instant now = Instant.now();

            Optional<Instant> expiryInstant = Optional.empty();

            if (ninjaSession.get("exp").isPresent()) {
                expiryInstant = Optional.of(Instant.ofEpochSecond(Long.parseLong(ninjaSession.get("exp").get())));
            }

            if (expiryInstant.isEmpty() && sessionExpiryTimeInSeconds.isPresent()) {
                expiryInstant = Optional.of(now.plusSeconds(sessionExpiryTimeInSeconds.get()));
            }

            // build jwt
            var nowDate = Date.from(now);
            var jwsBuilder = Jwts.builder()
                    .notBefore(nowDate)
                    .issuedAt(nowDate);

            expiryInstant.ifPresent(i -> jwsBuilder.expiration(Date.from(i)));

            String jws = jwsBuilder
                    .claims(ninjaSession.keyValueStore())
                    .signWith(secretKeyForSessionEncryption)
                    .compact();

            var maxAge = expiryInstant.map(i -> (int) Duration.between(now, i).getSeconds())
                    .orElse(0); // 0 is a session cookie
            
            //build cookie from jwt
            var cookie = new NinjaCookie(
                    NINJA_SESSION_COOKIE_NAME, 
                    jws,
                    Optional.empty(),
                    Optional.empty(),
                    maxAge,
                    Optional.of(NINJA_SESSION_PATH),
                    false,
                    false);

            return cookie;
        }
    }

}
