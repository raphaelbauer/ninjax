package org.ninja.demo.todo.tasks.views;

import org.juckula.JuckulaCompositionTemplate;
import org.juckula.JuckulaTool;
import org.ninja.demo.todo.tasks.Task;

import java.util.Map;
import java.util.List;
import org.ninja.demo.todo.views.LayoutTemplate;

public class TodoTemplateService {
    
    public String generateTodoPage(List<Task> tasks) {
        // Generate dynamic content
        JuckulaCompositionTemplate dynamicContent = TaskListTemplate.render(tasks);
        
        // Generate full layout
        JuckulaCompositionTemplate fullPage = LayoutTemplate.render("Todo List - NinjaX Demo", dynamicContent);
        
        return fullPage.toString();
    }

}