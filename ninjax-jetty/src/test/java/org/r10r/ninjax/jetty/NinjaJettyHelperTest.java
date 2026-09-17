package org.r10r.ninjax.jetty;

import static com.google.common.truth.Truth.assertThat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.r10r.ninjax.core.NinjaCookie;
import org.r10r.ninjax.core.SameSite;
import org.r10r.ninjax.jetty.NinjaJetty.NinjaJettyHelper;

class NinjaJettyHelperTest {

    @Test
    void convertNinjaCookieToServletCookie_setsSameSiteAttribute() {
        // given
        NinjaCookie ninjaCookie = NinjaCookie.builder("sid", "abc").sameSite(SameSite.Lax).build();

        // when
        Cookie cookie = NinjaJettyHelper.convertNinjaCookieToServletCookie(ninjaCookie);

        // then
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void convertNinjaCookieToServletCookie_omitsSameSiteWhenNotSet() {
        // given
        NinjaCookie ninjaCookie = NinjaCookie.builder("sid", "abc").build();

        // when
        Cookie cookie = NinjaJettyHelper.convertNinjaCookieToServletCookie(ninjaCookie);

        // then
        assertThat(cookie.getAttribute("SameSite")).isNull();
    }

    @Test
    void convertServletCookieToNinjaCookie_leavesSameSiteEmpty() {
        // given
        Cookie cookie = new Cookie("sid", "abc");

        // when
        NinjaCookie ninjaCookie = NinjaJettyHelper.convertServletCookieToNinjaCookie(cookie);

        // then
        assertThat(ninjaCookie.sameSite()).isEqualTo(Optional.empty());
    }

    @Test
    void jettyWritesSameSiteIntoSetCookieHeader() throws Exception {
        // given
        NinjaCookie ninjaCookie = NinjaCookie.builder("sid", "abc")
                .path("/")
                .secure(true)
                .httpOnly(true)
                .sameSite(SameSite.Strict)
                .build();

        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) {
                response.addCookie(NinjaJettyHelper.convertNinjaCookieToServletCookie(ninjaCookie));
            }
        };

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler();
        context.addServlet(new ServletHolder(servlet), "/*");
        server.setHandler(context);
        server.start();

        try (HttpClient client = HttpClient.newHttpClient()) {
            // when
            HttpResponse<Void> response = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + connector.getLocalPort() + "/")).build(),
                    HttpResponse.BodyHandlers.discarding());

            // then
            String setCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
            assertThat(setCookie).startsWith("sid=abc");
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("HttpOnly");
            assertThat(setCookie).contains("SameSite=Strict");
        } finally {
            server.stop();
        }
    }
}
