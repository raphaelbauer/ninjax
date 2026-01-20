package org.ninja.demo.todo.tasks.views;

import org.juckula.JuckulaCompositionTemplate;

public class TaskFormTemplate {
    
    public static JuckulaCompositionTemplate render() {
        JuckulaCompositionTemplate template = new JuckulaCompositionTemplate();
        
        template.html("""
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