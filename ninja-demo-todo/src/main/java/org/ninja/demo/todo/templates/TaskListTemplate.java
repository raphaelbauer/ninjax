package org.ninja.demo.todo.templates;

import org.ninja.demo.todo.Task;
import org.juckula.JuckulaCompositionTemplate;

import java.util.List;

public class TaskListTemplate {
    
    public static JuckulaCompositionTemplate render(List<Task> tasks) {
        JuckulaCompositionTemplate template = new JuckulaCompositionTemplate();
        
        template.html("<h1>📝 Todo List - NinjaX Framework Demo</h1>");
        
        // Add task form
        template.html(TaskFormTemplate.render());
        
        template.html("<h2>Tasks</h2>");
        
        if (tasks.isEmpty()) {
            template.html("<p class=\"empty-message\">No tasks yet. Add one above! 🚀</p>");
        } else {
            for (Task task : tasks) {
                template.html(TaskItemTemplate.render(task));
            }
        }
        
        return template;
    }
}