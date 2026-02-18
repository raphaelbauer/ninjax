package org.r10r.ninjax.core.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.r10r.ninjax.core.FileItem;
import org.r10r.ninjax.core.FilterChain;
import org.r10r.ninjax.core.HttpOnly;
import org.r10r.ninjax.core.NinjaCookie;
import org.r10r.ninjax.core.NinjaSession;
import org.r10r.ninjax.core.PathParameterExtractor;
import org.r10r.ninjax.core.Request;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.core.RouteFinder;
import org.r10r.ninjax.core.Router;
import org.r10r.ninjax.core.Secure;
import org.r10r.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.r10r.ninjax.core.NinjaSessionConverter;

/**
 * Pure JDK HttpServer implementation (no Jetty, no Servlet API, no external
 * deps).
 *
 * Features / suggestions implemented: 1) Binary-safe multipart/form-data
 * parsing (byte scanning for boundary, not line-based). 2) Strip exactly one
 * trailing CRLF (or LF) from multipart text field values. 3) Limits: - max
 * upload size across the whole request body when parsing urlencoded/multipart -
 * max in-memory size for multipart text field parts; spills to temp file beyond
 * that Defaults are 10 MiB each. 4) Jetty-like blocking behavior: new
 * NinjaHttpServer(router, props) blocks (join) until stop() / shutdown hook.
 *
 * Properties: - ninja.port (default 8080) - ninja.http.maxUploadBytes (default
 * 10485760) - ninja.http.maxInMemoryBytes (default 10485760)
 */
