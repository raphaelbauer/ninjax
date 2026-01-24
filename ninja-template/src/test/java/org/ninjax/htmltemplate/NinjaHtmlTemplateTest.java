package org.ninjax.htmltemplate;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class NinjaHtmlTemplateTest {

    @Test
    void html_canBeCalledMultipleTimes() {
        NinjaHtmlTemplate t = new NinjaHtmlTemplate();

        t.append("line1");
        t.append("line2");

        assertThat(t.toString()).isEqualTo("line1line2");
    }

    @Test
    void html_withTemplate_appendsOtherTemplateContent() {
        NinjaHtmlTemplate t1 = new NinjaHtmlTemplate();
        t1.append("foo");

        NinjaHtmlTemplate t2 = new NinjaHtmlTemplate();
        t2.append("bar");

        // append t2 into t1
        t1.append(t2);

        assertThat(t1.toString()).isEqualTo("foobar");
    }

    @Test
    void ninjaHtmlTemplate_escapes_strings_by_default() {
        // given
        String input = "<script>alert('x');</script> & \"test\"";

        // when
        var ninjaHtmlTemplate = new NinjaHtmlTemplate();
        ninjaHtmlTemplate.append(input);
        var result = ninjaHtmlTemplate.toString();

        // then
        assertThat(result).doesNotContain("<");
        assertThat(result).doesNotContain(">");
        assertThat(result).doesNotContain("\"");
        assertThat(result).contains("&lt;script&gt;alert(&#39;x&#39;);&lt;/script&gt; &amp; &quot;test&quot;");
    }
    
    @Test
    void ninjaHtmlTemplate_does_not_escape_other_NinjaHtmlTemplates() {
        // given
        String intentionalHtml = "<p>intentional html</p>";
        var intentionalHtmlTemplate = new NinjaHtmlTemplate();
        intentionalHtmlTemplate..appendHtml(intentionalHtml));

        // when
        var ninjaHtmlTemplate = new NinjaHtmlTemplate();
        ninjaHtmlTemplate.append(intentionalHtmlTemplate);
        var result = ninjaHtmlTemplate.toString();

        // then
        assertThat(result).isEqualTo(intentionalHtml);
    }

    @Test
    void ninjaHtmlTemplate_does_not_escape_html() {
        // given
        String input = "<script>alert('x');</script> & \"test\"";

        // when
        var ninjaHtmlTemplate = new NinjaHtmlTemplate();
        ninjaHtmlTemplate..appendHtml(input));
        var result = ninjaHtmlTemplate.toString();

        // then
        assertThat(result).isEqualTo(input);
    }

    @Test
    void escapeUnsafe_escapesHtmlSpecialChars() {
        String input = "<script>alert('x');</script> & \"test\"";
        String escaped = NinjaHtmlTemplate.escapeUnsafe(input);

        // Using Guava HtmlEscapers.htmlEscaper contract:
        assertThat(escaped).doesNotContain("<");
        assertThat(escaped).doesNotContain(">");
        assertThat(escaped).doesNotContain("\"");
        assertThat(escaped).contains("&lt;script&gt;alert(&#39;x&#39;);&lt;/script&gt; &amp; &quot;test&quot;");
    }

    @Test
    void writeOut_writesUtf8ContentToOutputStream() throws Exception {
        NinjaHtmlTemplate t = new NinjaHtmlTemplate();
        t.append("Héllo");
        t.append("World");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        t.writeOut(baos);

        String result = baos.toString(StandardCharsets.UTF_8);
        assertThat(result).isEqualTo("HélloWorld");
    }

    @Test
    void writeOut_wrapsIOExceptionInIllegalStateException() {
        NinjaHtmlTemplate t = new NinjaHtmlTemplate();
        t.append("data");

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
