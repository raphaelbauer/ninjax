package org.ninja.demo.todo.views;

import java.util.Map;
import java.util.Optional;
import org.juckula.JuckulaCompositionTemplate;
import org.juckula.JuckulaTool;

public class LayoutTemplate {

    private final static String TEMPLATE = JuckulaTool.readResourceFile(LayoutTemplate.class);

    public static JuckulaCompositionTemplate render(String title, JuckulaCompositionTemplate content) {

        var parameters = Map.of(
                "title", title,
                "content", content.toString()
        );
        var templateWithVariables = JuckulaTool.replacePlaceholders(TEMPLATE, parameters);
        
        var template = new JuckulaCompositionTemplate();
        template.html(templateWithVariables);

        return template;
    }

}
