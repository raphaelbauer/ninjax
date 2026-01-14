package org.ninja.demo.todo;

import org.ninja.core.Request;
import org.ninja.core.Result;

import java.util.List;

public class TodoController {

    private final TaskService taskService;

    public TodoController(TaskService taskService) {
        this.taskService = taskService;
    }

    public Result showTasks(Request request) {
        try {
            List<Task> tasks = taskService.findAny();
            
            String html = generateTodoHtml(tasks);
            
            return Result.builder()
                    .status(Result.SC_200_OK)
                    .html(html)
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error: " + e.getMessage())
                    .build();
        }
    }

    public Result addTask(Request request) {
        try {
            String title = request.getParameter("title").stream().findFirst().orElse("");
            
            if (title.trim().isEmpty()) {
                return Result.builder()
                        .badRequest()
                        .text("Title cannot be empty")
                        .build();
            }
            
            Task task = new Task(null, title.trim(), "", java.time.LocalDateTime.now(), false);
            taskService.create(task);
            
            return Result.builder()
                    .redirect("/")
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error adding task: " + e.getMessage())
                    .build();
        }
    }

    public Result deleteTask(Request request) {
        try {
            String idStr = request.getParameter("id").stream().findFirst().orElse("");
            
            long id = Long.parseLong(idStr);
            taskService.delete(id);
            
            return Result.builder()
                    .redirect("/")
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error deleting task: " + e.getMessage())
                    .build();
        }
    }

    public Result getTasksJson(Request request) {
        try {
            List<Task> tasks = taskService.findAny();
            
            return Result.builder()
                    .status(Result.SC_200_OK)
                    .json(tasks)
                    .build();
        } catch (Exception e) {
            return Result.builder()
                    .status(Result.SC_500_INTERNAL_SERVER_ERROR)
                    .text("Error getting tasks: " + e.getMessage())
                    .build();
        }
    }
    
    private String generateTodoHtml(List<Task> tasks) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"en\">");
        html.append("<head>");
        html.append("    <meta charset=\"UTF-8\">");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("    <title>Todo List - NinjaX Demo</title>");
        html.append("    <style>");
        html.append("        body { font-family: Arial, sans-serif; max-width: 800px; margin: 40px auto; padding: 20px; }");
        html.append("        .task { border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 5px; }");
        html.append("        .task.completed { background-color: #f0f0f0; text-decoration: line-through; }");
        html.append("        .task-actions { margin-top: 10px; }");
        html.append("        .btn { padding: 8px 16px; margin: 5px; border: none; border-radius: 3px; cursor: pointer; }");
        html.append("        .btn-delete { background-color: #dc3545; color: white; }");
        html.append("        .form-group { margin-bottom: 15px; }");
        html.append("        input[type=\"text\"] { width: 70%; padding: 8px; border: 1px solid #ddd; border-radius: 3px; }");
        html.append("        .btn-add { background-color: #28a745; color: white; padding: 8px 20px; }");
        html.append("        h1 { color: #333; }");
        html.append("        .empty-message { color: #666; font-style: italic; }");
        html.append("    </style>");
        html.append("</head>");
        html.append("<body>");
        html.append("    <h1>📝 Todo List - NinjaX Framework Demo</h1>");
        html.append("");
        html.append("    <form method=\"post\" action=\"/tasks\">");
        html.append("        <div class=\"form-group\">");
        html.append("            <input type=\"text\" name=\"title\" placeholder=\"Enter a new task...\" required>");
        html.append("            <button type=\"submit\" class=\"btn btn-add\">Add Task</button>");
        html.append("        </div>");
        html.append("    </form>");
        html.append("");
        html.append("    <h2>Tasks</h2>");
        
        if (tasks.isEmpty()) {
            html.append("    <p class=\"empty-message\">No tasks yet. Add one above! 🚀</p>");
        } else {
            for (Task task : tasks) {
                String completedClass = task.completed() ? "completed" : "";
                String completedText = task.completed() ? "✅ " : "⏳ ";
                
                html.append("    <div class=\"task ").append(completedClass).append("\">");
                html.append("        <strong>").append(completedText).append(escapeHtml(task.title())).append("</strong>");
                html.append("        <small style=\"color: #666; display: block; margin-top: 5px;\">");
                html.append("            Created: ").append(task.created_at().toString().substring(0, 19).replace("T", " "));
                
                if (task.completed()) {
                    html.append("            | ✅ Completed");
                }
                
                html.append("        </small>");
                html.append("        <div class=\"task-actions\">");
                html.append("            <form method=\"post\" action=\"/tasks/delete\" style=\"display: inline;\">");
                html.append("                <input type=\"hidden\" name=\"id\" value=\"").append(task.id()).append("\">");
                html.append("                <button type=\"submit\" class=\"btn btn-delete\" onclick=\"return confirm('Delete this task?')\">🗑️ Delete</button>");
                html.append("            </form>");
                html.append("        </div>");
                html.append("    </div>");
            }
        }
        
        html.append("");
        html.append("    <hr>");
        html.append("    <p style=\"color: #666; font-size: 14px;\">");
        html.append("        💡 Try the <a href=\"/tasks.json\" target=\"_blank\">JSON API endpoint</a> to see all tasks as JSON!");
        html.append("        <br>Built with NinjaX Java Framework - Simple, Modern, Fast 🚀");
        html.append("    </p>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}