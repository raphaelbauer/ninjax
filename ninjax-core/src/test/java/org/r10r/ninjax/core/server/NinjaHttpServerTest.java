package org.r10r.ninjax.core.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.r10r.ninjax.core.HttpOnly;
import org.r10r.ninjax.core.NinjaCookie;
import org.r10r.ninjax.core.Secure;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.r10r.ninjax.core.server.NinjaHttpServer.NinjaHttpServerHelper;

class NinjaHttpServerHelperTest {

    @TempDir
    Path tmpDir;

    // ---------------- cookies ----------------
    @Test
    void parseCookies_splitsMultiplePairsAndTrims() {
        Headers h = new Headers();
        h.add("Cookie", "a=1; b=hello;  c = spaced ");
        h.add("Cookie", "d=2");

        List<NinjaCookie> cookies = NinjaHttpServerHelper.parseCookies(h);

        assertEquals(4, cookies.size());
        assertEquals("a", cookies.get(0).name());
        assertEquals("1", cookies.get(0).value());

        assertEquals("b", cookies.get(1).name());
        assertEquals("hello", cookies.get(1).value());

        assertEquals("c", cookies.get(2).name());
        assertEquals("spaced", cookies.get(2).value());

        assertEquals("d", cookies.get(3).name());
        assertEquals("2", cookies.get(3).value());
    }

    @Test
    void toSetCookieHeader_rendersAttributes() {
        NinjaCookie c = new NinjaCookie(
                "sid",
                "abc",
                Optional.of("example.com"),
                3600,
                Optional.of("/"),
                Secure.ofBoolean(true),
                HttpOnly.ofBoolean(true)
        );

        String header = NinjaHttpServerHelper.toSetCookieHeader(c);

        assertTrue(header.startsWith("sid=abc"));
        assertTrue(header.contains("; Path=/"));
        assertTrue(header.contains("; Domain=example.com"));
        assertTrue(header.contains("; Max-Age=3600"));
        assertTrue(header.contains("; Secure"));
        assertTrue(header.contains("; HttpOnly"));
    }

    // ---------------- locale ----------------
    @Test
    void extractLocale_usesFirstLanguageTag() {
        Headers h = new Headers();
        h.add("Accept-Language", "de-DE,de;q=0.9,en;q=0.8");

        Locale locale = NinjaHttpServerHelper.extractLocale(h);

        assertEquals("de", locale.getLanguage());
        assertEquals("DE", locale.getCountry());
    }

    // ---------------- parameters / body parsing ----------------
    @Test
    void parseBodyAndParameters_queryOnly_doesNotConsumeBody() throws Exception {
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("GET")
                .uri("http://localhost/test?x=1&x=2&y=hi")
                .header("Content-Type", "")
                .bodyBytes(new byte[0])
                .build();

        List<Path> toDelete = new ArrayList<>();
        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10_000, 10_000);

