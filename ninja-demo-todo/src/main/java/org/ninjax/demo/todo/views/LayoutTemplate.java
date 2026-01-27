package org.ninjax.demo.todo.views;

import java.util.Map;
import java.util.Optional;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;
import org.ninjax.htmltemplate.NinjaHtmlTemplateTool;

public class LayoutTemplate {

    private final static String TEMPLATE = NinjaHtmlTemplateTool.readResourceFile(LayoutTemplate.class);

    public static NinjaHtmlTemplate render(String title, NinjaHtmlTemplate content) {

        var parameters = Map.of(
                "title", title,
                "content", content
        );
        var templateWithVariables = NinjaHtmlTemplateTool.replacePlaceholders(TEMPLATE, parameters);
        
        var template = new NinjaHtmlTemplate();
        template.appendHtml(templateWithVariables);

        return template;
    }

}
