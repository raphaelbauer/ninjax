package org.ninjax.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.jetty.servlet.FilterHolder;
import org.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaJetty {

    private static Logger logger = LoggerFactory.getLogger(NinjaJetty.class);

    public final RouteFinder routeFinder;
    public final NinjaProperties ninjaProperties;

    public NinjaJetty(Router router, NinjaProperties ninjaProperties) throws RuntimeException {
        this.routeFinder = new RouteFinder(router);
        this.ninjaProperties = ninjaProperties;

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

                var context = new Context(
                        route,
                        requestURI,
                        httpServletRequest.getInputStream(),
                        ninjaCookies);

                FilterChain chain = new FilterChain(route.filters, 0, routingResult.get().controllerMethod());
                var result = chain.doFilter(context);

                var status = result.status;
                var contentType = result.contentType;

                httpServletResponse.setContentType(contentType);
                httpServletResponse.setStatus(status);

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

}
