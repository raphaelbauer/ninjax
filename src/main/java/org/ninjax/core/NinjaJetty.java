package org.ninjax.core;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import org.eclipse.jetty.servlet.FilterHolder;

public class NinjaJetty {

    public final RouteFinder routeFinder;

    public NinjaJetty(Router router) throws RuntimeException {
        this.routeFinder = new RouteFinder(router);

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

    public class HelloServletFilter implements Filter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain fc) throws IOException, ServletException {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            var httpMethod = httpServletRequest.getMethod();
            var path = httpServletRequest.getRequestURI();
            var routingResult = routeFinder.getRouteFor(httpMethod, path);

            if (routingResult.isPresent()) {
                
                var route = routingResult.get();
                var context = new Context(route);
                
                
                var result = routingResult.get().controllerMethod().executeMethod(context);
                
                var status = result.status;
                var contentType = result.contentType;

                //////// OR => 
                // result.outputStreamRenderer.resultCreatorMethod(httpServletResponse.getOutputStream());
           


                httpServletResponse.setContentType(contentType);
                httpServletResponse.setStatus(status);
                httpServletResponse.getWriter().println(result.content);
                

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

//    public static class HelloServlet extends HttpServlet {
//
//        @Override
//        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//            resp.setContentType("text/plain");
//            resp.setStatus(HttpServletResponse.SC_OK);
//            resp.getWriter().println("Hello, World!");
//        }
//    }
}
