package org.ninjax.htmltemplate;

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
    void replacePlaceholders_does_not_escape_html_other_NinjaHtmlTemplates() {
        // given
        String intentionalHtml = "<p>intentional html</p>";
        var intentionalHtmlTemplate = new NinjaHtmlTemplate();
        intentionalHtmlTemplate.append(new Html(intentionalHtml));

        // when
        String template = "This should include intentional html: {{intentionalHtml}}";
        Map<String, ?> params = Map.of("intentionalHtml", intentionalHtmlTemplate);
        
        // when
        String result = NinjaHtmlTemplateTool.replacePlaceholders(template, params);

        // then
        assertThat(result).isEqualTo("This should include intentional html: <p>intentional html</p>");
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