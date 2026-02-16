package com.r10r.ninjax.demo.todo.tasks.views;

import java.util.Map;
import com.r10r.ninjax.demo.todo.tasks.Task;
import com.r10r.ninjax.htmltemplate.Html;
import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplate;
import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplateTool;

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
