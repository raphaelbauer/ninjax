package org.ninjax.core;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.jetty.servlet.FilterHolder;
import org.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaJetty {

    private static Logger logger = LoggerFactory.getLogger(NinjaJetty.class);

    public final RouteFinder routeFinder;
    public final NinjaProperties ninjaProperties;

    private final Optional<Long> sessionExpiryTimeInSeconds;

    public static final String NINJA_SESSION_COOKIE_NAME = "NINJA_SESSION";

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
        Server server = new Server(8080);

        // Create a ServletContextHandler with context path
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SECURITY);
        context.setContextPath("/");

        // Map servlets to the context handler
        server.setHandler(context);

        // Add a simple servlet to the context
        //context.addServlet(new ServletHolder(new HelloServlet()), "/hello");
        context.addFilter(new FilterHolder(new HelloServletFilter()), "/*", null);

        // Start the server
        server.start();
        server.join();
    }

    public class HelloServletFilter implements jakarta.servlet.Filter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, jakarta.servlet.FilterChain fc) throws IOException, ServletException {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            var httpMethod = httpServletRequest.getMethod();
            var requestURI = httpServletRequest.getRequestURI();
            var routingResult = routeFinder.getRouteFor(httpMethod, requestURI);

            if (routingResult.isPresent()) {

                var route = routingResult.get();

                List<NinjaCookie> ninjaCookies = httpServletRequest.getCookies() == null
                        ? List.of()
                        : Arrays.stream(httpServletRequest.getCookies())
                                .map(c -> convertServletCookieToNinjaCookie(c))
                                .toList();

                var headers = extractHeaders(httpServletRequest);

                var ninjaSession = getSession(ninjaCookies);

                var context = new Context(
                        route,
                        requestURI,
                        httpServletRequest.getInputStream(),
                        ninjaCookies,
                        headers,
                        ninjaSession);

                FilterChain chain = new FilterChain(route.filters, 0, routingResult.get().controllerMethod());
                var result = chain.doFilter(context);

                var status = result.status;
                var contentType = result.contentType;

                httpServletResponse.setContentType(contentType);
                httpServletResponse.setStatus(status);
                setHeadersOnResponse(httpServletResponse, result.headers);

                saveSession(httpServletResponse, context.getNinjaSession());

                for (var ninjaCookie : result.cookies) {
                    httpServletResponse.addCookie(convertNinjaCookieToServletCookue(ninjaCookie));
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

        }
    }

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

    public void setHeadersOnResponse(HttpServletResponse response, Map<String, List<String>> headers) {
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
    
    
    public NinjaSession getSession(List<NinjaCookie> ninjaCookies) {
        var ninjaCookie = ninjaCookies.stream()
                .filter(c -> c.name().equals(NINJA_SESSION_COOKIE_NAME))
                .findFirst();
                
        if (ninjaCookie.isPresent()) {
            var ninjaSessionCookue = ninjaCookie.get();

            var now = System.currentTimeMillis();

            var claims = Jwts.parser()
                    .verifyWith(secretKeyForSessionEncryption)
                    .build()
                    .parseSignedClaims(ninjaSessionCookue.value())
                    .getPayload();

            if (claims.getNotBefore() != null  /* Not our Api. We have to do a null check :( */ 
                    && now < claims.getNotBefore().getTime()) {
                return new NinjaSession();
            }

            if (claims.getExpiration() != null  /* Not our Api. We have to do a null check :( */ 
                    && now > claims.getExpiration().getTime()) {
                return new NinjaSession();
            }

            var ninjaSession = new NinjaSession();
            for (var e : claims.entrySet()) {
                ninjaSession.put(e.getKey(), e.getValue().toString());
            }

            return ninjaSession;
        } else {
            return new NinjaSession();
        }
    }

    public void saveSession(HttpServletResponse httpServletResponse, NinjaSession ninjaSession) {
        
        // some setup
        Instant now = Instant.now();
        
        Optional<Instant> expiryInstant = Optional.empty();
        
        if (ninjaSession.get("exp").isPresent()) {
            expiryInstant = Optional.of(Instant.ofEpochSecond(Long.parseLong(ninjaSession.get("exp").get())));
        }
        
        if (expiryInstant.isEmpty() && this.sessionExpiryTimeInSeconds.isPresent()) {
            expiryInstant = Optional.of(now.plusSeconds(this.sessionExpiryTimeInSeconds.get()));
        }
        
        // build jwt
        var nowDate = Date.from(now);
        var jwsBuilder = Jwts.builder()
                .notBefore(nowDate)
                .issuedAt(nowDate);
       
        //var expireTimeOpt = expiryUnixTimestampInMilliseconds.map(e -> new Date(e));       
        expiryInstant.ifPresent(i -> jwsBuilder.expiration(Date.from(i)));

        String jws = jwsBuilder
                .claims(ninjaSession.keyValueStore)
                .signWith(secretKeyForSessionEncryption)
                .compact();
        
        //build cookie from jwt
        var sessionCookie = new Cookie(NINJA_SESSION_COOKIE_NAME, jws);
        expiryInstant.ifPresent(i -> sessionCookie.setMaxAge((int) Duration.between(now, i).getSeconds()));

        httpServletResponse.addCookie(sessionCookie);
    }

}

