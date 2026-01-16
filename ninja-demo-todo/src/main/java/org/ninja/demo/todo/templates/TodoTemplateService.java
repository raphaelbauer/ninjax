package org.ninja.demo.todo.templates;

import org.juckula.JuckulaCompositionTemplate;
import org.juckula.JuckulaTool;
import org.ninja.demo.todo.Task;

import java.util.Map;
import java.util.List;

public class TodoTemplateService {
    
    public String generateTodoPage(List<Task> tasks) {
        // Generate dynamic content
        JuckulaCompositionTemplate dynamicContent = TaskListTemplate.render(tasks);
        
        // Generate full layout
        JuckulaCompositionTemplate fullPage = LayoutTemplate.render("Todo List - NinjaX Demo", dynamicContent);
        
        return fullPage.toString();
    }

}