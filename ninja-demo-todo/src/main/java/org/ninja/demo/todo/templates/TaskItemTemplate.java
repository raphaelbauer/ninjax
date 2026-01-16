package org.ninja.demo.todo.templates;

import java.util.Map;
import org.ninja.demo.todo.Task;
import org.juckula.JuckulaCompositionTemplate;
import org.juckula.JuckulaTool;

public class TaskItemTemplate {

    private final static String TEMPLATE = JuckulaTool.readResourceFile(TaskItemTemplate.class);

    public static JuckulaCompositionTemplate render(Task task) {

        String completedClass = task.completed() ? "completed" : "";
        String completedText = task.completed() ? "✅ " : "⏳ ";
        String createdAtStr = task.createdAt().toString().substring(0, 19).replace("T", " ");
        String completed = task.completed() ? " | ✅ Completed" : "";
        String toggleButtonText = task.completed() ? "↩️ Undo" : "✅ Complete";

        var parameters = Map.of(
                "completedClass", completedClass,
                "completedText", completedText,
                "title", task.title(),
                "createdAtStr", createdAtStr,
                "completed", completed,
                "taskId", task.id().toString(),
                "toggleButtonText", toggleButtonText
        );
        var templateWithVariables = JuckulaTool.replacePlaceholders(TEMPLATE, parameters);

        var template = new JuckulaCompositionTemplate();
        template.html(templateWithVariables);

        return template;

    }

}
