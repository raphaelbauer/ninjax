package com.ninjaxframework.demo.todo.tasks.views;

import java.util.Map;
import com.ninjaxframework.demo.todo.tasks.Task;
import com.ninjaxframework.htmltemplate.Html;
import com.ninjaxframework.htmltemplate.NinjaHtmlTemplate;
import com.ninjaxframework.htmltemplate.NinjaHtmlTemplateTool;

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
