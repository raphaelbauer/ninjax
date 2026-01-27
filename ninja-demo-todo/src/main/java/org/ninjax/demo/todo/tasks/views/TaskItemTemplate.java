package org.ninjax.demo.todo.tasks.views;

import java.util.Map;
import org.ninjax.demo.todo.tasks.Task;
import org.ninjax.htmltemplate.Html;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;
import org.ninjax.htmltemplate.NinjaHtmlTemplateTool;

public class TaskItemTemplate {

    private final static String TEMPLATE = NinjaHtmlTemplateTool.readResourceFile(TaskItemTemplate.class);

    public static NinjaHtmlTemplate render(Task task) {

        String completedClass = task.completed() ? "completed" : "";
        String completedText = task.completed() ? "✅ " : "⏳ ";
        String createdAtStr = task.createdAt().toString().substring(0, 19).replace("T", " ");
        String completed = task.completed() ? " | ✅ Completed" : "";
        String toggleButtonText = task.completed() ? "↩️ Undo" : "✅ Complete";

        var parameters = Map.of(
                "completedClass", completedClass,
                "completedText", completedText,
                "title", new Html(task.title()),
                "createdAtStr", createdAtStr,
                "completed", completed,
                "taskId", task.id().toString(),
                "toggleButtonText", toggleButtonText
        );
        var templateWithVariables = NinjaHtmlTemplateTool.replacePlaceholders(TEMPLATE, parameters);

        var template = new NinjaHtmlTemplate();
        template.appendHtml(templateWithVariables);

        return template;

    }

}