public class NinjaHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(NinjaHttpServer.class);

    public final RouteFinder routeFinder;
    public final NinjaProperties ninjaProperties;

    private final int serverPort;
    private final NinjaSessionConverter ninjaSessionConverter;

    private static final long DEFAULT_LIMIT_BYTES = 10L * 1024L * 1024L; // 10 MiB
    private final long maxUploadBytes;
    private final long maxInMemoryBytes;

    private volatile HttpServer server;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private static final String NINJA_LOGO
            = """
                  _______  .___ _______        ____.  _____  ____  ___
                  \\      \\ |   |\\      \\      |    | /  _  \\ \\   \\/  /
                  /   |   \\|   |/   |   \\     |    |/  /_\\  \\ \\     /
                 /    |    \\   /    |    \\/\\__|    /    |    \\/     \\
                 \\____|__  /___\\____|__  /\\________\\____|__  /___/\\  \\
                         \\/            \\/                  \\/      \\_/
            """;

    /**
     * Starts the server and BLOCKS (Jetty-like) until stop() is called or JVM
     * is shutting down.
     */
    public NinjaHttpServer(Router router, NinjaProperties ninjaProperties) throws RuntimeException {
        this.routeFinder = new RouteFinder(router);
        this.ninjaProperties = ninjaProperties;

        this.serverPort = Integer.parseInt(ninjaProperties.get("ninja.port").orElse("8080"));
        this.ninjaSessionConverter = new NinjaSessionConverter(ninjaProperties);

        this.maxUploadBytes = parseLong(ninjaProperties.get("ninja.http.maxUploadBytes"), DEFAULT_LIMIT_BYTES);
        this.maxInMemoryBytes = parseLong(ninjaProperties.get("ninja.http.maxInMemoryBytes"), DEFAULT_LIMIT_BYTES);

        try {
            start();
            join(); // block like Jetty
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static long parseLong(Optional<String> v, long dflt) {
        if (v == null || v.isEmpty()) {
            return dflt;
        }
        try {
            return Long.parseLong(v.get().trim());
        } catch (Exception e) {
            return dflt;
        }
    }

    /**
     * Starts the underlying JDK HttpServer. Non-blocking by itself.
     */
    private void start() throws Exception {
        System.out.println(NINJA_LOGO);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
        httpServer.createContext("/", new NinjaHandler(maxUploadBytes, maxInMemoryBytes));

        //var exeuctor = NinjaHttpServer.createBoundedExecutor();
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
        this.server = httpServer;

        logger.info("NinjaHttpServer started on port {}", serverPort);

        // Ensure "constructor join" is released on Ctrl+C / SIGTERM
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            try {
                stop();
            } catch (Throwable ignored) {
            }
        }));
    }

    /**
     * Blocks current thread until stop() is called (or the thread is
     * interrupted).
     */
    private void join() throws InterruptedException {
        stopLatch.await();
    }

    /**
     * Stops accepting new requests. Existing exchanges get a small grace
     * window.
     */
    public void stop() {
        HttpServer s = this.server;
        if (s != null) {
            s.stop(1);
        }
        stopLatch.countDown();
        logger.info("NinjaHttpServer stopped");
    }

    private final class NinjaHandler implements HttpHandler {

        private final long maxUploadBytes;
        private final long maxInMemoryBytes;

        private NinjaHandler(long maxUploadBytes, long maxInMemoryBytes) {
            this.maxUploadBytes = maxUploadBytes;
            this.maxInMemoryBytes = maxInMemoryBytes;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Path> tempFilesToDelete = new ArrayList<>();

            try {
                String httpMethod = exchange.getRequestMethod();
                String requestPath = exchange.getRequestURI().getPath();
                var routingResult = routeFinder.getRouteFor(httpMethod, requestPath);

                if (routingResult.isEmpty()) {
                    sendPlain(exchange, 404, "Opsi. Not found");
                    return;
                }

                var route = routingResult.get();

                Request.Headers headers = NinjaHttpServerHelper.extractHeaders(exchange.getRequestHeaders());

                List<NinjaCookie> ninjaCookies = NinjaHttpServerHelper.parseCookies(exchange.getRequestHeaders());
                Optional<NinjaCookie> ninjaSessionCookie = ninjaCookies.stream()
                        .filter(c -> c.name().equals(NinjaSessionConverter.NINJA_SESSION_COOKIE_NAME))
                        .findFirst();

                Optional<NinjaSession> ninjaSessionInRequest = ninjaSessionCookie
                        .map(c -> ninjaSessionConverter.extractSessionFromCookie(c))
                        .orElseGet(Optional::empty);

                NinjaHttpServerHelper.ParsedBody parsedBody
                        = NinjaHttpServerHelper.parseBodyAndParameters(exchange, tempFilesToDelete, maxUploadBytes, maxInMemoryBytes);

                Map<String, String[]> parameterMap = parsedBody.parameterMap();

                Request.InputStreamGetter inputStreamGetter = () -> {
                    if (parsedBody.bodyAlreadyConsumed()) {
                        return InputStream.nullInputStream();
                    }
                    return exchange.getRequestBody();
                };

                Request.FileItemGetter fileItemGetter = (String fieldName)
                        -> parsedBody.firstFile(fieldName).map(NinjaHttpServerHelper.MultipartFile::toFileItem);

                Request.FileItemsGetter fileItemsGetter = (String fieldName)
                        -> parsedBody.files(fieldName).stream()
                                .map(NinjaHttpServerHelper.MultipartFile::toFileItem)
                                .toList();

                var payload = new Request.Payload(Map.of());

                var pathParams = PathParameterExtractor.extractPathParameters(
                        route.pathRegex(),
                        route.parameters,
                        requestPath
                );

                var parameters = new Request.Parameters(parameterMap);
                Locale locale = NinjaHttpServerHelper.extractLocale(exchange.getRequestHeaders());

                Request request = Request.builder()
                        .requestPath(requestPath)
                        .pathParameters(pathParams)
                        .inputStreamGetter(inputStreamGetter)
                        .fileItemGetter(fileItemGetter)
                        .fileItemsGetter(fileItemsGetter)
                        .ninjaCookies(ninjaCookies)
                        .payload(payload)
                        .headers(headers)
                        .parameters(parameters)
                        .ninjaSession(ninjaSessionInRequest)
                        .language(locale)
                        .build();

                FilterChain chain = new FilterChain(route.filters, 0, route.controllerMethod());
                Result result = chain.doFilter(request);

                Headers respHeaders = exchange.getResponseHeaders();
                respHeaders.set("Content-Type", result.contentType());
                NinjaHttpServerHelper.addHeaders(respHeaders, result.headers());

                switch (result.ninjaSessionState()) {
                    case Result.Exists exists -> {
                        NinjaSession ninjaSessionForResponse = exists.getSession();
                        var cookie = ninjaSessionConverter.createCookieWithInformationOfNinjaSession(ninjaSessionForResponse);
                        respHeaders.add("Set-Cookie", NinjaHttpServerHelper.toSetCookieHeader(cookie));
                    }
                    case Result.Remove remove -> {
                        var cookie = ninjaSessionConverter.createCookieToRemoveNinjaSession();
                        respHeaders.add("Set-Cookie", NinjaHttpServerHelper.toSetCookieHeader(cookie));
                    }
                    case Result.UnknownButDontTouch unknown -> {
                        /* no-op */ }
                }

                for (var ninjaCookie : result.cookies()) {
                    respHeaders.add("Set-Cookie", NinjaHttpServerHelper.toSetCookieHeader(ninjaCookie));
                }

                int status = result.status();

                if (result.outputStreamRenderer().isPresent()) {
                    exchange.sendResponseHeaders(status, 0); // chunked
                    try (OutputStream os = exchange.getResponseBody()) {
                        result.outputStreamRenderer().get().streamTo(os);
                    }
                } else {
                    exchange.sendResponseHeaders(status, -1);
                }

            } catch (NinjaHttpServerHelper.PayloadTooLargeException tooLarge) {
                sendPlain(exchange, 413, "Payload too large");
            } catch (Throwable t) {
                logger.error("OMG! Something really bad happened. Time to investigate...", t);
                try {
                    sendPlain(exchange, 500, "Wow. Something really bad happened. Ask the owner of this server if error persists...");
                } catch (Throwable e) {
                    logger.debug("I was not able to send a message via http to the user. That may be expected depending on the error", e);
                }
            } finally {
                for (Path p : tempFilesToDelete) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Throwable ignore) {
                    }
                }
                try {
                    exchange.getRequestBody().close();
                } catch (Throwable ignore) {
                }
                try {
                    exchange.close();
                } catch (Throwable ignore) {
                }
            }
        }

        private void sendPlain(HttpExchange exchange, int status, String text) throws IOException {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    public static final class NinjaHttpServerHelper {

        private NinjaHttpServerHelper() {
        }

        // -------- errors / limits --------
        public static final class PayloadTooLargeException extends IOException {

            public PayloadTooLargeException(String message) {
                super(message);
            }
        }

        private static final class LimitedInputStream extends FilterInputStream {

            private final long maxBytes;
            private long count;

            LimitedInputStream(InputStream in, long maxBytes) {
                super(in);
                this.maxBytes = maxBytes;
                this.count = 0;
            }

            @Override
            public int read() throws IOException {
                int r = super.read();
                if (r >= 0) {
                    increment(1);
                }
                return r;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int r = super.read(b, off, len);
                if (r > 0) {
                    increment(r);
                }
                return r;
            }

            private void increment(long n) throws PayloadTooLargeException {
                count += n;
                if (count > maxBytes) {
                    throw new PayloadTooLargeException("Request body exceeded maxUploadBytes=" + maxBytes);
                }
            }
        }

        // -------- headers / cookies --------
        public static Request.Headers extractHeaders(Headers reqHeaders) {
            Map<String, List<String>> map = new HashMap<>();
            for (Map.Entry<String, List<String>> e : reqHeaders.entrySet()) {
                map.put(e.getKey(), List.copyOf(e.getValue()));
            }
            return new Request.Headers(map);
        }

        public static void addHeaders(Headers responseHeaders, Map<String, List<String>> headers) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                for (String value : entry.getValue()) {
                    responseHeaders.add(headerName, value);
                }
            }
        }

        public static List<NinjaCookie> parseCookies(Headers requestHeaders) {
            List<String> cookieHeaders = headerValues(requestHeaders, "Cookie");
            if (cookieHeaders.isEmpty()) {
                return List.of();
            }

            List<NinjaCookie> out = new ArrayList<>();
            for (String header : cookieHeaders) {
                if (header == null || header.isBlank()) {
                    continue;
                }
                String[] parts = header.split(";");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.isEmpty()) {
                        continue;
                    }
                    int eq = p.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String name = p.substring(0, eq).trim();
                    String value = p.substring(eq + 1).trim();
                    out.add(new NinjaCookie(
                            name,
                            value,
                            Optional.empty(),
                            -1,
                            Optional.empty(),
                            Secure.ofBoolean(false),
                            HttpOnly.ofBoolean(false)
                    ));
                }
            }
            return out;
        }

        public static String toSetCookieHeader(NinjaCookie ninjaCookie) {
            StringBuilder sb = new StringBuilder();
            sb.append(ninjaCookie.name()).append("=").append(ninjaCookie.value() == null ? "" : ninjaCookie.value());
            ninjaCookie.path().ifPresent(p -> sb.append("; Path=").append(p));
            ninjaCookie.domain().ifPresent(d -> sb.append("; Domain=").append(d));
            if (ninjaCookie.maxAge() >= 0) {
                sb.append("; Max-Age=").append(ninjaCookie.maxAge());
            }
            if (ninjaCookie.secure().toBoolean()) {
                sb.append("; Secure");
            }
            if (ninjaCookie.httpOnly().toBoolean()) {
                sb.append("; HttpOnly");
            }
            return sb.toString();
        }

        public static Locale extractLocale(Headers requestHeaders) {
            String al = firstHeader(requestHeaders, "Accept-Language").orElse(null);
            if (al == null || al.isBlank()) {
                return Locale.getDefault();
            }
            String first = al.split(",")[0].trim();
            if (first.isBlank()) {
                return Locale.getDefault();
            }
            String tag = first.split(";")[0].trim().replace('_', '-');
            return Locale.forLanguageTag(tag);
        }

        // -------- parameters / body parsing --------
        public record ParsedBody(
                Map<String, String[]> parameterMap,
                Map<String, List<MultipartFile>> filesByField,
                boolean bodyAlreadyConsumed
                ) {

            Optional<MultipartFile> firstFile(String field) {
                List<MultipartFile> l = filesByField.get(field);
                if (l == null || l.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(l.get(0));
            }

            List<MultipartFile> files(String field) {
                return filesByField.getOrDefault(field, List.of());
            }
        }

        /**
         * Parses query parameters plus body parameters/files (when urlencoded
         * or multipart). Enforces: - maxUploadBytes (overall body read) -
         * maxInMemoryBytes (per multipart text field; spills beyond to temp
         * file)
         */
        public static ParsedBody parseBodyAndParameters(
                HttpExchange exchange,
                List<Path> tempFilesToDelete,
                long maxUploadBytes,
                long maxInMemoryBytes
        ) throws IOException {

            // pre-check Content-Length if present
            OptionalLong cl = contentLength(exchange.getRequestHeaders());
            if (cl.isPresent() && cl.getAsLong() > maxUploadBytes) {
                throw new PayloadTooLargeException("Content-Length " + cl.getAsLong() + " exceeds maxUploadBytes=" + maxUploadBytes);
            }

            Map<String, List<String>> accParams = new LinkedHashMap<>();

            // Query params
            String rawQuery = exchange.getRequestURI().getRawQuery();
            if (rawQuery != null && !rawQuery.isBlank()) {
                parseUrlEncodedInto(accParams, rawQuery, StandardCharsets.UTF_8);
            }

            Map<String, List<MultipartFile>> filesByField = new LinkedHashMap<>();
            boolean consumed = false;

            String method = exchange.getRequestMethod();
            String ct = firstHeader(exchange.getRequestHeaders(), "Content-Type").orElse("");
            String ctLower = ct == null ? "" : ct.toLowerCase(Locale.ROOT);

            if (hasRequestBody(method) && (ctLower.startsWith("application/x-www-form-urlencoded") || ctLower.startsWith("multipart/form-data"))) {
                consumed = true;

                // enforce maxUploadBytes while reading
                InputStream limited = new LimitedInputStream(exchange.getRequestBody(), maxUploadBytes);

                if (ctLower.startsWith("application/x-www-form-urlencoded")) {
                    Charset cs = contentTypeCharset(ct).orElse(StandardCharsets.UTF_8);
                    byte[] body = readAllBytes(limited);
                    String raw = new String(body, cs);
                    if (!raw.isBlank()) {
                        parseUrlEncodedInto(accParams, raw, cs);
                    }

                } else {
                    String boundary = extractBoundary(ct).orElseThrow(() -> new IOException("multipart/form-data missing boundary"));
                    MultipartParseResult m = Multipart.parse(limited, boundary, tempFilesToDelete, maxInMemoryBytes);

                    for (var e : m.fields.entrySet()) {
                        for (String v : e.getValue()) {
                            accParams.computeIfAbsent(e.getKey(), _k -> new ArrayList<>()).add(v);
                        }
                    }
                    filesByField.putAll(m.filesByField);
                }
            }

            Map<String, String[]> parameterMap = new LinkedHashMap<>();
            for (var e : accParams.entrySet()) {
                parameterMap.put(e.getKey(), e.getValue().toArray(String[]::new));
            }

            return new ParsedBody(parameterMap, filesByField, consumed);
        }

        private static OptionalLong contentLength(Headers headers) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase("Content-Length")) {
                    if (e.getValue() == null || e.getValue().isEmpty()) {
                        return OptionalLong.empty();
                    }
                    try {
                        return OptionalLong.of(Long.parseLong(e.getValue().get(0).trim()));
                    } catch (Exception ex) {
                        return OptionalLong.empty();
                    }
                }
            }
            return OptionalLong.empty();
        }

        private static boolean hasRequestBody(String method) {
            return "POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method);
        }

        private static void parseUrlEncodedInto(Map<String, List<String>> acc, String raw, Charset cs) {
            String[] pairs = raw.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String k = eq >= 0 ? pair.substring(0, eq) : pair;
                String v = eq >= 0 ? pair.substring(eq + 1) : "";
                String key = urlDecode(k, cs);
                String val = urlDecode(v, cs);
                acc.computeIfAbsent(key, _k -> new ArrayList<>()).add(val);
            }
        }

        private static String urlDecode(String s, Charset cs) {
            try {
                return URLDecoder.decode(s, cs);
            } catch (Exception e) {
                return s;
            }
        }

        private static Optional<Charset> contentTypeCharset(String contentType) {
            if (contentType == null) {
                return Optional.empty();
            }
            String lower = contentType.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("charset=");
            if (idx < 0) {
                return Optional.empty();
            }
            String cs = contentType.substring(idx + "charset=".length()).trim();
            int semi = cs.indexOf(';');
            if (semi >= 0) {
                cs = cs.substring(0, semi).trim();
            }
            cs = cs.replace("\"", "");
            try {
                return Optional.of(Charset.forName(cs));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        private static Optional<String> extractBoundary(String contentType) {
            if (contentType == null) {
                return Optional.empty();
            }
            String[] parts = contentType.split(";");
            for (String p : parts) {
                String t = p.trim();
                if (t.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                    String b = t.substring("boundary=".length()).trim();
                    if (b.startsWith("\"") && b.endsWith("\"") && b.length() >= 2) {
                        b = b.substring(1, b.length() - 1);
                    }
                    if (!b.isBlank()) {
                        return Optional.of(b);
                    }
                }
            }
            return Optional.empty();
        }

        private static byte[] readAllBytes(InputStream is) throws IOException {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) >= 0) {
                    if (r > 0) {
                        bos.write(buf, 0, r);
                    }
                }
                return bos.toByteArray();
            }
        }

        // -------- multipart --------
        public static final class MultipartFile {

            public final String fieldName;
            public final String submittedFileName; // may be null
            public final String contentType;       // may be null
            public final long size;
            public final Path tempFile;

            MultipartFile(String fieldName, String submittedFileName, String contentType, long size, Path tempFile) {
                this.fieldName = fieldName;
                this.submittedFileName = submittedFileName;
                this.contentType = contentType;
                this.size = size;
                this.tempFile = tempFile;
            }

            public FileItem toFileItem() {
                try {
                    return new FileItem(
                            submittedFileName,
                            contentType,
                            size,
                            Files.newInputStream(tempFile)
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        private static final class MultipartParseResult {

            final Map<String, List<String>> fields = new LinkedHashMap<>();
            final Map<String, List<MultipartFile>> filesByField = new LinkedHashMap<>();
        }

        private static final class Multipart {

            static MultipartParseResult parse(
                    InputStream rawIn,
                    String boundary,
                    List<Path> tempFilesToDelete,
                    long maxInMemoryBytes
            ) throws IOException {

                BufferedInputStream in = new BufferedInputStream(rawIn, 128 * 1024);

                byte[] boundaryStart = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
                byte[] boundaryDelim = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

                // consume preamble / first boundary
                if (!consumeExact(in, boundaryStart)) {
                    if (!scanTo(in, boundaryStart, boundaryDelim)) {
                        return new MultipartParseResult();
                    }
                }

                boolean done = consumeBoundaryTrailer(in);
                if (done) {
                    return new MultipartParseResult();
                }

                MultipartParseResult res = new MultipartParseResult();

                while (true) {
                    Map<String, String> headers = readPartHeaders(in);
                    ContentDisposition cd = ContentDisposition.parse(headers.get("Content-Disposition"));
                    String contentType = headers.get("Content-Type");

                    if (cd == null || cd.name == null) {
                        BoundaryHit hit = drainBodyToBoundary(in, boundaryDelim, boundaryStart, OutputStream.nullOutputStream());
                        if (hit == BoundaryHit.FINAL) {
                            return res;
                        }
                        continue;
                    }

                    if (cd.filename != null) {
                        // file part: always to temp file
                        Path tmp = Files.createTempFile("ninjax-upload-", ".tmp");
                        tempFilesToDelete.add(tmp);

                        try (OutputStream os = Files.newOutputStream(tmp)) {
                            BoundaryHit hit = drainBodyToBoundary(in, boundaryDelim, boundaryStart, os);
                            long size = Files.size(tmp);

                            MultipartFile mf = new MultipartFile(cd.name, cd.filename, contentType, size, tmp);
                            res.filesByField.computeIfAbsent(cd.name, _k -> new ArrayList<>()).add(mf);

                            if (hit == BoundaryHit.FINAL) {
                                return res;
                            }
                        }
                    } else {
                        // field part: in-memory up to maxInMemoryBytes, then spill
                        Charset cs = charsetFromContentType(contentType).orElse(StandardCharsets.UTF_8);

                        SpillBuffer sb = new SpillBuffer(maxInMemoryBytes, tempFilesToDelete);
                        BoundaryHit hit = drainBodyToBoundary(in, boundaryDelim, boundaryStart, sb);

                        byte[] raw = sb.readAllBytesAndClose();
                        raw = stripSingleTrailingCrlf(raw);

                        String val = new String(raw, cs);
                        res.fields.computeIfAbsent(cd.name, _k -> new ArrayList<>()).add(val);

                        if (hit == BoundaryHit.FINAL) {
                            return res;
                        }
                    }
                }
            }

            private enum BoundaryHit {
                NEXT, FINAL
            }

            /**
             * Binary-safe drain until boundary delimiter.
             */
            private static BoundaryHit drainBodyToBoundary(BufferedInputStream in,
                    byte[] boundaryDelim,
                    byte[] boundaryStart,
                    OutputStream out) throws IOException {
                int maxNeedle = Math.max(boundaryDelim.length, boundaryStart.length);
                byte[] window = new byte[maxNeedle];
                int wLen = 0;

                while (true) {
                    int b = in.read();
                    if (b == -1) {
                        throw new EOFException("Unexpected EOF in multipart body");
                    }

                    if (wLen < window.length) {
                        window[wLen++] = (byte) b;
                    } else {
                        out.write(window[0]);
                        System.arraycopy(window, 1, window, 0, window.length - 1);
                        window[window.length - 1] = (byte) b;
                    }

                    if (endsWith(window, wLen, boundaryDelim)) {
                        int keep = wLen - boundaryDelim.length;
                        if (keep > 0) {
                            out.write(window, 0, keep);
                        }
                        wLen = 0;
                        boolean finalBoundary = consumeBoundaryTrailer(in);
                        return finalBoundary ? BoundaryHit.FINAL : BoundaryHit.NEXT;
                    }

                    // boundary at immediate start (empty body)
                    if (endsWith(window, wLen, boundaryStart)) {
                        int keep = wLen - boundaryStart.length;
                        if (keep > 0) {
                            out.write(window, 0, keep);
                        }
                        wLen = 0;
                        boolean finalBoundary = consumeBoundaryTrailer(in);
                        return finalBoundary ? BoundaryHit.FINAL : BoundaryHit.NEXT;
                    }
                }
            }

            /**
             * After "--boundary" consume either "--" (final) or CRLF (next).
             */
            private static boolean consumeBoundaryTrailer(BufferedInputStream in) throws IOException {
                in.mark(2);
                int a = in.read();
                int b = in.read();
                if (a == '-' && b == '-') {
                    // final; optionally followed by CRLF
                    in.mark(2);
                    int c = in.read();
                    int d = in.read();
                    if (!(c == '\r' && d == '\n')) {
                        in.reset();
                    }
                    return true;
                }

                if (a == '\r' && b == '\n') {
                    return false;
                }

                // tolerate LF-only
                if (a == '\n') {
                    return false;
                }

                throw new IOException("Malformed multipart boundary trailer: expected CRLF or --");
            }

            private static Map<String, String> readPartHeaders(BufferedInputStream in) throws IOException {
                Map<String, String> headers = new LinkedHashMap<>();
                while (true) {
                    String line = readHeaderLine(in);
                    if (line == null) {
                        throw new EOFException("Unexpected EOF in multipart headers");
                    }
                    if (line.isEmpty()) {
                        return headers;
                    }

                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        String name = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        headers.put(name, value);
                    }
                }
            }

            private static String readHeaderLine(BufferedInputStream in) throws IOException {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
                int prev = -1;
                while (true) {
                    int c = in.read();
                    if (c == -1) {
                        return bos.size() == 0 ? null : bos.toString(StandardCharsets.ISO_8859_1);
                    }
                    if (c == '\n') {
                        byte[] arr = bos.toByteArray();
                        if (prev == '\r' && arr.length > 0 && arr[arr.length - 1] == '\r') {
                            arr = Arrays.copyOf(arr, arr.length - 1);
                        }
                        return new String(arr, StandardCharsets.ISO_8859_1);
                    }
                    bos.write(c);
                    prev = c;
                }
            }

            private static boolean consumeExact(BufferedInputStream in, byte[] seq) throws IOException {
                in.mark(seq.length);
                for (byte b : seq) {
                    int r = in.read();
                    if (r != (b & 0xff)) {
                        in.reset();
                        return false;
                    }
                }
                return true;
            }

            private static boolean scanTo(BufferedInputStream in, byte[]... needles) throws IOException {
                int max = 0;
                for (byte[] n : needles) {
                    max = Math.max(max, n.length);
                }
                byte[] window = new byte[max];
                int wLen = 0;

                while (true) {
                    int b = in.read();
                    if (b == -1) {
                        return false;
                    }

                    if (wLen < window.length) {
                        window[wLen++] = (byte) b;
                    } else {
                        System.arraycopy(window, 1, window, 0, window.length - 1);
                        window[window.length - 1] = (byte) b;
                    }

                    for (byte[] n : needles) {
                        if (endsWith(window, wLen, n)) {
                            return true;
                        }
                    }
                }
            }

            private static boolean endsWith(byte[] window, int wLen, byte[] needle) {
                if (wLen < needle.length) {
                    return false;
                }
                int off = wLen - needle.length;
                for (int i = 0; i < needle.length; i++) {
                    if (window[off + i] != needle[i]) {
                        return false;
                    }
                }
                return true;
            }

            // suggestion #2: strip exactly one trailing newline from field value
            private static byte[] stripSingleTrailingCrlf(byte[] raw) {
                if (raw.length >= 2 && raw[raw.length - 2] == '\r' && raw[raw.length - 1] == '\n') {
                    return Arrays.copyOf(raw, raw.length - 2);
                }
                if (raw.length >= 1 && raw[raw.length - 1] == '\n') {
                    return Arrays.copyOf(raw, raw.length - 1);
                }
                return raw;
            }

            private static Optional<Charset> charsetFromContentType(String ct) {
                if (ct == null) {
                    return Optional.empty();
                }
                String lower = ct.toLowerCase(Locale.ROOT);
                int idx = lower.indexOf("charset=");
                if (idx < 0) {
                    return Optional.empty();
                }
                String cs = ct.substring(idx + "charset=".length()).trim();
                int semi = cs.indexOf(';');
                if (semi >= 0) {
                    cs = cs.substring(0, semi).trim();
                }
                cs = cs.replace("\"", "");
                try {
                    return Optional.of(Charset.forName(cs));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }

            /**
             * OutputStream that buffers up to maxInMemory bytes in memory, then
             * spills to a temp file.
             */
            private static final class SpillBuffer extends OutputStream {

                private final long maxInMemory;
                private final List<Path> tempFilesToDelete;

                private ByteArrayOutputStream mem;
                private OutputStream fileOut;
                private Path tempFile;
                private long size;

                SpillBuffer(long maxInMemory, List<Path> tempFilesToDelete) {
                    this.maxInMemory = Math.max(0, maxInMemory);
                    this.tempFilesToDelete = tempFilesToDelete;
                    this.mem = new ByteArrayOutputStream((int) Math.min(8192, Math.max(0, maxInMemory)));
                    this.size = 0;
                }

                @Override
                public void write(int b) throws IOException {
                    ensureCapacityFor(1);
                    currentOut().write(b);
                    size++;
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    if (len <= 0) {
                        return;
                    }
                    ensureCapacityFor(len);
                    currentOut().write(b, off, len);
                    size += len;
                }

                private OutputStream currentOut() {
                    return (fileOut != null) ? fileOut : mem;
                }

                private void ensureCapacityFor(int incoming) throws IOException {
                    if (fileOut != null) {
                        return;
                    }
                    if (size + incoming <= maxInMemory) {
                        return;
                    }

                    // spill
                    tempFile = Files.createTempFile("ninjax-field-", ".tmp");
                    tempFilesToDelete.add(tempFile);

                    fileOut = new BufferedOutputStream(Files.newOutputStream(tempFile), 64 * 1024);
                    mem.writeTo(fileOut);
                    mem = null;
                }

                byte[] readAllBytesAndClose() throws IOException {
                    close();
                    if (tempFile != null) {
                        return Files.readAllBytes(tempFile);
                    }
                    return mem == null ? new byte[0] : mem.toByteArray();
                }

                @Override
                public void close() throws IOException {
                    if (fileOut != null) {
                        fileOut.close();
                    }
                }
            }
        }

        private static final class ContentDisposition {

            final String name;
            final String filename;

            ContentDisposition(String name, String filename) {
                this.name = name;
                this.filename = filename;
            }

            static ContentDisposition parse(String headerValue) {
                if (headerValue == null) {
                    return null;
                }
                String[] parts = headerValue.split(";");
                String name = null;
                String filename = null;
                for (String p : parts) {
                    String t = p.trim();
                    int eq = t.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String k = t.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                    String v = t.substring(eq + 1).trim();
                    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                        v = v.substring(1, v.length() - 1);
                    }
                    if (k.equals("name")) {
                        name = v;
                    }
                    if (k.equals("filename")) {
                        filename = v;
                    }
                }
                if (name == null) {
                    return null;
                }
                return new ContentDisposition(name, filename);
            }
        }

        // -------- generic header helpers --------
        private static Optional<String> firstHeader(Headers headers, String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    List<String> v = e.getValue();
                    if (v != null && !v.isEmpty()) {
                        return Optional.ofNullable(v.get(0));
                    }
                }
            }
            return Optional.empty();
        }

        private static List<String> headerValues(Headers headers, String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    List<String> v = e.getValue();
                    return v == null ? List.of() : v;
                }
            }
            return List.of();
        }

    }

    static ExecutorService createBoundedExecutor() {
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        int queueSize = 1024;

        return new ThreadPoolExecutor(
                threads,
                threads,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                Thread.ofPlatform().name("ninjax-http-", 0).factory(),
                // Backpressure: when saturated, run handler on acceptor thread
                // (or use AbortPolicy to return 503 yourself).
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
