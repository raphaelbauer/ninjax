package org.ninjax.demo.todo.tasks.views;

import org.ninjax.demo.todo.tasks.Task;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;

import java.util.List;

public class TaskListTemplate {
    
    public static NinjaHtmlTemplate render(List<Task> tasks) {
        NinjaHtmlTemplate template = new NinjaHtmlTemplate();
        
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