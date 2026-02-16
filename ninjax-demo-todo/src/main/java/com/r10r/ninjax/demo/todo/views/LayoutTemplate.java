package com.r10r.ninjax.demo.todo.views;

import java.util.Map;
import java.util.Optional;
import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplate;
import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplateTool;

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
