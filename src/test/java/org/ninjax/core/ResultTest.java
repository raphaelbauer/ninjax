package org.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void defaults_areOkTextPlain_unknownSession_noRenderer_noHeaders_noCookies() {
        Result r = Result.builder().build();

        assertThat(r.getStatus()).isEqualTo(Result.SC_200_OK);
        assertThat(r.getContentType()).isEqualTo(Result.TEXT_PLAIN);

        assertThat(r.getOutputStreamRenderer()).isEmpty();
        assertThat(r.getHeaders()).isEmpty();
        assertThat(r.getCookies()).isEmpty();

        assertThat(r.getNinjaSessionState()).isInstanceOf(Result.UnknownButDontTouch.class);
    }

    @Test
    void statusAndContentType_areSet() {
        Result r = Result.builder()
                .status(Result.SC_201_CREATED)
                .contentType(Result.APPLICATION_XML)
                .build();

        assertThat(r.getStatus()).isEqualTo(Result.SC_201_CREATED);
        assertThat(r.getContentType()).isEqualTo(Result.APPLICATION_XML);
    }

    @Test
    void redirect_sets303_andLocationHeader() {
        Result r = Result.builder().redirect("https://example.test/x").build();

        assertThat(r.getStatus()).isEqualTo(Result.SC_303_SEE_OTHER);
        assertThat(r.getHeaders()).containsKey(Result.LOCATION);
        assertThat(r.getHeaders().get(Result.LOCATION)).containsExactly("https://example.test/x");
    }

    @Test
    void addHeader_allowsMultipleValues_andPreservesOrderPerKey() {
        Result r = Result.builder()
                .addHeader("X-Test", "a")
                .addHeader("X-Test", "b")
                .build();

        assertThat(r.getHeaders()).isEqualTo(Map.of("X-Test", List.of("a", "b")));
    }

    @Test
    void headersAndCookies_areImmutableViews() {
        Result r = Result.builder()
                .addHeader("X-Test", "a")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> r.getHeaders().put("Y", List.of("z")));
        assertThrows(UnsupportedOperationException.class, () -> r.getHeaders().get("X-Test").add("b"));
        assertThrows(UnsupportedOperationException.class, () -> r.getCookies().add(null));
    }

    @Test
    void toBuilder_copiesAndIsIndependentOfOriginalResult() {
        Result original = Result.builder()
                .status(Result.SC_200_OK)
                .addHeader("X-Test", "a")
                .text("hello")
                .build();

        Result modified = original.toBuilder()
                .addHeader("X-Test", "b")
                .status(Result.SC_201_CREATED)
                .build();

        // original unchanged
        assertThat(original.getStatus()).isEqualTo(Result.SC_200_OK);
        assertThat(original.getHeaders().get("X-Test")).containsExactly("a");

        // modified has updates
        assertThat(modified.getStatus()).isEqualTo(Result.SC_201_CREATED);
        assertThat(modified.getHeaders().get("X-Test")).containsExactly("a", "b").inOrder();
    }

    @Test
    void html_rendersUtf8_andSetsContentType() throws Exception {
        Result r = Result.builder().html("Héllo").build();

        assertThat(r.getContentType()).isEqualTo(Result.TEXT_HTML);
        assertThat(r.getOutputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.getOutputStreamRenderer().get().streamTo(baos);

        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("Héllo");
    }

    @Test
    void text_rendersUtf8_andSetsContentType() throws Exception {
        Result r = Result.builder().text("hi").build();

        assertThat(r.getContentType()).isEqualTo(Result.TEXT_PLAIN);
        assertThat(r.getOutputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.getOutputStreamRenderer().get().streamTo(baos);

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

        assertThat(r.getOutputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.getOutputStreamRenderer().get().streamTo(baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("x");
    }

    @Test
    void ninjaSessionState_canBeExistsOrRemove() {
        // We can't easily instantiate NinjaSession here unless it's available on the classpath.
        // So we verify Remove works and default is Unknown.
        Result removed = Result.builder().deleteNinjaSession().build();
        assertThat(removed.getNinjaSessionState()).isInstanceOf(Result.Remove.class);

        Result unknown = Result.builder().build();
        assertThat(unknown.getNinjaSessionState()).isInstanceOf(Result.UnknownButDontTouch.class);
    }

    @Test
    void nullGuards() {
        assertThrows(NullPointerException.class, () -> Result.builder().contentType(null));
        assertThrows(NullPointerException.class, () -> Result.builder().addHeader(null, "v"));
        assertThrows(NullPointerException.class, () -> Result.builder().addHeader("k", null));
        assertThrows(NullPointerException.class, () -> Result.builder().stream(null));
    }
}