package com.ninjaxframework.demo.todo.views;

import java.util.Map;
import java.util.Optional;
import com.ninjaxframework.htmltemplate.NinjaHtmlTemplate;
import com.ninjaxframework.htmltemplate.NinjaHtmlTemplateTool;

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
