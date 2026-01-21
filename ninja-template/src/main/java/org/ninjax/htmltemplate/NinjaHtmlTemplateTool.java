package org.ninjax.htmltemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NinjaHtmlTemplateTool {

    // Precompiled regex for performance - used to replace {{yourParameter}} with the parameter in your map
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    public static String replacePlaceholders(String template, Map<String, ?> params) {

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            if (!params.containsKey(key)) {
                throw new IllegalArgumentException("Missing variable for placeholder: " + key);
            }
            Object v = params.get(key);
            if (v == null) {
                throw new IllegalArgumentException("Missing variable for placeholder: " + key);
            }

            String replacement = renderRawHtmlOrString(v);
            
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

    protected static String renderRawHtmlOrString(Object rawHtmlOrString) {
        return switch (rawHtmlOrString) {
            case Html html -> html.rawHtml();
            case String s -> NinjaHtmlTemplate.escapeUnsafe(s);
            default -> NinjaHtmlTemplate.escapeUnsafe(String.valueOf(rawHtmlOrString));
        };

    }

}
