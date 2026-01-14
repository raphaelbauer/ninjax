package org.ninja.demo.todo.templates;

import org.ninja.demo.todo.Task;
import org.juckula.JuckulaCompositionTemplate;

public class TaskItemTemplate {
    
    public static JuckulaCompositionTemplate render(Task task) {
        JuckulaCompositionTemplate template = new JuckulaCompositionTemplate();
        
        String completedClass = task.completed() ? "completed" : "";
        String completedText = task.completed() ? "✅ " : "⏳ ";
        String createdAtStr = task.createdAt().toString().substring(0, 19).replace("T", " ");
        
        template.html("""
            <div class="task """, completedClass, """
                ">
                <strong>""", completedText, JuckulaCompositionTemplate.escapeUnsafe(task.title()), "</strong>");
        
        template.html("""
                <small style="color: #666; display: block; margin-top: 5px;">
                    Created: """, createdAtStr);
        
        if (task.completed()) {
            template.html(" | ✅ Completed");
        }
        
        template.html(String.format("""
                </small>
                <div class="task-actions">
                    <form method="post" action="/tasks/delete" style="display: inline;">
                        <input type="hidden" name="id" value="%d">
                        <button type="submit" class="btn btn-delete" onclick="return confirm('Delete this task?')">🗑️ Delete</button>
                    </form>
                </div>
            </div>
            """, task.id()));
        
        return template;
    }
}