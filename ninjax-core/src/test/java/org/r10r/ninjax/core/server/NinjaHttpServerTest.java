package org.r10r.ninjax.core.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.r10r.ninjax.core.HttpOnly;
import org.r10r.ninjax.core.NinjaCookie;
import org.r10r.ninjax.core.Result;
import org.r10r.ninjax.core.SameSite;
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
    void responseLengthFor_knownBytes_isExactLength_emptyIsNoBody_streamIsChunked() {
        // given
        Result.OutputStreamRenderer html = Result.builder().html("<p>ä</p>").build().outputStreamRenderer().get();
        Result.OutputStreamRenderer empty = Result.builder().text("").build().outputStreamRenderer().get();
        Result.OutputStreamRenderer stream = outputStream -> {};

        // when / then
        assertEquals(9, NinjaHttpServerHelper.responseLengthFor(html)); // "ä" is 2 bytes in UTF-8
        assertEquals(-1, NinjaHttpServerHelper.responseLengthFor(empty));
        assertEquals(0, NinjaHttpServerHelper.responseLengthFor(stream));
    }

    @Test
    void toSetCookieHeader_rejectsCookieThatWouldInjectAttributes() {
        // given: built via the canonical constructor, which does not validate
        NinjaCookie c = new NinjaCookie("a", "x; Domain=evil.example", Optional.empty(), -1,
                Optional.empty(), Secure.No, HttpOnly.No, Optional.empty());

        // when / then
        assertThrows(IllegalArgumentException.class, () -> NinjaHttpServerHelper.toSetCookieHeader(c));
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
                HttpOnly.ofBoolean(true),
                Optional.of(SameSite.Strict)
        );

        String header = NinjaHttpServerHelper.toSetCookieHeader(c);

        assertTrue(header.startsWith("sid=abc"));
        assertTrue(header.contains("; Path=/"));
        assertTrue(header.contains("; Domain=example.com"));
        assertTrue(header.contains("; Max-Age=3600"));
        assertTrue(header.contains("; Secure"));
        assertTrue(header.contains("; HttpOnly"));
        assertTrue(header.contains("; SameSite=Strict"));
    }

    @Test
    void toSetCookieHeader_omitsSameSiteWhenNotSet() {
        NinjaCookie c = NinjaCookie.builder("sid", "abc").build();

        String header = NinjaHttpServerHelper.toSetCookieHeader(c);

        assertEquals("sid=abc", header);
    }

    @Test
    void parseCookies_leavesSameSiteEmpty() {
        Headers h = new Headers();
        h.add("Cookie", "a=1");

        List<NinjaCookie> cookies = NinjaHttpServerHelper.parseCookies(h);

        assertTrue(cookies.get(0).sameSite().isEmpty());
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
    void parseBodyAndParameters_multipartTextFieldAboveInMemoryLimit_throwsPayloadTooLarge_withoutTempFile() {
        // given
        String boundary = "BOUNDARY123";
        byte[] multipart = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "x".repeat(200) + "\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .bodyBytes(multipart)
                .build();
        List<Path> toDelete = new ArrayList<>();

        // when
        assertThrows(NinjaHttpServerHelper.PayloadTooLargeException.class,
                () -> NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 100_000, 100));

        // then
        assertTrue(toDelete.isEmpty());
    }

    @Test
    void parseBodyAndParameters_multipartTextFieldWithinInMemoryLimit_isParsed() throws Exception {
        // given
        String boundary = "BOUNDARY123";
        byte[] multipart = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "x".repeat(100) + "\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .bodyBytes(multipart)
                .build();
        List<Path> toDelete = new ArrayList<>();

        // when
        NinjaHttpServerHelper.ParsedBody parsed = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 100_000, 100);

        // then
        assertEquals("x".repeat(100), parsed.parameterMap().get("title")[0]);
        assertTrue(toDelete.isEmpty());
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

    @Test
    void parseBodyAndParameters_multipartBodyExceedingLimit_throwsPayloadTooLarge() {
        // given a valid multipart body larger than the limit, with no Content-Length
        // (exercises the streaming limit, not the Content-Length pre-check)
        String boundary = "BOUNDARY123";
        byte[] multipart = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "hello\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1); // >> 10 bytes

        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .bodyBytes(multipart)
                .build();

        List<Path> toDelete = new ArrayList<>();

        // when / then the raw body trips the limit while streaming
        assertThrows(NinjaHttpServerHelper.PayloadTooLargeException.class, ()
                -> NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10, 10_000));
    }

    // ---------------- multipart boundary scanning ----------------
    private final List<Path> multipartTempFiles = new ArrayList<>();

    @AfterEach
    void deleteMultipartTempFiles() throws IOException {
        for (Path p : multipartTempFiles) {
            Files.deleteIfExists(p);
        }
    }

    @Test
    void parseBodyAndParameters_multipartDelivered1To3BytesPerRead_findsDelimitersSplitAcrossReads() throws Exception {
        // given a body that arrives in tiny chunks, so every delimiter is split across several reads
        String boundary = "BOUNDARY123";
        byte[] multipart = ("preamble\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "hello\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"\r\n"
                + "\r\n"
                + "FILEDATA\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);

        // when
        NinjaHttpServerHelper.ParsedBody parsed = parseMultipart(boundary, new TrickleInputStream(multipart));

        // then
        assertEquals("hello", parsed.parameterMap().get("title")[0]);
        assertEquals("FILEDATA", Files.readString(parsed.firstFile("file").get().tempFile, StandardCharsets.ISO_8859_1));
    }

    @Test
    void parseBodyAndParameters_multipartBinaryContentWithPartialDelimiters_keepsContentIntact() throws Exception {
        // given file content with every byte value and near-miss delimiters that must stay part of the data
        String boundary = "BOUNDARY123";
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (int i = 0; i < 256; i++) {
            content.write(i);
        }
        content.writeBytes(bytes("\r\n--BOUND\r\n-\r\n--\r\n--BOUNDARY12\r\n\r\n--BOUNDARX123-"));
        content.writeBytes(bytes("--BOUNDARY12"));
        byte[] expected = content.toByteArray();
        byte[] multipart = singleFileMultipart(boundary, expected);

        // when parsed from one stream and from a stream returning 1-3 bytes per read
        NinjaHttpServerHelper.ParsedBody parsed = parseMultipart(boundary, new ByteArrayInputStream(multipart));
        NinjaHttpServerHelper.ParsedBody trickled = parseMultipart(boundary, new TrickleInputStream(multipart));

        // then
        assertArrayEquals(expected, Files.readAllBytes(parsed.firstFile("file").get().tempFile));
        assertArrayEquals(expected, Files.readAllBytes(trickled.firstFile("file").get().tempFile));
    }

    @Test
    void parseBodyAndParameters_multipartWithManyAndEmptyParts_parsesEveryPart() throws Exception {
        // given 500 fields, an empty field and an empty file
        String boundary = "xyz";
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            body.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"field\"\r\n\r\n")
                    .append("value").append(i).append("\r\n");
        }
        body.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"empty\"\r\n\r\n\r\n")
                .append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"emptyFile\"; filename=\"e.txt\"\r\n\r\n\r\n")
                .append("--").append(boundary).append("--\r\n");

        // when
        NinjaHttpServerHelper.ParsedBody parsed = parseMultipart(boundary, new ByteArrayInputStream(bytes(body.toString())));

        // then
        String[] values = parsed.parameterMap().get("field");
        assertEquals(500, values.length);
        assertEquals("value0", values[0]);
        assertEquals("value499", values[499]);
        assertEquals("", parsed.parameterMap().get("empty")[0]);
        assertEquals(0, parsed.firstFile("emptyFile").get().size);
    }

    @Test
    void parseBodyAndParameters_multipartWithLfOnlyLineEndings_parsesParts() throws Exception {
        // given a client that uses LF instead of CRLF everywhere
        String boundary = "xyz";
        byte[] multipart = bytes("--xyz\n"
                + "Content-Disposition: form-data; name=\"title\"\n"
                + "\n"
                + "hello\n"
                + "--xyz\n"
                + "Content-Disposition: form-data; name=\"other\"\n"
                + "\n"
                + "world\n"
                + "--xyz--\n");

        // when
        NinjaHttpServerHelper.ParsedBody parsed = parseMultipart(boundary, new ByteArrayInputStream(multipart));

        // then
        assertEquals("hello", parsed.parameterMap().get("title")[0]);
        assertEquals("world", parsed.parameterMap().get("other")[0]);
    }

    @Test
    void parseBodyAndParameters_largeMultipartUpload_isParsedCompletely() throws Exception {
        // given an 8 MiB random file
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        byte[] expected = new byte[8 * 1024 * 1024];
        new Random(42).nextBytes(expected);
        byte[] multipart = singleFileMultipart(boundary, expected);

        // when
        NinjaHttpServerHelper.ParsedBody parsed = parseMultipart(boundary, new ByteArrayInputStream(multipart));

        // then
        NinjaHttpServerHelper.MultipartFile file = parsed.firstFile("file").get();
        assertEquals(expected.length, file.size);
        assertArrayEquals(expected, Files.readAllBytes(file.tempFile));
    }

    @Test
    void parseBodyAndParameters_multipartEndingInsideBody_throwsEof() {
        // given a body that stops before the closing delimiter
        String boundary = "xyz";
        byte[] multipart = bytes("--xyz\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n"
                + "\r\n"
                + "hello\r\n--xy");

        // when / then
        assertThrows(EOFException.class, ()
                -> parseMultipart(boundary, new ByteArrayInputStream(multipart)));
    }

    @Test
    void parseBodyAndParameters_multipartBoundaryLongerThan70Chars_isRejected() {
        // given a boundary longer than RFC 2046 allows
        String boundary = "a".repeat(71);
        byte[] multipart = singleFileMultipart(boundary, bytes("data"));

        // when / then
        assertThrows(IOException.class, ()
                -> parseMultipart(boundary, new ByteArrayInputStream(multipart)));
    }

    private NinjaHttpServerHelper.ParsedBody parseMultipart(String boundary, InputStream body) throws IOException {
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/upload")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .body(body)
                .build();
        return NinjaHttpServerHelper.parseBodyAndParameters(ex, multipartTempFiles, 20_000_000, 100_000);
    }

    private static byte[] singleFileMultipart(String boundary, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(bytes("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"data.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "\r\n"));
        out.writeBytes(content);
        out.writeBytes(bytes("\r\n--" + boundary + "--\r\n"));
        return out.toByteArray();
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    // ---------------- input stream getter (non-form bodies) ----------------
    @Test
    void inputStreamGetter_jsonBodyExceedingLimit_throwsPayloadTooLargeWhenRead() throws Exception {
        // given a JSON (non-form) body larger than the limit, with no Content-Length
        // (so the Content-Length pre-check cannot catch it; mimics chunked encoding)
        byte[] body = "{\"name\":\"aaaaaaaaaaaaaaaaaaaa\"}".getBytes(StandardCharsets.UTF_8); // > 10 bytes
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/api")
                .header("Content-Type", "application/json")
                .bodyBytes(body)
                .build();

        List<Path> toDelete = new ArrayList<>();
        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10, 10);

        // JSON bodies are not consumed during parsing, so the controller reads them via the getter
        assertFalse(parsed.bodyAlreadyConsumed());

        // when the controller reads the body the framework hands it
        var getter = NinjaHttpServerHelper.inputStreamGetter(ex, parsed, 10);

        // then the stream is bounded by maxUploadBytes
        assertThrows(NinjaHttpServerHelper.PayloadTooLargeException.class,
                () -> getter.get().readAllBytes());
    }

    @Test
    void inputStreamGetter_jsonBodyWithinLimit_returnsBytes() throws Exception {
        // given a JSON body smaller than the limit
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8); // 7 bytes
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/api")
                .header("Content-Type", "application/json")
                .bodyBytes(body)
                .build();

        List<Path> toDelete = new ArrayList<>();
        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, toDelete, 10, 10);

        // when the controller reads the body
        var getter = NinjaHttpServerHelper.inputStreamGetter(ex, parsed, 10);
        byte[] read = getter.get().readAllBytes();

        // then it reads the full body without throwing
        assertArrayEquals(body, read);
    }

    @Test
    void inputStreamGetter_repeatedGetCalls_shareOneLimit() throws Exception {
        // given a 20 byte JSON body and a limit of 10 bytes
        byte[] body = "0123456789ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8);
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/api")
                .header("Content-Type", "application/json")
                .bodyBytes(body)
                .build();

        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, new ArrayList<>(), 10, 10);
        var getter = NinjaHttpServerHelper.inputStreamGetter(ex, parsed, 10);

        // when the body is read in two steps via separate get() calls (e.g. a filter, then the controller)
        byte[] firstPart = getter.get().readNBytes(10);

        // then both calls return the same stream and the limit is not reset
        assertSame(getter.get(), getter.get());
        assertEquals(10, firstPart.length);
        assertThrows(NinjaHttpServerHelper.PayloadTooLargeException.class,
                () -> getter.get().readNBytes(10));
    }

    @Test
    void inputStreamGetter_bodyAlreadyConsumed_returnsEmptyStream() throws Exception {
        // given a urlencoded body that was consumed during parsing
        FakeHttpExchange ex = FakeHttpExchange.builder()
                .method("POST")
                .uri("http://localhost/form")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyBytes("a=1".getBytes(StandardCharsets.UTF_8))
                .build();

        NinjaHttpServerHelper.ParsedBody parsed
                = NinjaHttpServerHelper.parseBodyAndParameters(ex, new ArrayList<>(), 10, 10);

        // when the controller asks for the body
        byte[] read = NinjaHttpServerHelper.inputStreamGetter(ex, parsed, 10).get().readAllBytes();

        // then it is empty
        assertTrue(parsed.bodyAlreadyConsumed());
        assertEquals(0, read.length);
    }

    // ---------------- payload too large detection ----------------
    @Test
    void isCausedByPayloadTooLarge_directException_returnsTrue() {
        // given
        var tooLarge = new NinjaHttpServerHelper.PayloadTooLargeException("too large");

        // when / then
        assertTrue(NinjaHttpServerHelper.isCausedByPayloadTooLarge(tooLarge));
    }

    @Test
    void isCausedByPayloadTooLarge_wrappedException_returnsTrue() {
        // given the exception wrapped the way a controller has to (unchecked, several levels deep)
        var wrapped = new RuntimeException("controller failed",
                new UncheckedIOException(new NinjaHttpServerHelper.PayloadTooLargeException("too large")));

        // when / then
        assertTrue(NinjaHttpServerHelper.isCausedByPayloadTooLarge(wrapped));
    }

    @Test
    void isCausedByPayloadTooLarge_unrelatedException_returnsFalse() {
        // given
        var unrelated = new IllegalStateException("boom", new IOException("disk full"));

        // when / then
        assertFalse(NinjaHttpServerHelper.isCausedByPayloadTooLarge(unrelated));
        assertFalse(NinjaHttpServerHelper.isCausedByPayloadTooLarge(null));
    }

    @Test
    void isCausedByPayloadTooLarge_cyclicCauseChain_terminates() {
        // given two exceptions that are each other's cause
        var first = new RuntimeException("first");
        var second = new RuntimeException("second", first);
        first.initCause(second);

        // when / then it does not loop forever
        assertFalse(NinjaHttpServerHelper.isCausedByPayloadTooLarge(first));
    }

    // ----------------------------------------------------------------------
    // InputStream that returns only 1 to 3 bytes per read, like a slow network
    // ----------------------------------------------------------------------
    static final class TrickleInputStream extends InputStream {

        private final byte[] data;
        private int pos;
        private int reads;

        TrickleInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xff : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= data.length) {
                return -1;
            }
            int n = Math.min(Math.min(len, 1 + reads++ % 3), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }
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

        private FakeHttpExchange(String method, URI uri, Headers headers, InputStream requestBody) {
            this.method = method;
            this.uri = uri;
            this.requestHeaders.putAll(headers);
            this.requestBody = requestBody;
        }

        static Builder builder() {
            return new Builder();
        }

        static final class Builder {

            private String method = "GET";
            private URI uri = URI.create("http://localhost/");
            private final Headers headers = new Headers();
            private InputStream body = InputStream.nullInputStream();

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
                this.body = new ByteArrayInputStream(b == null ? new byte[0] : b);
                return this;
            }

            Builder body(InputStream in) {
                this.body = in;
                return this;
            }

            FakeHttpExchange build() {
                return new FakeHttpExchange(method, uri, headers, body);
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