        assertFalse(parsed.bodyAlreadyConsumed());
        assertArrayEquals(new String[]{"1", "2"}, parsed.parameterMap().get("x"));
        assertArrayEquals(new String[]{"hi"}, parsed.parameterMap().get("y"));
        assertTrue(parsed.filesByField().isEmpty());
    }

    @Test
    void parseBodyAndParameters_urlEncoded_mergesWithQueryAndConsumesBody() throws Exception {
        byte[] body = "a=fromBody&b=2".getBytes(StandardCharsets.UTF_8);

        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/form?a=fromQuery")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .bodyBytes(body)
                .build();

        List<Path> toDelete = new ArrayList<>();
        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10_000, 10_000);

        assertTrue(parsed.bodyAlreadyConsumed());
        // query first then body appended
        assertArrayEquals(new String[]{"fromQuery", "fromBody"}, parsed.parameterMap().get("a"));
        assertArrayEquals(new String[]{"2"}, parsed.parameterMap().get("b"));
    }

    @Test
    void parseBodyAndParameters_multipart_parsesFieldStripsSingleTrailingCrlf_andExtractsFile(@TempDir Path tempDir) throws Exception {
        String boundary = "BOUNDARY123";

        // Field value has trailing CRLF as it appears before boundary; helper should strip exactly one CRLF/LF.
        byte[] multipart = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "hello\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\n"
                + "Content-Type: text/plain\r\n"
                + "\r\n"
                + "FILEDATA\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);

        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .bodyBytes(multipart)
                .build();

        List<Path> toDelete = new ArrayList<>();
        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 100_000, 100_000);

        assertTrue(parsed.bodyAlreadyConsumed());
        assertEquals("hello", parsed.parameterMap().get("title")[0]); // CRLF stripped

        var fileOpt = parsed.firstFile("file");
        assertTrue(fileOpt.isPresent());

        var mf = fileOpt.get();
        assertEquals("file", mf.fieldName);
        assertEquals("a.txt", mf.submittedFileName);
        assertEquals("text/plain", mf.contentType);
        assertEquals(8, mf.size);

        String fileContent = Files.readString(mf.tempFile, StandardCharsets.ISO_8859_1);
        assertEquals("FILEDATA", fileContent);

        // temp file path is returned to caller for deletion (server does this in finally)
        assertFalse(toDelete.isEmpty());
        assertTrue(toDelete.contains(mf.tempFile));
    }

    @Test
    void parseBodyAndParameters_throwsPayloadTooLarge_whenContentLengthExceedsLimit() {
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/form")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Content-Length", "50")
                .bodyBytes("a=1".getBytes(StandardCharsets.UTF_8))
                .build();

        List<Path> toDelete = new ArrayList<>();

        assertThrows(NinjaHttpServerHelper.PayloadTooLargeException.class, ()
                -> NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10, 10_000));
    }

    // ----------------------------------------------------------------------
    // Minimal HttpExchange implementation for unit tests
    // ----------------------------------------------------------------------
    static final class FakeHttpExchange extends HttpExchange {

        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final URI uri;
        private final String method;
        private final InputStream requestBody;

        private FakeHttpExchange(String method, URI uri, Headers headers, byte[] bodyBytes) {
            this.method = method;
            this.uri = uri;
            this.requestHeaders.putAll(headers);
            this.requestBody = new ByteArrayInputStream(bodyBytes == null ? new byte[0] : bodyBytes);
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {

            private String method = "GET";
            private URI uri = URI.create("http://localhost/");
            private final Headers headers = new Headers();
            private byte[] bodyBytes = new byte[0];

            Builder method(String m) {
                this.method = m;
                return this;
            }

            Builder uri(String u) {
                this.uri = URI.create(u);
                return this;
            }

            Builder header(String k, String v) {
                this.headers.add(k, v);
                return this;
            }

            Builder bodyBytes(byte[] b) {
                this.bodyBytes = b;
                return this;
            }

            FakeHttpExchange build() {
                return new FakeHttpExchange(method, uri, headers, bodyBytes);
            }
        }

        @Override
        public Headers getRequestHeaders() {
            return requestHeaders;
        }

        @Override
        public Headers getResponseHeaders() {
            return responseHeaders;
        }

        @Override
        public URI getRequestURI() {
            return uri;
        }

        @Override
        public String getRequestMethod() {
            return method;
        }

        @Override
        public HttpContext getHttpContext() {
            return null;
        }

        @Override
        public void close() {
            /* no-op */ }

        @Override
        public InputStream getRequestBody() {
            return requestBody;
        }

        @Override
        public OutputStream getResponseBody() {
            return new ByteArrayOutputStream();
        }

        @Override
        public void sendResponseHeaders(int rCode, long responseLength) {
            /* no-op */ }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("127.0.0.1", 12345);
        }

        @Override
        public int getResponseCode() {
            return 0;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return new InetSocketAddress("127.0.0.1", 8080);
        }

        @Override
        public String getProtocol() {
            return "HTTP/1.1";
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public void setAttribute(String name, Object value) {
        }

        @Override
        public void setStreams(InputStream i, OutputStream o) {
        }

        @Override
        public HttpPrincipal getPrincipal() {
            return null;
        }
    }
}
