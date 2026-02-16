package com.ninjaxframework.demo.todo.tasks.views;

import com.ninjaxframework.htmltemplate.NinjaHtmlTemplate;
import com.ninjaxframework.htmltemplate.NinjaHtmlTemplateTool;
import com.ninjaxframework.demo.todo.tasks.Task;

import java.util.Map;
import java.util.List;
import com.ninjaxframework.demo.todo.views.LayoutTemplate;

public class TodoTemplateService {
    
    public String generateTodoPage(List<Task> tasks) {
        // Generate dynamic content
        NinjaHtmlTemplate dynamicContent = TaskListTemplate.render(tasks);
        
        // Generate full layout
        NinjaHtmlTemplate fullPage = LayoutTemplate.render("Todo List - NinjaX Demo", dynamicContent);
        
        return fullPage.toString();
    }

}