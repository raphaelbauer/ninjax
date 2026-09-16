package org.r10r.ninjax.core.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Started in a fresh child JVM by NinjaHttpServerLimitsTest. The JDK reads its timeout properties only once per JVM,
 * so checking them inside the shared test JVM would depend on which test created a server first.
 *
 * Starts a server with maxReqTime=1s, acts as a slowloris client (trickles header lines, never finishes the request)
 * and exits with 0 if the server drops the connection after the 1s timeout, 1 otherwise (e.g. still open after 10s).
 */
public class SlowClientTimeoutCheck {

    static final int CONNECTION_CLOSED_BY_SERVER = 0;
    static final int CONNECTION_STILL_OPEN = 1;

    private static final String LOOPBACK = "127.0.0.1";
    private static final Duration GIVE_UP_AFTER = Duration.ofSeconds(10);

    public static void main(String[] args) throws Exception {
        NinjaHttpServer.applyJdkHttpServerTimeouts(1, 300);

        HttpServer server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        long startNanos = System.nanoTime();
        boolean closedByServer = trickleHeadersUntilClosed(server.getAddress().getPort());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        server.stop(0);

        // Closing before 1s would mean something other than the timeout dropped the connection.
        boolean closedByTimeout = closedByServer && elapsed.compareTo(Duration.ofSeconds(1)) >= 0;
        System.out.println("closedByServer=" + closedByServer + " after " + elapsed.toMillis() + "ms");
        System.exit(closedByTimeout ? CONNECTION_CLOSED_BY_SERVER : CONNECTION_STILL_OPEN);
    }

    private static boolean trickleHeadersUntilClosed(int port) throws IOException {
        long startNanos = System.nanoTime();
        try (Socket socket = new Socket(LOOPBACK, port)) {
            socket.setSoTimeout(200);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            out.write("GET / HTTP/1.1\r\nHost: localhost\r\n".getBytes(StandardCharsets.US_ASCII));
            while (Duration.ofNanos(System.nanoTime() - startNanos).compareTo(GIVE_UP_AFTER) < 0) {
                try {
                    out.write("X-Slow: a\r\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                    if (in.read() == -1) {
                        return true;
                    }
                } catch (SocketTimeoutException stillOpen) {
                    // server is still waiting for the rest of the request
                } catch (IOException connectionReset) {
                    return true;
                }
            }
            return false;
        }
    }
}
