package org.ninjax.htmltemplate;


import com.google.common.html.HtmlEscapers;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NinjaHtmlTemplate {
 


    private final StringBuilder stringBuilder = new StringBuilder();

    public void append(String string) {
        String toRender = NinjaHtmlTemplate.escapeUnsafe(string);
        stringBuilder.append(toRender);
    }
    
    /**
     * This appends a String without any html escaping (!).
     * Only do this when you created and trust that string. 
     * Never do this for anything that's coming from a user.
     * 
     * This is equivalent to append(new Html(..)...
     * 
     * @param htmlString The string that will be appended. No escaping is performed.
     */
    public void appendHtml(String htmlString) {
        stringBuilder.append(htmlString);
    }
        
    public void append(Object stringOrRawHtml) {
        String toRender = NinjaHtmlTemplateTool.renderRawHtmlOrString(stringOrRawHtml);
        stringBuilder.append(toRender);
    }

    public void append(NinjaHtmlTemplate template) {
        stringBuilder.append(template.toString());
    }

    public static String escapeUnsafe(String unescapedString) {
        return HtmlEscapers.htmlEscaper().escape(unescapedString);
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }

    public void writeOut(OutputStream outputStream) {
        try {
            outputStream.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ioException) {
            throw new IllegalStateException("Could not find charset for UTF-8. That's totally strange. Stopping.", ioException);
        }
    }

}