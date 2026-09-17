package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void static_ok_withoutBody_sets200_andDefaultContentType_andNoRenderer() {
        Result r = Result.ok();

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void static_ok_withBody_sets200_andTextPlain_andRendersBody() throws Exception {
        Result r = Result.ok("hello");

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void static_notFound_sets404_andDefaultContentType_andNoRenderer() {
        Result r = Result.notFound();

        assertThat(r.status()).isEqualTo(Result.SC_404_NOT_FOUND);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }
    
    @Test
    void static_badRequest_sets400_andDefaultContentType_andNoRenderer() {
        Result r = Result.badRequest();

        assertThat(r.status()).isEqualTo(Result.SC_400_BAD_REQUEST);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }
    
    @Test
    void static_badRequest_withBody_sets400_andTextPlain_andRendersBody() throws Exception {
        Result r = Result.badRequest("invalid input");

        assertThat(r.status()).isEqualTo(Result.SC_400_BAD_REQUEST);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("invalid input");
    }

    @Test
    void static_redirect_uses303_andLocationHeader() {
        Result r = Result.redirect("https://example.test/static");

        assertThat(r.status()).isEqualTo(303);
        assertThat(r.headers()).containsKey(Result.LOCATION);
        assertThat(r.headers().get(Result.LOCATION))
                .containsExactly("https://example.test/static");
    }

    @Test
    void builder_ok_setsStatus200_andKeepsOtherDefaults() {
        Result r = Result.builder().ok().build();

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void builder_notFound_setsStatus404_andKeepsOtherDefaults() {
        Result r = Result.builder().notFound().build();

        assertThat(r.status()).isEqualTo(Result.SC_404_NOT_FOUND);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void builder_badRequest_setsStatus400_andKeepsOtherDefaults() {
        Result r = Result.builder().badRequest().build();

        assertThat(r.status()).isEqualTo(Result.SC_400_BAD_REQUEST);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void builder_internalServerError_setsStatus500_andKeepsOtherDefaults() {
        Result r = Result.builder().internalServerError().build();

        assertThat(r.status()).isEqualTo(Result.SC_500_INTERNAL_SERVER_ERROR);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void builder_redirect_sets303_andLocationHeader_andOverridesStatus() {
        Result r = Result.builder()
                .status(Result.SC_200_OK) // should be overridden
                .redirect("https://example.test/builder")
                .build();

        assertThat(r.status()).isEqualTo(Result.SC_303_SEE_OTHER);
        assertThat(r.headers()).containsKey(Result.LOCATION);
        assertThat(r.headers().get(Result.LOCATION))
                .containsExactly("https://example.test/builder");
    }

    @Test
    void json_setsContentType_andUsesJsonObjectMapper() throws Exception {
        // simple DTO
        record Foo(String name, int value) {

        }
        Foo foo = new Foo("abc", 123);
        
        org.r10r.ninjax.core.Result.OutputStreamRenderer dummyOutputStreamRenderer = outputStream -> {
            // just a dummy. In reality you'd use someting that can render Json.
        };
        
        Result r = Result.builder().json(dummyOutputStreamRenderer).build();

        assertThat(r.contentType()).isEqualTo(Result.APPLICATION_JSON);
        assertThat(r.outputStreamRenderer()).isPresent();
        
        assertThat(r.outputStreamRenderer().get()).isEqualTo(dummyOutputStreamRenderer);
    }

    @Test
    void addCookie_addsToCookiesList_andIsVisibleInImmutableView() {
        NinjaCookie cookie = NinjaCookie.builder("sid", "xyz").build();

        Result r = Result.builder()
                .addCookie(cookie)
                .build();

        assertThat(r.cookies()).hasSize(1);
        assertThat(r.cookies().get(0)).isSameInstanceAs(cookie);
    }

    @Test
    void addCookie_nullGuard() {
        assertThrows(NullPointerException.class, () -> Result.builder().addCookie(null));
    }

    @Test
    void defaults_areOkTextPlain_unknownSession_noRenderer_noHeaders_noCookies() {
        Result r = Result.builder().build();

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);

        assertThat(r.outputStreamRenderer()).isEmpty();
        assertThat(r.headers()).isEmpty();
        assertThat(r.cookies()).isEmpty();

        assertThat(r.ninjaSessionState()).isInstanceOf(Result.UnknownButDontTouch.class);
    }

    @Test
    void statusAndContentType_areSet() {
        Result r = Result.builder()
                .status(Result.SC_201_CREATED)
                .contentType(Result.APPLICATION_XML)
                .build();

        assertThat(r.status()).isEqualTo(Result.SC_201_CREATED);
        assertThat(r.contentType()).isEqualTo(Result.APPLICATION_XML);
    }

    @Test
    void redirect_sets303_andLocationHeader() {
        Result r = Result.builder().redirect("https://example.test/x").build();

        assertThat(r.status()).isEqualTo(Result.SC_303_SEE_OTHER);
        assertThat(r.headers()).containsKey(Result.LOCATION);
        assertThat(r.headers().get(Result.LOCATION)).containsExactly("https://example.test/x");
    }

    @Test
    void addHeader_allowsMultipleValues_andPreservesOrderPerKey() {
        Result r = Result.builder()
                .addHeader("X-Test", "a")
                .addHeader("X-Test", "b")
                .build();

        assertThat(r.headers()).isEqualTo(Map.of("X-Test", List.of("a", "b")));
    }

    @Test
    void headersAndCookies_areImmutableViews() {
        Result r = Result.builder()
                .addHeader("X-Test", "a")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> r.headers().put("Y", List.of("z")));
        assertThrows(UnsupportedOperationException.class, () -> r.headers().get("X-Test").add("b"));
        assertThrows(UnsupportedOperationException.class, () -> r.cookies().add(null));
    }

    @Test
    void html_rendersUtf8_andSetsContentType() throws Exception {
        Result r = Result.builder().html("Héllo").build();

        assertThat(r.contentType()).isEqualTo(Result.TEXT_HTML);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);

        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("Héllo");
    }

    @Test
    void text_rendersUtf8_andSetsContentType() throws Exception {
        Result r = Result.builder().text("hi").build();

        assertThat(r.contentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);

        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("hi");
    }

    @Test
    void stream_setsRenderer() throws Exception {
        Result r = Result.builder()
                .stream(os -> {
                    try {
                        os.write("x".getBytes(StandardCharsets.UTF_8));
                    } catch (Exception ignored) {
                    }
                })
                .build();

        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("x");
    }

    @Test
    void ninjaSessionState_canBeExistsOrRemove() {
        // We can't easily instantiate NinjaSession here unless it's available on the classpath.
        // So we verify Remove works and default is Unknown.
        Result removed = Result.builder().deleteNinjaSession().build();
        assertThat(removed.ninjaSessionState()).isInstanceOf(Result.Remove.class);

        Result unknown = Result.builder().build();
        assertThat(unknown.ninjaSessionState()).isInstanceOf(Result.UnknownButDontTouch.class);
    }

    @Test
    void nullGuards() {
        assertThrows(NullPointerException.class, () -> Result.builder().contentType(null));
        assertThrows(NullPointerException.class, () -> Result.builder().addHeader(null, "v"));
        assertThrows(NullPointerException.class, () -> Result.builder().addHeader("k", null));
        assertThrows(NullPointerException.class, () -> Result.builder().stream(null));
    }

    @Test
    void statusShortcuts_keepEverythingSetBefore() throws Exception {
        // given
        NinjaSession session = new NinjaSession(Map.of("user", "bob"));
        NinjaCookie cookie = NinjaCookie.builder("flash", "saved").build();

        // when
        Result redirect = Result.builder()
                .withNinjaSession(session)
                .addCookie(cookie)
                .addHeader("X-Custom", "1")
                .redirect("/")
                .build();
        Result notFound = Result.builder().html("<b>missing</b>").notFound().build();
        Result badRequest = Result.builder().text("invalid").badRequest().build();
        Result serverError = Result.builder().text("oops").internalServerError().build();
        Result ok = Result.builder().status(Result.SC_404_NOT_FOUND).text("fine").ok().build();

        // then
        assertThat(redirect.status()).isEqualTo(Result.SC_303_SEE_OTHER);
        assertThat(redirect.headers()).containsEntry(Result.LOCATION, List.of("/"));
        assertThat(redirect.headers()).containsEntry("X-Custom", List.of("1"));
        assertThat(redirect.cookies()).containsExactly(cookie);
        assertThat(redirect.ninjaSessionState()).isInstanceOf(Result.Exists.class);
        assertThat(((Result.Exists) redirect.ninjaSessionState()).getSession()).isEqualTo(session);

        assertThat(notFound.status()).isEqualTo(Result.SC_404_NOT_FOUND);
        assertThat(notFound.contentType()).isEqualTo(Result.TEXT_HTML);
        assertThat(render(notFound)).isEqualTo("<b>missing</b>");

        assertThat(badRequest.status()).isEqualTo(Result.SC_400_BAD_REQUEST);
        assertThat(render(badRequest)).isEqualTo("invalid");

        assertThat(serverError.status()).isEqualTo(Result.SC_500_INTERNAL_SERVER_ERROR);
        assertThat(render(serverError)).isEqualTo("oops");

        assertThat(ok.status()).isEqualTo(Result.SC_200_OK);
        assertThat(render(ok)).isEqualTo("fine");
    }

    private static String render(Result result) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.outputStreamRenderer().orElseThrow().streamTo(baos);
        return baos.toString(StandardCharsets.UTF_8);
    }
}
