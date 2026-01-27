package org.ninjax.demo.todo.tasks.views;

import org.ninjax.htmltemplate.NinjaHtmlTemplate;

public class TaskFormTemplate {
    
    public static NinjaHtmlTemplate render() {
        NinjaHtmlTemplate template = new NinjaHtmlTemplate();
        
        template.appendHtml("""
            <form method="post" action="/tasks">
                <div class="form-group">
                    <input type="text" name="title" placeholder="Enter a new task..." required>
                    <button type="submit" class="btn btn-add">Add Task</button>
                </div>
            </form>
            """);
        
        return template;
    }
}