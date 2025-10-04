package org.juckula;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JuckulaTool {

    // Precompiled regex for performance - used to replace {{yourParameter}} with the parameter in your map
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    public static String replacePlaceholders(String template, Map<String, String> params) {
        // FIXME => No Html Escaping... This needs to be added...

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (!params.containsKey(key)) {
                throw new IllegalArgumentException("Missing variable for placeholder: " + key);
            }
            String replacement = params.get(key);
            // Escape $ and \ in replacement to avoid issues
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String readResourceFile(Class<?> clazz) {
        String resourceName = clazz.getSimpleName() + ".html";
        try (InputStream is = clazz.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourceName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
