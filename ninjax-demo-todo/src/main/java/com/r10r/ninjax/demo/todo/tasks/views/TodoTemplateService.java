package com.r10r.ninjax.demo.todo.tasks.views;

import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplate;
import com.r10r.ninjax.htmltemplate.NinjaHtmlTemplateTool;
import com.r10r.ninjax.demo.todo.tasks.Task;

import java.util.Map;
import java.util.List;
import com.r10r.ninjax.demo.todo.views.LayoutTemplate;

public class TodoTemplateService {
    
    public String generateTodoPage(List<Task> tasks) {
        // Generate dynamic content
        NinjaHtmlTemplate dynamicContent = TaskListTemplate.render(tasks);
        
        // Generate full layout
        NinjaHtmlTemplate fullPage = LayoutTemplate.render("Todo List - NinjaX Demo", dynamicContent);
        
        return fullPage.toString();
    }

}