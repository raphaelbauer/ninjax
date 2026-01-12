package org.juckula;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JuckulaToolTest {

    // ---------- replacePlaceholders ----------

    @Test
    void replacePlaceholders_replacesSinglePlaceholder() {
        String template = "Hello {{name}}!";
        Map<String, String> params = Map.of("name", "World");

        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Hello World!");
    }

    @Test
    void replacePlaceholders_replacesMultiplePlaceholdersAndKeepsOtherText() {
        String template = "Dear {{firstName}} {{lastName}}, your id is {{id}}.";
        Map<String, String> params = Map.of(
                "firstName", "Jane",
                "lastName", "Doe",
                "id", "123"
        );

        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Dear Jane Doe, your id is 123.");
    }

    @Test
    void replacePlaceholders_throwsOnMissingVariable() {
        String template = "Hello {{name}}!";
        Map<String, String> params = Map.of(); // empty

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JuckulaTool.replacePlaceholders(template, params)
        );

        assertThat(ex).hasMessageThat().contains("Missing variable for placeholder: name");
    }

    @Test
    void replacePlaceholders_allowsSamePlaceholderMultipleTimes() {
        String template = "{{x}} + {{x}} = {{x}}";
        Map<String, String> params = Map.of("x", "1");

        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("1 + 1 = 1");
    }

    @Test
    void replacePlaceholders_supportsSpecialRegexCharsInReplacement() {
        // ensure $ and \ in replacements don’t break Matcher.appendReplacement
        String template = "Price: {{price}}; Path: {{path}}";
        Map<String, String> params = Map.of(
                "price", "$10",
                "path", "C:\\temp\\file.txt"
        );

        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Price: $10; Path: C:\\temp\\file.txt");
    }

    @Test
    void replacePlaceholders_leavesTemplateUnchangedWhenNoPlaceholders() {
        String template = "Just plain text with no variables.";
        Map<String, String> params = Map.of("unused", "ignored");

        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Just plain text with no variables.");
    }

    // ---------- readResourceFile ----------

    /**
     * Helper nested class purely to host a test resource named:
     *   org/juckula/JuckulaToolTest_ReadResourceFileTest.html
     *
     * Place this file on the classpath (e.g. under src/test/resources/org/juckula/)
     * with some simple HTML content like:
     *
     *   <!doctype html>
     *   <html><body>Test Template</body></html>
     */
    static class JuckulaToolTest_ReadResourceFileTest {
        // no content needed, only used for resource lookup
    }

    @Test
    void readResourceFile_readsUtf8HtmlFromClasspath() {
        String html = JuckulaTool.readResourceFile(JuckulaToolTest_ReadResourceFileTest.class);

        // Simple assertion that we got non-empty UTF-8 content and some expected substring.
        assertThat(html).isNotEmpty();
        // adjust this expected fragment to whatever you actually put into the .html test resource
        assertThat(html).contains("Test Template");
    }

    @Test
    void readResourceFile_throwsWhenResourceMissing() {
        class NonExistingResourceClass {
            // resource "NonExistingResourceClass.html" will not be present
        }

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> JuckulaTool.readResourceFile(NonExistingResourceClass.class)
        );

        assertThat(ex).hasMessageThat().contains("Resource not found: NonExistingResourceClass.html");
    }
}
