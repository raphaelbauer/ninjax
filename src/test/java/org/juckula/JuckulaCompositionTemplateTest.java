package org.juckula;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class JuckulaCompositionTemplateTest {

    @Test
    void html_appendsStringsWithTrailingNewline() {
        JuckulaCompositionTemplate t = new JuckulaCompositionTemplate();

        t.html("Hello", " ", "World");

        assertThat(t.toString()).isEqualTo("Hello World\n");
    }

    @Test
    void html_canBeCalledMultipleTimes_appendsLines() {
        JuckulaCompositionTemplate t = new JuckulaCompositionTemplate();

        t.html("line1");
        t.html("line2");

        assertThat(t.toString()).isEqualTo("line1\nline2\n");
    }

    @Test
    void html_withTemplate_appendsOtherTemplateContent() {
        JuckulaCompositionTemplate t1 = new JuckulaCompositionTemplate();
        t1.html("foo");

        JuckulaCompositionTemplate t2 = new JuckulaCompositionTemplate();
        t2.html("bar");

        // append t2 into t1
        t1.html(t2);

        assertThat(t1.toString()).isEqualTo("foo\nbar\n");
    }

    @Test
    void escapeUnsafe_escapesHtmlSpecialChars() {
        String input = "<script>alert('x');</script> & \"test\"";
        String escaped = JuckulaCompositionTemplate.escapeUnsafe(input);

        // Using Guava HtmlEscapers.htmlEscaper contract:
        assertThat(escaped).doesNotContain("<");
        assertThat(escaped).doesNotContain(">");
        assertThat(escaped).doesNotContain("\"");
        assertThat(escaped).contains("&lt;script&gt;alert(&#39;x&#39;);&lt;/script&gt; &amp; &quot;test&quot;");
    }

    @Test
    void writeOut_writesUtf8ContentToOutputStream() throws Exception {
        JuckulaCompositionTemplate t = new JuckulaCompositionTemplate();
        t.html("Héllo");
        t.html("World");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        t.writeOut(baos);

        String result = baos.toString(StandardCharsets.UTF_8);
        assertThat(result).isEqualTo("Héllo\nWorld\n");
    }

    @Test
    void writeOut_wrapsIOExceptionInIllegalStateException() {
        JuckulaCompositionTemplate t = new JuckulaCompositionTemplate();
        t.html("data");

        OutputStream throwingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("boom");
            }
        };

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> t.writeOut(throwingStream)
        );

        assertThat(ex).hasMessageThat()
                .contains("Could not find charset for UTF-8. That's totally strange. Stopping.");
        assertThat(ex.getCause()).isInstanceOf(IOException.class);
        assertThat(ex.getCause()).hasMessageThat().contains("boom");
    }
}
