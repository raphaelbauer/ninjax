package org.ninjax.htmltemplate;

import org.ninjax.htmltemplate.NinjaHtmlTemplateTool;
import org.ninjax.htmltemplate.Html;
import static com.google.common.truth.Truth.assertThat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NinjaHtmlTemplateToolTest {
    
    @Test
    void replacePlaceholders_replacesSinglePlaceholder() {
        String template = "Hello {{name}}!";
        Map<String, String> params = Map.of("name", "World");
        String result = NinjaHtmlTemplateTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Hello World!");
    }
    
    @Test
    void replacePlaceholders_escapes_strings() {
        //given
        String template = "Hello {{name}}!";
        Map<String, String> params = Map.of("name", "<script>alert('x');</script> & \"test\"");
        
        // when
        String result = NinjaHtmlTemplateTool.replacePlaceholders(template, params);

        // then
        assertThat(result).doesNotContain("<");
        assertThat(result).doesNotContain(">");
        assertThat(result).doesNotContain("\"");
        assertThat(result).contains("&lt;script&gt;alert(&#39;x&#39;);&lt;/script&gt; &amp; &quot;test&quot;");

    }
    
    @Test
    void replacePlaceholders_does_not_escape_html() {
        //given
        String template = "Hello {{name}}!";
        Map<String, ?> params = Map.of("name", new Html("<script>alert('x');</script> & \"test\""));
        
        // when
        String result = NinjaHtmlTemplateTool.replacePlaceholders(template, params);

        // then
        assertThat(result).isEqualTo("Hello <script>alert('x');</script> & \"test\"!");
    }
  
}