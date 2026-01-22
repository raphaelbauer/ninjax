package views;

import java.util.Map;
import org.ninjax.htmltemplate.Html;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;
import org.ninjax.htmltemplate.NinjaHtmlTemplateTool;

public class LinkTag {
    
    private static final String TEMPLATE = """
                                  <a href="{{href}}">{{innerText}}</a>
                                  """;

    public static NinjaHtmlTemplate render(String innerText, String href) {
        
        var templateWithParameters = NinjaHtmlTemplateTool.replacePlaceholders(
                TEMPLATE, 
                Map.of("href", href, "innerText", innerText));
        
        NinjaHtmlTemplate ninjaHtmlTemplate = new NinjaHtmlTemplate();
        ninjaHtmlTemplate.append(new Html(templateWithParameters));

        return ninjaHtmlTemplate;
    }

}
