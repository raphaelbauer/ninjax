package org.juckula;

import static com.google.common.truth.Truth.assertThat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuckulaToolTest {
    
    @Test
    void replacePlaceholders_replacesSinglePlaceholder() {
        String template = "Hello {{name}}!";
        Map<String, String> params = Map.of("name", "World");
        String result = JuckulaTool.replacePlaceholders(template, params);

        assertThat(result).isEqualTo("Hello World!");
    }
}