package org.ninjax.demo.todo.tasks.views;

import org.ninjax.demo.todo.tasks.Task;
import org.ninjax.htmltemplate.NinjaHtmlTemplate;

import java.util.List;

public class TaskListTemplate {
    
    public static NinjaHtmlTemplate render(List<Task> tasks) {
        NinjaHtmlTemplate template = new NinjaHtmlTemplate();
        
        template.appendHtml("<h1>📝 Todo List - NinjaX Framework Demo</h1>");
        
        // Add task form
        template.append(TaskFormTemplate.render());
        
        template.appendHtml("<h2>Tasks</h2>");
        
        if (tasks.isEmpty()) {
            template.appendHtml("<p class=\"empty-message\">No tasks yet. Add one above! 🚀</p>");
        } else {
            for (Task task : tasks) {
                template.append(TaskItemTemplate.render(task));
            }
        }
        
        return template;
    }
}